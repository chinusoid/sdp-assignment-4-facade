package com.company;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Assigns rental IDs, calculates due dates and stores rental records. */
public class RentalRegistry {
    private final Map<String, RentalReceipt> rentals = new LinkedHashMap<>();
    private int nextId = 1;

    public RentalReceipt register(Student student, Equipment equipment,
                                  RentalQuote quote, LocalDate startDate) {
        Objects.requireNonNull(student, "Student is required.");
        Objects.requireNonNull(equipment, "Equipment is required.");
        Objects.requireNonNull(quote, "Quote is required.");
        Objects.requireNonNull(startDate, "Start date is required.");
        String rentalId = "RENT-%03d".formatted(nextId);
        RentalReceipt receipt = new RentalReceipt(rentalId, student.name(), equipment.name(),
                quote.days(), startDate.plusDays(quote.days()), quote.rentalFee(), quote.deposit());
        rentals.put(rentalId, receipt);
        nextId++;
        return receipt;
    }

    public Map<String, RentalReceipt> allRentals() {
        return Map.copyOf(rentals);
    }
}
