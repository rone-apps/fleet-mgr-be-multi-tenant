package com.taxi.web.dto.expense;

import com.taxi.domain.expense.model.OneTimeExpense.PaidBy;
import com.taxi.domain.expense.model.OneTimeExpense.ResponsibleParty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOneTimeExpenseRequest {

    @NotNull(message = "Expense category ID is required")
    private Long expenseCategoryId;

    @NotBlank(message = "Expense name is required")
    private String name;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;

    @NotNull(message = "Paid by is required")
    private PaidBy paidBy;

    private ResponsibleParty responsibleParty;

    private String description;

    private String vendor;

    private String receiptUrl;

    private String invoiceNumber;

    private boolean isReimbursable;

    private String notes;
}
