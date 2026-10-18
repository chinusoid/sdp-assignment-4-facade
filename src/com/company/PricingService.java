package com.company;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/** Applies rental duration rules, a weekly discount and currency rounding. */
public class PricingService {
    private static final int MAX_DAYS = 14;
    private static final int WEEKLY_DISCOUNT_START = 7;
    private static final BigDecimal WEEKLY_MULTIPLIER = new BigDecimal("0.90");

    public RentalQuote calculate(Equipment equipment, int days) {
        Objects.requireNonNull(equipment, "Equipment is required.");
        if (days < 1 || days > MAX_DAYS) {
            throw new IllegalArgumentException("Rental duration must be between 1 and 14 days.");
        }
        BigDecimal fee = equipment.dailyRate().multiply(BigDecimal.valueOf(days));
        if (days >= WEEKLY_DISCOUNT_START) {
            fee = fee.multiply(WEEKLY_MULTIPLIER);
        }
        return new RentalQuote(days, fee.setScale(2, RoundingMode.HALF_UP),
                equipment.deposit().setScale(2, RoundingMode.HALF_UP));
    }
}
