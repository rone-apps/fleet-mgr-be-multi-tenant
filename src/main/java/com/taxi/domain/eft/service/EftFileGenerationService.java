package com.taxi.domain.eft.service;

import com.taxi.domain.account.model.PaymentBatch;
import com.taxi.domain.account.model.StatementPayment;
import com.taxi.domain.account.repository.PaymentBatchRepository;
import com.taxi.domain.account.repository.StatementPaymentRepository;
import com.taxi.domain.eft.model.BankAccount;
import com.taxi.domain.eft.model.EftConfig;
import com.taxi.domain.eft.model.EftFileGeneration;
import com.taxi.domain.eft.repository.BankAccountRepository;
import com.taxi.domain.eft.repository.EftConfigRepository;
import com.taxi.domain.eft.repository.EftFileGenerationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoField;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EftFileGenerationService {

    private static final int SEGMENT_LENGTH = 240;
    private static final int SEGMENTS_PER_RECORD = 6;
    private static final int RECORD_LENGTH = 1464;

    private final EftConfigRepository eftConfigRepository;
    private final BankAccountRepository bankAccountRepository;
    private final EftFileGenerationRepository eftFileGenerationRepository;
    private final PaymentBatchRepository paymentBatchRepository;
    private final StatementPaymentRepository statementPaymentRepository;

    /**
     * Generate a CPA Standard 005 EFT file for a posted payment batch.
     * Returns the file content as a string and records the generation.
     */
    @Transactional
    public EftGenerationResult generateEftFile(Long batchId, String generatedBy) {
        PaymentBatch batch = paymentBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Payment batch not found: " + batchId));

        if (!"POSTED".equals(batch.getStatus()) && !"PROCESSED".equals(batch.getStatus())) {
            throw new IllegalStateException("Batch must be POSTED or PROCESSED to generate EFT. Current status: " + batch.getStatus());
        }

        EftConfig config = eftConfigRepository.findByIsActiveTrue()
                .orElseThrow(() -> new IllegalStateException("No active EFT configuration found. Please configure EFT settings first."));

        List<StatementPayment> payments = statementPaymentRepository.findByPaymentBatchId(batchId);
        if (payments.isEmpty()) {
            throw new IllegalStateException("No payments found in batch " + batchId);
        }

        // Filter to only Direct Deposit payments (DD method code)
        List<StatementPayment> ddPayments = payments.stream()
                .filter(p -> p.getPaymentMethod() != null && "DD".equals(p.getPaymentMethod().getMethodCode()))
                .filter(p -> p.getAmount() != null && p.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();

        if (ddPayments.isEmpty()) {
            throw new IllegalStateException("No Direct Deposit payments found in this batch. EFT only generates for DD payment method.");
        }

        // Look up bank accounts for each payee
        List<EftPaymentRecord> eftRecords = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (StatementPayment payment : ddPayments) {
            Optional<BankAccount> bankAccountOpt = bankAccountRepository.findByDriverIdAndIsActiveTrue(payment.getPersonId());
            if (bankAccountOpt.isEmpty()) {
                warnings.add(payment.getPersonName() + " (ID: " + payment.getPersonId() + ") - No active bank account on file");
                continue;
            }
            BankAccount bankAccount = bankAccountOpt.get();
            eftRecords.add(new EftPaymentRecord(payment, bankAccount));
        }

        if (eftRecords.isEmpty()) {
            throw new IllegalStateException("No payments could be included in EFT file. " +
                    warnings.size() + " payment(s) skipped due to missing bank accounts.");
        }

        // Get and increment file creation number
        int fileCreationNum = config.getNextFileCreationNumber();
        eftConfigRepository.save(config);

        // Build the file
        LocalDate processingDate = batch.getBatchDate();
        StringBuilder fileContent = new StringBuilder();

        // Header record (Type A)
        fileContent.append(buildHeaderRecord(config, fileCreationNum, processingDate));
        fileContent.append("\n");

        // Detail records (Type C for credits)
        BigDecimal totalCredits = BigDecimal.ZERO;
        int creditCount = 0;

        // Pack segments into physical records (6 segments per record)
        List<String> segments = new ArrayList<>();
        for (EftPaymentRecord rec : eftRecords) {
            String segment = buildCreditSegment(config, rec, processingDate);
            segments.add(segment);
            totalCredits = totalCredits.add(rec.payment.getAmount());
            creditCount++;
        }

        // Pack 6 segments per physical record
        for (int i = 0; i < segments.size(); i += SEGMENTS_PER_RECORD) {
            StringBuilder physicalRecord = new StringBuilder();
            for (int j = i; j < Math.min(i + SEGMENTS_PER_RECORD, segments.size()); j++) {
                physicalRecord.append(segments.get(j));
            }
            // Pad to 1464 characters
            fileContent.append(padRight(physicalRecord.toString(), RECORD_LENGTH));
            fileContent.append("\n");
        }

        // Trailer record (Type Z)
        fileContent.append(buildTrailerRecord(config, fileCreationNum, totalCredits, creditCount));
        fileContent.append("\n");

        // Generate filename
        String fileName = String.format("EFT_%s_%s_%04d.txt",
                config.getOriginatorShortName().trim().replaceAll("\\s+", "_"),
                processingDate.toString().replace("-", ""),
                fileCreationNum);

        // Record the generation
        EftFileGeneration generation = EftFileGeneration.builder()
                .batchId(batchId)
                .fileCreationNumber(fileCreationNum)
                .fileName(fileName)
                .recordCount(creditCount)
                .totalCreditAmount(totalCredits)
                .totalDebitAmount(BigDecimal.ZERO)
                .status("GENERATED")
                .generatedBy(generatedBy)
                .build();
        eftFileGenerationRepository.save(generation);

        log.info("Generated EFT file {} for batch {}: {} records, total ${}", fileName, batchId, creditCount, totalCredits);

        EftGenerationResult result = new EftGenerationResult();
        result.fileContent = fileContent.toString();
        result.fileName = fileName;
        result.recordCount = creditCount;
        result.totalAmount = totalCredits;
        result.warnings = warnings;
        result.generationId = generation.getId();
        return result;
    }

    /**
     * Get generation history for a batch.
     */
    public List<EftFileGeneration> getGenerationHistory(Long batchId) {
        return eftFileGenerationRepository.findByBatchId(batchId);
    }

    // ==================== CPA 005 Record Builders ====================

    private String buildHeaderRecord(EftConfig config, int fileCreationNum, LocalDate date) {
        StringBuilder rec = new StringBuilder();
        rec.append("A");                                                // 1: Record type
        rec.append("1");                                                // 2: Record count
        rec.append(padRight(config.getOriginatorId(), 10));             // 3-12: Originator ID
        rec.append(padLeft(String.valueOf(fileCreationNum), 4, '0'));   // 13-16: File creation number
        rec.append(toJulianDate(date));                                 // 17-22: Creation date (0YYDDD)
        rec.append(padRight(config.getProcessingCentre(), 5));          // 23-27: Processing centre
        rec.append(padRight("", 21));                                   // 28-48: Filler
        rec.append(padRight(config.getCurrencyCode(), 3));              // 49-51: Currency
        rec.append(padRight("", 1413));                                 // 52-1464: Filler
        return padRight(rec.toString(), RECORD_LENGTH);
    }

    private String buildCreditSegment(EftConfig config, EftPaymentRecord rec, LocalDate fundsDate) {
        StatementPayment payment = rec.payment;
        BankAccount bank = rec.bankAccount;

        // Amount in cents
        long amountCents = payment.getAmount().movePointRight(2).longValue();

        StringBuilder seg = new StringBuilder();
        seg.append("C");                                                          // 1: Record type
        seg.append(padRight(config.getTransactionCode(), 3));                     // 2-4: Transaction code (200=Payroll)
        seg.append(padLeft(String.valueOf(amountCents), 10, '0'));                 // 5-14: Amount in cents
        seg.append(toJulianDate(fundsDate));                                      // 15-20: Date funds available
        seg.append("0");                                                          // 21: Leading zero
        seg.append(padLeft(bank.getInstitutionNumber(), 3, '0'));                  // 22-24: Payee institution
        seg.append(padLeft(bank.getTransitNumber(), 5, '0'));                      // 25-29: Payee transit
        seg.append(padRight(bank.getAccountNumber(), 12));                         // 30-41: Payee account
        seg.append(padRight(payment.getPaymentNumber() != null ? payment.getPaymentNumber() : "", 25)); // 42-66: Item trace
        seg.append("000");                                                        // 67-69: Stored txn type
        seg.append(padRight(config.getOriginatorShortName(), 15));                 // 70-84: Originator short name
        seg.append(padRight(bank.getAccountHolderName(), 30));                     // 85-114: Payee name
        seg.append(padRight(config.getOriginatorLongName(), 15));                  // 115-129: Originator long name
        seg.append(padRight(config.getOriginatorId(), 10));                        // 130-139: Originator user ID
        seg.append(padRight(payment.getPersonName() != null ? payment.getPersonName() : "", 25)); // 140-164: Cross-reference
        seg.append("0");                                                          // 165: Return routing leading zero
        seg.append(padLeft(config.getReturnInstitutionId(), 3, '0'));              // 166-168: Return institution
        seg.append(padLeft(config.getReturnTransitNumber(), 5, '0'));              // 169-173: Return transit
        seg.append(padRight(config.getReturnAccountNumber(), 12));                 // 174-185: Return account
        seg.append(padRight("", 15));                                             // 186-200: Sundry info
        seg.append(padRight("", 22));                                             // 201-222: Filler + sundry
        seg.append(padRight("", 18));                                             // 223-240: Filler

        return padRight(seg.toString(), SEGMENT_LENGTH);
    }

    private String buildTrailerRecord(EftConfig config, int fileCreationNum, BigDecimal totalCredits, int creditCount) {
        long totalCreditCents = totalCredits.movePointRight(2).longValue();

        StringBuilder rec = new StringBuilder();
        rec.append("Z");                                                // 1: Record type
        rec.append(padRight("", 3));                                    // 2-4: Filler
        rec.append(padRight(config.getOriginatorId(), 10));             // 5-14: Originator ID
        rec.append(padLeft(String.valueOf(fileCreationNum), 8, '0'));   // 15-22: File creation number
        rec.append(padLeft("0", 14, '0'));                              // 23-36: Total debits (zero - credits only)
        rec.append(padLeft("0", 8, '0'));                               // 37-44: Debit count
        rec.append(padLeft(String.valueOf(totalCreditCents), 14, '0')); // 45-58: Total credits in cents
        rec.append(padLeft(String.valueOf(creditCount), 8, '0'));       // 59-66: Credit count
        rec.append(padLeft("0", 14, '0'));                              // 67-80: Error correction debits
        rec.append(padLeft("0", 8, '0'));                               // 81-88: Error correction debit count
        rec.append(padLeft("0", 14, '0'));                              // 89-102: Error correction credits
        rec.append(padLeft("0", 8, '0'));                               // 103-110: Error correction credit count
        rec.append(padRight("", 1354));                                 // 111-1464: Filler

        return padRight(rec.toString(), RECORD_LENGTH);
    }

    // ==================== Utility Methods ====================

    /**
     * Convert a LocalDate to CPA Julian format: 0YYDDD
     */
    private String toJulianDate(LocalDate date) {
        int year = date.getYear() % 100;
        int dayOfYear = date.get(ChronoField.DAY_OF_YEAR);
        return "0" + padLeft(String.valueOf(year), 2, '0') + padLeft(String.valueOf(dayOfYear), 3, '0');
    }

    private static String padRight(String str, int len) {
        if (str == null) str = "";
        if (str.length() >= len) return str.substring(0, len);
        return str + " ".repeat(len - str.length());
    }

    private static String padLeft(String str, int len, char pad) {
        if (str == null) str = "";
        if (str.length() >= len) return str.substring(0, len);
        return String.valueOf(pad).repeat(len - str.length()) + str;
    }

    // ==================== Inner Classes ====================

    private record EftPaymentRecord(StatementPayment payment, BankAccount bankAccount) {}

    public static class EftGenerationResult {
        public String fileContent;
        public String fileName;
        public int recordCount;
        public BigDecimal totalAmount;
        public List<String> warnings;
        public Long generationId;
    }
}
