package com.company;

import java.math.BigDecimal;

public record Equipment(String id, String name, BigDecimal dailyRate, BigDecimal deposit) {
    public Equipment {
        if (id == null || id.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Equipment ID and name are required.");
        }
        if (dailyRate == null || dailyRate.signum() <= 0
                || deposit == null || deposit.signum() < 0) {
            throw new IllegalArgumentException("The daily rate must be positive and deposit non-negative.");
        }
    }
}
