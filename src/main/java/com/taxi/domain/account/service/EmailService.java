package com.taxi.domain.account.service;

import com.taxi.domain.account.model.Invoice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String emailFrom;

    @Value("${app.mail.sender-name}")
    private String senderName;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send invoice via email to customer with PDF attachment (backward compatibility)
     */
    public void sendInvoiceEmail(Invoice invoice, byte[] pdfContent, String recipientEmail) {
        sendInvoiceEmail(invoice, pdfContent, recipientEmail, "Maclures Cabs");
    }

    /**
     * Send invoice via email to customer with PDF attachment with company name
     */
    public void sendInvoiceEmail(Invoice invoice, byte[] pdfContent, String recipientEmail, String companyName) {
        try {
            log.info("========== EMAIL DEBUG START ==========");
            log.info("Attempting to send invoice email");
            log.info("From: {}", emailFrom);
            log.info("Sender Name: {}", senderName);
            log.info("To: {}", recipientEmail);
            log.info("Invoice: {}", invoice.getInvoiceNumber());
            log.info("PDF Content Size: {} bytes", pdfContent.length);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set email headers
            try {
                helper.setFrom(emailFrom, senderName);
                log.info("Email From set with sender name");
            } catch (UnsupportedEncodingException e) {
                log.warn("Could not set sender name, using email only: {}", e.getMessage());
                helper.setFrom(emailFrom);
            }
            helper.setTo(recipientEmail);
            helper.setSubject("Invoice " + invoice.getInvoiceNumber() + " from FareFlow");

            // Format amounts
            DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00");
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MMM dd, yyyy");

            // Build HTML email body
            String emailBody = buildInvoiceEmailBody(invoice, currencyFormat, dateFormat, companyName);
            helper.setText(emailBody, true);

            // Attach PDF
            helper.addAttachment("Invoice_" + invoice.getInvoiceNumber() + ".pdf",
                    new ByteArrayResource(pdfContent));
            log.info("PDF attachment added");

            log.info("Sending email via JavaMailSender...");
            mailSender.send(message);
            log.info("========== EMAIL SENT SUCCESSFULLY ==========");
            log.info("Invoice email sent to {} for invoice {}", recipientEmail, invoice.getInvoiceNumber());

        } catch (MessagingException e) {
            log.error("========== EMAIL FAILED ==========");
            log.error("MessagingException while sending invoice email to {}", recipientEmail);
            log.error("Exception Type: {}", e.getClass().getName());
            log.error("Exception Message: {}", e.getMessage());
            log.error("Exception Cause: {}", e.getCause());
            log.error("Stack Trace:", e);
            throw new RuntimeException("Failed to send invoice email: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("========== UNEXPECTED ERROR ==========");
            log.error("Unexpected exception while sending invoice email", e);
            throw new RuntimeException("Failed to send invoice email: " + e.getMessage(), e);
        }
    }

    /**
     * Build HTML email body for invoice
     */
    private String buildInvoiceEmailBody(Invoice invoice, DecimalFormat currencyFormat,
                                        DateTimeFormatter dateFormat, String companyName) {
        StringBuilder html = new StringBuilder();

        html.append("<html><body style=\"font-family: Arial, sans-serif; color: #333;\">");
        html.append("<div style=\"max-width: 600px; margin: 0 auto;\">");

        // Header
        html.append("<div style=\"background-color: #3e5244; color: white; padding: 20px; text-align: center; border-radius: 4px;\">");
        html.append("<h1 style=\"margin: 0; font-size: 24px;\">🚕 FareFlow Invoice</h1>");
        html.append("<p style=\"margin: 5px 0 0 0; font-size: 14px;\">").append(companyName != null ? companyName : "Maclures Cabs").append("</p>");
        html.append("</div>");

        // Customer and Invoice Details
        html.append("<div style=\"margin: 20px 0; padding: 15px; background-color: #f5f5f5; border-radius: 4px;\">");
        html.append("<div style=\"display: flex; justify-content: space-between;\">");

        html.append("<div>");
        html.append("<h3 style=\"margin: 0 0 10px 0; color: #3e5244;\">Bill To:</h3>");
        html.append("<p style=\"margin: 5px 0;\"><strong>").append(invoice.getCustomer().getCompanyName()).append("</strong></p>");
        html.append("<p style=\"margin: 5px 0;\">").append(invoice.getCustomer().getContactPerson()).append("</p>");
        html.append("<p style=\"margin: 5px 0;\">").append(invoice.getCustomer().getStreetAddress()).append("</p>");
        html.append("<p style=\"margin: 5px 0;\">")
                .append(invoice.getCustomer().getCity()).append(", ")
                .append(invoice.getCustomer().getProvince()).append(" ")
                .append(invoice.getCustomer().getPostalCode()).append("</p>");
        html.append("<p style=\"margin: 5px 0;\">").append(invoice.getCustomer().getCountry()).append("</p>");
        html.append("</div>");

        html.append("<div style=\"text-align: right;\">");
        html.append("<p style=\"margin: 5px 0;\"><strong>Invoice #:</strong> ").append(invoice.getInvoiceNumber()).append("</p>");
        html.append("<p style=\"margin: 5px 0;\"><strong>Invoice Date:</strong> ").append(invoice.getInvoiceDate().format(dateFormat)).append("</p>");
        html.append("<p style=\"margin: 5px 0;\"><strong>Due Date:</strong> ").append(invoice.getDueDate().format(dateFormat)).append("</p>");
        html.append("<p style=\"margin: 5px 0;\"><strong>Billing Period:</strong></p>");
        html.append("<p style=\"margin: 2px 0;\">").append(invoice.getBillingPeriodStart().format(dateFormat))
                .append(" to ").append(invoice.getBillingPeriodEnd().format(dateFormat)).append("</p>");
        html.append("</div>");

        html.append("</div>");
        html.append("</div>");

        // Invoice summary box
        html.append("<div style=\"margin: 20px 0; padding: 15px; background-color: #e8f5e9; border-left: 4px solid #4caf50; border-radius: 4px;\">");
        html.append("<h3 style=\"margin: 0 0 10px 0; color: #3e5244;\">Invoice Summary</h3>");

        html.append("<div style=\"display: flex; justify-content: space-between; margin: 10px 0;\">");
        html.append("<span>Subtotal:</span>");
        html.append("<strong>").append(currencyFormat.format(invoice.getSubtotal())).append("</strong>");
        html.append("</div>");

        if (invoice.getTaxRate().compareTo(BigDecimal.ZERO) > 0) {
            html.append("<div style=\"display: flex; justify-content: space-between; margin: 10px 0;\">");
            html.append("<span>Tax (").append(invoice.getTaxRate().stripTrailingZeros().toPlainString()).append("%):</span>");
            html.append("<strong>").append(currencyFormat.format(invoice.getTaxAmount())).append("</strong>");
            html.append("</div>");
        }

        html.append("<div style=\"display: flex; justify-content: space-between; margin: 10px 0; font-size: 18px; border-top: 2px solid #4caf50; padding-top: 10px;\">");
        html.append("<span><strong>Total:</strong></span>");
        html.append("<strong style=\"color: #4caf50;\">").append(currencyFormat.format(invoice.getTotalAmount())).append("</strong>");
        html.append("</div>");

        if (invoice.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            html.append("<div style=\"display: flex; justify-content: space-between; margin: 10px 0;\">");
            html.append("<span>Amount Paid:</span>");
            html.append("<strong>").append(currencyFormat.format(invoice.getAmountPaid())).append("</strong>");
            html.append("</div>");

            html.append("<div style=\"display: flex; justify-content: space-between; margin: 10px 0; font-size: 16px; padding-top: 10px; border-top: 1px solid #ddd;\">");
            html.append("<span><strong>Balance Due:</strong></span>");
            html.append("<strong style=\"color: #d32f2f;\">").append(currencyFormat.format(invoice.getBalanceDue())).append("</strong>");
            html.append("</div>");
        } else {
            html.append("<div style=\"display: flex; justify-content: space-between; margin: 10px 0; font-size: 16px; padding-top: 10px; border-top: 1px solid #ddd;\">");
            html.append("<span><strong>Balance Due:</strong></span>");
            html.append("<strong style=\"color: #d32f2f;\">").append(currencyFormat.format(invoice.getBalanceDue())).append("</strong>");
            html.append("</div>");
        }

        html.append("</div>");

        // Terms
        if (invoice.getTerms() != null && !invoice.getTerms().isEmpty()) {
            html.append("<div style=\"margin: 20px 0; padding: 15px; background-color: #fff3e0; border-left: 4px solid #ff9800; border-radius: 4px;\">");
            html.append("<h3 style=\"margin: 0 0 10px 0; color: #3e5244;\">Terms & Conditions</h3>");
            html.append("<p style=\"margin: 0; line-height: 1.6;\">").append(invoice.getTerms()).append("</p>");
            html.append("</div>");
        }

        // Footer
        html.append("<div style=\"margin: 20px 0; padding: 15px; text-align: center; border-top: 1px solid #ddd; color: #666; font-size: 12px;\">");
        html.append("<p style=\"margin: 5px 0;\">Thank you for your business!</p>");
        html.append("<p style=\"margin: 5px 0;\">Please see attached PDF for complete invoice details.</p>");
        html.append("<p style=\"margin: 5px 0;\">For questions about this invoice, please contact us.</p>");
        html.append("</div>");

        html.append("</div></body></html>");

        return html.toString();
    }

    /**
     * Send test email
     */
    public void sendTestEmail(String recipientEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(recipientEmail);
            message.setSubject("Test Email from FareFlow");
            message.setText("This is a test email from FareFlow Invoice System.\n\n" +
                    "If you received this email, the email configuration is working correctly.");

            mailSender.send(message);
            log.info("Test email sent to {}", recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send test email: {}", e.getMessage());
            throw new RuntimeException("Failed to send test email: " + e.getMessage());
        }
    }
}
