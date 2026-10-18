package com.company;

import java.math.BigDecimal;

public record RentalQuote(int days, BigDecimal rentalFee, BigDecimal deposit) {
    public RentalQuote {
        if (days < 1 || rentalFee == null || rentalFee.signum() <= 0
                || deposit == null || deposit.signum() < 0) {
            throw new IllegalArgumentException("A quote needs positive days and fee, and a non-negative deposit.");
        }
    }

    public BigDecimal total() {
        return rentalFee.add(deposit);
    }
}
