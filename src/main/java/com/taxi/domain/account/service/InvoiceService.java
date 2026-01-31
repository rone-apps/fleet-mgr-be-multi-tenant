package com.taxi.domain.account.service;

import com.taxi.domain.account.dto.InvoiceSummaryDTO;
import com.taxi.domain.account.dto.InvoiceDetailsDTO;
import com.taxi.domain.account.model.*;
import com.taxi.domain.account.repository.AccountChargeRepository;
import com.taxi.domain.account.repository.AccountCustomerRepository;
import com.taxi.domain.account.repository.InvoiceLineItemRepository;
import com.taxi.domain.account.repository.InvoiceRepository;
import com.taxi.domain.account.repository.PaymentRepository;
import com.taxi.domain.tenant.service.TenantConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final AccountCustomerRepository customerRepository;
    private final AccountChargeRepository chargeRepository;
    private final EmailService emailService;
    private final InvoicePDFService pdfService;
    private final TenantConfigService tenantConfigService;

    /**
     * Generate invoice for customer for a specific billing period
     */
    public Invoice generateInvoice(Long customerId, LocalDate periodStart, LocalDate periodEnd,
                                   BigDecimal taxRate, String terms) {
        // Get customer
        AccountCustomer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        // Check for overlapping invoices
        List<Invoice> overlappingInvoices = invoiceRepository.findInvoicesWithOverlappingPeriod(
                customerId, periodStart, periodEnd);

        if (!overlappingInvoices.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
            StringBuilder details = new StringBuilder();
            for (Invoice existing : overlappingInvoices) {
                details.append(existing.getInvoiceNumber())
                        .append(" (")
                        .append(existing.getBillingPeriodStart().format(formatter))
                        .append(" - ")
                        .append(existing.getBillingPeriodEnd().format(formatter))
                        .append("), ");
            }
            throw new IllegalStateException("An invoice with an overlapping period already exists. " +
                    "Existing invoice(s): " + details.toString().replaceAll(", $", ""));
        }

        // Get unpaid charges for the period that are NOT already on an invoice
        List<AccountCharge> charges = chargeRepository
                .findUninvoicedChargesByCustomerAndDateRange(
                        customerId, periodStart, periodEnd);

        if (charges.isEmpty()) {
            throw new IllegalStateException("No uninvoiced charges found for the specified period. " +
                    "All charges may already be on an existing invoice.");
        }

        // Create invoice
        Invoice invoice = Invoice.builder()
                .invoiceNumber(generateInvoiceNumber())
                .accountId(customer.getAccountId())
                .customer(customer)
                .invoiceDate(LocalDate.now())
                .dueDate(calculateDueDate(customer))
                .billingPeriodStart(periodStart)
                .billingPeriodEnd(periodEnd)
                .taxRate(taxRate != null ? taxRate : BigDecimal.ZERO)
                .terms(terms != null ? terms : getDefaultTerms())
                .status(Invoice.InvoiceStatus.DRAFT)
                .build();

        // Add line items
        for (AccountCharge charge : charges) {
            InvoiceLineItem lineItem = InvoiceLineItem.fromCharge(charge);
            invoice.addLineItem(lineItem);
        }

        // Calculate totals
        invoice.calculateTotals();

        // Save invoice
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Update charges with invoice reference
        for (int i = 0; i < charges.size(); i++) {
            AccountCharge charge = charges.get(i);
            charge.setInvoiceId(savedInvoice.getId());
            charge.setInvoiceNumber(savedInvoice.getInvoiceNumber());
        }
        chargeRepository.saveAll(charges);

        return savedInvoice;
    }

    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<InvoiceSummaryDTO> getAllInvoiceSummaries() {
        return invoiceRepository.findAllSummaries();
    }

    @Transactional(readOnly = true)
    public List<InvoiceSummaryDTO> getInvoiceSummariesByCustomer(Long customerId) {
        return invoiceRepository.findByCustomerId(customerId).stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InvoiceSummaryDTO> getInvoiceSummariesByAccount(String accountId) {
        return invoiceRepository.findByAccountId(accountId).stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InvoiceSummaryDTO> getUnpaidInvoiceSummariesByCustomer(Long customerId) {
        return invoiceRepository.findUnpaidInvoicesByCustomer(customerId).stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InvoiceSummaryDTO> getUnpaidInvoiceSummariesByAccount(String accountId) {
        return invoiceRepository.findUnpaidInvoicesByAccount(accountId).stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InvoiceSummaryDTO> getOverdueInvoiceSummaries() {
        return invoiceRepository.findOverdueInvoices().stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get invoice by ID
     */
    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
    }

    @Transactional(readOnly = true)
    public InvoiceDetailsDTO getInvoiceDetailsById(Long id) {
        Invoice invoice = getInvoiceById(id);

        // Fetch related collections separately to avoid MultipleBagFetchException
        List<InvoiceLineItem> lineItems = invoiceLineItemRepository.findByInvoiceId(id);
        List<Payment> payments = paymentRepository.findByInvoiceId(id);

        return toDetailsDTO(invoice, lineItems, payments);
    }

    /**
     * Get invoice by invoice number
     */
    public Invoice getInvoiceByNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
    }

    @Transactional(readOnly = true)
    public InvoiceDetailsDTO getInvoiceDetailsByNumber(String invoiceNumber) {
        Invoice invoice = getInvoiceByNumber(invoiceNumber);
        Long invoiceId = invoice.getId();
        List<InvoiceLineItem> lineItems = invoiceLineItemRepository.findByInvoiceId(invoiceId);
        List<Payment> payments = paymentRepository.findByInvoiceId(invoiceId);
        return toDetailsDTO(invoice, lineItems, payments);
    }

    private InvoiceSummaryDTO toSummaryDTO(Invoice invoice) {
        return InvoiceSummaryDTO.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .accountId(invoice.getAccountId())
                .customerId(invoice.getCustomer() != null ? invoice.getCustomer().getId() : null)
                .customerName(invoice.getCustomer() != null ? invoice.getCustomer().getCompanyName() : null)
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus())
                .totalAmount(invoice.getTotalAmount())
                .amountPaid(invoice.getAmountPaid())
                .balanceDue(invoice.getBalanceDue())
                .build();
    }

    private InvoiceDetailsDTO toDetailsDTO(Invoice invoice, List<InvoiceLineItem> lineItems, List<Payment> payments) {
        return InvoiceDetailsDTO.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .accountId(invoice.getAccountId())
                .customerId(invoice.getCustomer() != null ? invoice.getCustomer().getId() : null)
                .customerName(invoice.getCustomer() != null ? invoice.getCustomer().getCompanyName() : null)
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .billingPeriodStart(invoice.getBillingPeriodStart())
                .billingPeriodEnd(invoice.getBillingPeriodEnd())
                .subtotal(invoice.getSubtotal())
                .taxRate(invoice.getTaxRate())
                .taxAmount(invoice.getTaxAmount())
                .totalAmount(invoice.getTotalAmount())
                .amountPaid(invoice.getAmountPaid())
                .balanceDue(invoice.getBalanceDue())
                .status(invoice.getStatus())
                .notes(invoice.getNotes())
                .terms(invoice.getTerms())
                .lineItems(lineItems.stream().map(li -> InvoiceDetailsDTO.LineItemDTO.builder()
                        .id(li.getId())
                        .chargeId(li.getCharge() != null ? li.getCharge().getId() : null)
                        .description(li.getDescription())
                        .tripDate(li.getTripDate())
                        .quantity(li.getQuantity())
                        .unitPrice(li.getUnitPrice())
                        .amount(li.getAmount())
                        .build()).collect(Collectors.toList()))
                .payments(payments.stream().map(p -> InvoiceDetailsDTO.PaymentDTO.builder()
                        .id(p.getId())
                        .paymentNumber(p.getPaymentNumber())
                        .paymentDate(p.getPaymentDate())
                        .amount(p.getAmount())
                        .paymentMethod(p.getPaymentMethod())
                        .referenceNumber(p.getReferenceNumber())
                        .notes(p.getNotes())
                        .build()).collect(Collectors.toList()))
                .build();
    }

    /**
     * Get all invoices for customer
     */
    public List<Invoice> getInvoicesByCustomer(Long customerId) {
        return invoiceRepository.findByCustomerId(customerId);
    }

    /**
     * Get all invoices for account
     */
    public List<Invoice> getInvoicesByAccount(String accountId) {
        return invoiceRepository.findByAccountId(accountId);
    }

    /**
     * Get unpaid invoices for customer
     */
    public List<Invoice> getUnpaidInvoicesByCustomer(Long customerId) {
        return invoiceRepository.findUnpaidInvoicesByCustomer(customerId);
    }

    /**
     * Get unpaid invoices for account
     */
    public List<Invoice> getUnpaidInvoicesByAccount(String accountId) {
        return invoiceRepository.findUnpaidInvoicesByAccount(accountId);
    }

    /**
     * Get overdue invoices
     */
    public List<Invoice> getOverdueInvoices() {
        return invoiceRepository.findOverdueInvoices();
    }

    /**
     * Mark invoice as sent
     */
    public InvoiceSummaryDTO sendInvoice(Long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        invoice.markAsSent();
        Invoice savedInvoice = invoiceRepository.save(invoice);
        return toSummaryDTO(savedInvoice);
    }

    /**
     * Cancel invoice
     */
    public InvoiceSummaryDTO cancelInvoice(Long invoiceId, String reason) {
        Invoice invoice = getInvoiceById(invoiceId);
        
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            throw new IllegalStateException("Cannot cancel a paid invoice");
        }
        
        if (invoice.getStatus() == Invoice.InvoiceStatus.PARTIAL) {
            throw new IllegalStateException("Cannot cancel a partially paid invoice");
        }
        
        invoice.markAsCancelled();
        if (reason != null) {
            invoice.setNotes((invoice.getNotes() != null ? invoice.getNotes() + "\n" : "") + 
                           "Cancelled: " + reason);
        }
        
        // Clear invoice reference from charges - release them for new invoices
        List<AccountCharge> charges = chargeRepository.findByInvoiceNumber(invoice.getInvoiceNumber());
        for (AccountCharge charge : charges) {
            charge.setInvoiceId(null);
            charge.setInvoiceNumber(null);
        }
        chargeRepository.saveAll(charges);
        
        Invoice savedInvoice = invoiceRepository.save(invoice);
        return toSummaryDTO(savedInvoice);
    }

    /**
     * Update invoice status (checks for overdue, updates based on payments)
     */
    public Invoice updateInvoiceStatus(Long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        invoice.updateStatus();
        return invoiceRepository.save(invoice);
    }

    /**
     * Record payment for invoice
     */
    public Payment recordPayment(Long invoiceId, BigDecimal amount, LocalDate paymentDate,
                                 Payment.PaymentMethod paymentMethod, String referenceNumber, 
                                 String notes, Long userId) {
        Invoice invoice = getInvoiceById(invoiceId);
        
        if (invoice.getStatus() == Invoice.InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Cannot record payment for cancelled invoice");
        }
        
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        
        if (amount.compareTo(invoice.getBalanceDue()) > 0) {
            throw new IllegalArgumentException(
                    "Payment amount exceeds balance due. Balance: " + invoice.getBalanceDue());
        }

        // Create payment
        Payment payment = Payment.builder()
                .paymentNumber(generatePaymentNumber())
                .invoice(invoice)
                .customer(invoice.getCustomer())
                .accountId(invoice.getAccountId())
                .paymentDate(paymentDate != null ? paymentDate : LocalDate.now())
                .amount(amount)
                .paymentMethod(paymentMethod)
                .referenceNumber(referenceNumber)
                .notes(notes)
                .createdBy(userId)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        
        // Update invoice
        invoice.addPayment(savedPayment);
        invoiceRepository.save(invoice);
        
        // If invoice is now paid, mark all associated charges as paid
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            markChargesAsPaid(invoice);
        }
        
        return savedPayment;
    }

    /**
     * Get all payments for invoice
     */
    public List<Payment> getPaymentsByInvoice(Long invoiceId) {
        return paymentRepository.findByInvoiceId(invoiceId);
    }

    /**
     * Get all payments for customer
     */
    public List<Payment> getPaymentsByCustomer(Long customerId) {
        return paymentRepository.findByCustomerId(customerId);
    }

    /**
     * Calculate outstanding balance for customer
     */
    public BigDecimal calculateOutstandingBalance(Long customerId) {
        return invoiceRepository.calculateOutstandingBalance(customerId);
    }

    /**
     * Calculate outstanding balance for account
     */
    public BigDecimal calculateOutstandingBalanceByAccount(String accountId) {
        return invoiceRepository.calculateOutstandingBalanceByAccount(accountId);
    }

    /**
     * Get invoice summary for customer
     */
    public Map<String, Object> getInvoiceSummary(Long customerId) {
        BigDecimal outstanding = calculateOutstandingBalance(customerId);
        List<Invoice> unpaidInvoices = getUnpaidInvoicesByCustomer(customerId);
        List<Invoice> allInvoices = getInvoicesByCustomer(customerId);
        
        long totalInvoices = allInvoices.size();
        long paidInvoices = allInvoices.stream()
                .filter(Invoice::isPaid)
                .count();
        long overdueInvoices = allInvoices.stream()
                .filter(Invoice::isOverdue)
                .count();
        
        BigDecimal totalInvoiced = allInvoices.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalPaid = allInvoices.stream()
                .map(Invoice::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return Map.of(
                "customerId", customerId,
                "totalInvoices", totalInvoices,
                "paidInvoices", paidInvoices,
                "unpaidInvoices", unpaidInvoices.size(),
                "overdueInvoices", overdueInvoices,
                "totalInvoiced", totalInvoiced,
                "totalPaid", totalPaid,
                "outstandingBalance", outstanding
        );
    }

    // ==================== Helper Methods ====================

    private String generateInvoiceNumber() {
        String prefix = "INV-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        String maxNumber = invoiceRepository.findMaxInvoiceNumberWithPrefix(prefix);
        
        if (maxNumber == null) {
            return prefix + "0001";
        }
        
        int lastNumber = Integer.parseInt(maxNumber.substring(maxNumber.lastIndexOf("-") + 1));
        return prefix + String.format("%04d", lastNumber + 1);
    }

    private String generatePaymentNumber() {
        String prefix = "PAY-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-";
        String maxNumber = paymentRepository.findMaxPaymentNumberWithPrefix(prefix);
        
        if (maxNumber == null) {
            return prefix + "0001";
        }
        
        int lastNumber = Integer.parseInt(maxNumber.substring(maxNumber.lastIndexOf("-") + 1));
        return prefix + String.format("%04d", lastNumber + 1);
    }

    private LocalDate calculateDueDate(AccountCustomer customer) {
        // Default: 30 days from invoice date
        int daysToAdd = 30;
        
        // Adjust based on billing period
        if (customer.getBillingPeriod() != null) {
            switch (customer.getBillingPeriod()) {
                case "WEEKLY":
                    daysToAdd = 7;
                    break;
                case "BI_WEEKLY":
                    daysToAdd = 14;
                    break;
                case "MONTHLY":
                    daysToAdd = 30;
                    break;
            }
        }
        
        return LocalDate.now().plusDays(daysToAdd);
    }

    private String getDefaultTerms() {
        return "Payment is due within 30 days of invoice date. " +
               "Late payments may be subject to additional charges. " +
               "Please include invoice number with payment.";
    }

    private void markChargesAsPaid(Invoice invoice) {
        List<AccountCharge> charges = chargeRepository
                .findByAccountCustomerIdAndTripDateBetween(
                        invoice.getCustomer().getId(),
                        invoice.getBillingPeriodStart(),
                        invoice.getBillingPeriodEnd());

        for (AccountCharge charge : charges) {
            if (invoice.getInvoiceNumber().equals(charge.getInvoiceNumber())) {
                charge.markAsPaid(invoice.getInvoiceNumber());
            }
        }
        chargeRepository.saveAll(charges);
    }

    /**
     * Generate PDF invoice as byte array
     */
    @Transactional(readOnly = true)
    public byte[] generateInvoicePDF(Long invoiceId) {
        Invoice invoice = getInvoiceById(invoiceId);
        // Fetch line items if needed
        List<InvoiceLineItem> lineItems = invoiceLineItemRepository.findByInvoiceId(invoiceId);
        if (lineItems != null && !lineItems.isEmpty()) {
            invoice.setLineItems(lineItems);
        }

        // Get company name from tenant config
        String companyName = tenantConfigService.getCurrentTenantConfig()
                .map(config -> config.getCompanyName())
                .orElse("Maclures Cabs");

        return pdfService.generateInvoicePDF(invoice, companyName);
    }

    /**
     * Send invoice via email to customer
     */
    public void sendInvoiceViaEmail(Long invoiceId, String recipientEmail) {
        Invoice invoice = getInvoiceById(invoiceId);

        // Generate PDF
        byte[] pdfContent = generateInvoicePDF(invoiceId);

        // Get company name from tenant config
        String companyName = tenantConfigService.getCurrentTenantConfig()
                .map(config -> config.getCompanyName())
                .orElse("Maclures Cabs");

        // Send email
        emailService.sendInvoiceEmail(invoice, pdfContent, recipientEmail, companyName);

        // Update invoice status to SENT
        invoice.markAsSent();

        // Record email send for resend tracking
        invoice.recordEmailSent(recipientEmail);

        invoiceRepository.save(invoice);
    }
}
