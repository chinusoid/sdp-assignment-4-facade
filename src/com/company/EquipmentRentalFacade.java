package com.company;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/** The client supplies three values instead of coordinating five services. */
public class EquipmentRentalFacade {
    private final StudentService students;
    private final InventoryService inventory;
    private final PricingService pricing;
    private final PaymentService payments;
    private final RentalRegistry rentals;
    private final Clock clock;

    public EquipmentRentalFacade() {
        this(new StudentService(), new InventoryService(), new PricingService(),
                new PaymentService(), new RentalRegistry(), Clock.systemDefaultZone());
    }

    // Package-private wiring supports isolated tests without expanding the public API.
    EquipmentRentalFacade(StudentService students, InventoryService inventory,
                          PricingService pricing, PaymentService payments,
                          RentalRegistry rentals, Clock clock) {
        this.students = Objects.requireNonNull(students);
        this.inventory = Objects.requireNonNull(inventory);
        this.pricing = Objects.requireNonNull(pricing);
        this.payments = Objects.requireNonNull(payments);
        this.rentals = Objects.requireNonNull(rentals);
        this.clock = Objects.requireNonNull(clock);
    }

    public RentalReceipt rentEquipment(String studentId, String equipmentId, int days) {
        String reservationId = null;
        String paymentId = null;
        try {
            Student student = students.requireEligible(studentId);
            Equipment equipment = inventory.find(equipmentId);
            RentalQuote quote = pricing.calculate(equipment, days);
            reservationId = inventory.reserve(equipment.id());
            paymentId = payments.charge(student.id(), quote.total());
            return rentals.register(student, equipment, quote, LocalDate.now(clock));
        } catch (RuntimeException failure) {
            // Undo completed changes in reverse order; preserve the original failure.
            if (paymentId != null) {
                try {
                    payments.refund(paymentId);
                } catch (RuntimeException refundFailure) {
                    failure.addSuppressed(refundFailure);
                }
            }
            if (reservationId != null) {
                try {
                    inventory.release(reservationId);
                } catch (RuntimeException releaseFailure) {
                    failure.addSuppressed(releaseFailure);
                }
            }
            throw new RentalException("Rental could not be completed: " + failure.getMessage(), failure);
        }
    }
}
