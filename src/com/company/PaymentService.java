package com.company;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A simulated student wallet, with charges and idempotent refunds. */
public class PaymentService {
    private final Map<String, BigDecimal> balances = new HashMap<>(Map.of(
            "ST1001", new BigDecimal("50000.00"),
            "ST1002", new BigDecimal("1000.00"),
            "ST1003", new BigDecimal("50000.00")));
    private final Map<String, Charge> charges = new HashMap<>();

    public BigDecimal balanceOf(String studentId) {
        if (studentId == null || !balances.containsKey(studentId)) {
            throw new IllegalArgumentException("Wallet not found: " + studentId);
        }
        return balances.get(studentId);
    }

    public String charge(String studentId, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive.");
        }
        BigDecimal payable = amount.setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal balance = balanceOf(studentId);
        if (balance.compareTo(payable) < 0) {
            throw new IllegalStateException("Insufficient wallet balance for student " + studentId);
        }
        String paymentId = UUID.randomUUID().toString();
        charges.put(paymentId, new Charge(studentId, payable));
        balances.put(studentId, balance.subtract(payable));
        return paymentId;
    }

    public void refund(String paymentId) {
        Charge charge = charges.remove(paymentId);
        if (charge != null) {
            balances.compute(charge.studentId(), (id, balance) -> balance.add(charge.amount()));
        }
    }

    private record Charge(String studentId, BigDecimal amount) { }
}
