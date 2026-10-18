package com.company;

import java.math.BigDecimal;
import java.time.LocalDate;

/** An immutable result for the client; it exposes no subsystem objects. */
public record RentalReceipt(String rentalId, String studentName, String equipmentName,
                            int days, LocalDate dueDate, BigDecimal rentalFee,
                            BigDecimal deposit) {
    public BigDecimal totalPaid() {
        return rentalFee.add(deposit);
    }
}
