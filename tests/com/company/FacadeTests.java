package com.company;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;

/** Dependency-free behavioral tests. Every check runs without JVM assertion flags. */
public class FacadeTests {
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-10-18T10:00:00Z"), ZoneOffset.UTC);
    private static int passed;

    public static void main(String[] args) {
        run("successful rental updates all subsystem state", FacadeTests::successfulRental);
        run("duration boundaries and weekly discount", FacadeTests::pricingBoundaries);
        run("money is rounded to two decimal places", FacadeTests::currencyRounding);
        run("unavailable equipment never charges a wallet", FacadeTests::unavailableEquipment);
        run("declined payment releases stock for the next client", FacadeTests::declinedPayment);
        run("inactive student cannot change state", FacadeTests::inactiveStudent);
        run("invalid inputs leave subsystem state unchanged", FacadeTests::invalidInputs);
        run("registry failure refunds money and releases stock", FacadeTests::registryFailure);
        run("cleanup failure preserves the original error", FacadeTests::cleanupFailure);
        run("refund and release are idempotent", FacadeTests::idempotentCompensation);
        run("advanced clients can use subsystems directly", FacadeTests::directSubsystemUse);
        run("registry returns immutable snapshots", FacadeTests::immutableSnapshot);
        System.out.println("\nAll " + passed + " tests passed.");
    }

    private static void successfulRental() {
        Fixture fixture = new Fixture();
        RentalReceipt receipt = fixture.facade.rentEquipment("ST1001", "CAMERA", 3);
        equal("RENT-001", receipt.rentalId());
        equal("Aida", receipt.studentName());
        equal(LocalDate.of(2026, 10, 21), receipt.dueDate());
        money("4500.00", receipt.rentalFee());
        money("12500.00", receipt.totalPaid());
        money("37500.00", fixture.payments.balanceOf("ST1001"));
        equal(0, fixture.inventory.available("CAMERA"));
        equal(receipt, fixture.rentals.allRentals().get(receipt.rentalId()));
    }

    private static void pricingBoundaries() {
        Fixture fixture = new Fixture();
        Equipment camera = fixture.inventory.find("CAMERA");
        money("1500.00", fixture.pricing.calculate(camera, 1).rentalFee());
        money("9000.00", fixture.pricing.calculate(camera, 6).rentalFee());
        money("9450.00", fixture.pricing.calculate(camera, 7).rentalFee());
        money("18900.00", fixture.pricing.calculate(camera, 14).rentalFee());
        expect(IllegalArgumentException.class, () -> fixture.pricing.calculate(camera, 0));
        expect(IllegalArgumentException.class, () -> fixture.pricing.calculate(camera, 15));
    }

    private static void currencyRounding() {
        Equipment item = new Equipment("TEST", "Test equipment",
                new BigDecimal("9.999"), new BigDecimal("0.014"));
        RentalQuote quote = new PricingService().calculate(item, 7);
        money("62.99", quote.rentalFee());
        money("0.01", quote.deposit());
        money("63.00", quote.total());
    }

    private static void unavailableEquipment() {
        Fixture fixture = new Fixture();
        fixture.facade.rentEquipment("ST1001", "CAMERA", 1);
        BigDecimal balance = fixture.payments.balanceOf("ST1001");
        expect(RentalException.class, () -> fixture.facade.rentEquipment("ST1001", "CAMERA", 1));
        equal(balance, fixture.payments.balanceOf("ST1001"));
        equal(1, fixture.rentals.allRentals().size());
        equal(0, fixture.inventory.available("CAMERA"));
    }

    private static void declinedPayment() {
        Fixture fixture = new Fixture();
        expect(RentalException.class, () -> fixture.facade.rentEquipment("ST1002", "PROJECTOR", 2));
        equal(1, fixture.inventory.available("PROJECTOR"));
        equal(0, fixture.rentals.allRentals().size());
        money("1000.00", fixture.payments.balanceOf("ST1002"));
        fixture.facade.rentEquipment("ST1001", "PROJECTOR", 2);
        equal(0, fixture.inventory.available("PROJECTOR"));
    }

    private static void inactiveStudent() {
        Fixture fixture = new Fixture();
        expect(RentalException.class, () -> fixture.facade.rentEquipment("ST1003", "CAMERA", 1));
        unchanged(fixture);
        money("50000.00", fixture.payments.balanceOf("ST1003"));
    }

    private static void invalidInputs() {
        Fixture fixture = new Fixture();
        for (String id : new String[] { null, "", " ", "UNKNOWN" }) {
            expect(RentalException.class, () -> fixture.facade.rentEquipment(id, "CAMERA", 1));
            expect(RentalException.class, () -> fixture.facade.rentEquipment("ST1001", id, 1));
        }
        for (int days : new int[] { -1, 0, 15, Integer.MAX_VALUE }) {
            expect(RentalException.class, () -> fixture.facade.rentEquipment("ST1001", "CAMERA", days));
        }
        unchanged(fixture);
    }

    private static void registryFailure() {
        Fixture fixture = new Fixture(new InventoryService(), failingRegistry());
        RentalException failure = expect(RentalException.class,
                () -> fixture.facade.rentEquipment("ST1001", "CAMERA", 3));
        equal("Registry temporarily unavailable.", failure.getCause().getMessage());
        unchanged(fixture);
    }

    private static void cleanupFailure() {
        InventoryService inventory = new InventoryService() {
            @Override
            public void release(String reservationId) {
                throw new IllegalStateException("Stock release failed.");
            }
        };
        Fixture fixture = new Fixture(inventory, failingRegistry());
        RentalException failure = expect(RentalException.class,
                () -> fixture.facade.rentEquipment("ST1001", "CAMERA", 3));
        equal("Registry temporarily unavailable.", failure.getCause().getMessage());
        equal(1, failure.getCause().getSuppressed().length);
        money("50000.00", fixture.payments.balanceOf("ST1001"));
    }

    private static RentalRegistry failingRegistry() {
        return new RentalRegistry() {
            @Override
            public RentalReceipt register(Student student, Equipment equipment,
                                          RentalQuote quote, LocalDate startDate) {
                throw new IllegalStateException("Registry temporarily unavailable.");
            }
        };
    }

    private static void idempotentCompensation() {
        Fixture fixture = new Fixture();
        String reservationId = fixture.inventory.reserve("CAMERA");
        fixture.inventory.release(reservationId);
        fixture.inventory.release(reservationId);
        String paymentId = fixture.payments.charge("ST1001", new BigDecimal("100.00"));
        fixture.payments.refund(paymentId);
        fixture.payments.refund(paymentId);
        unchanged(fixture);
    }

    private static void directSubsystemUse() {
        StudentService students = new StudentService();
        InventoryService inventory = new InventoryService();
        PricingService pricing = new PricingService();
        PaymentService payments = new PaymentService();
        RentalRegistry rentals = new RentalRegistry();
        Student student = students.requireEligible("ST1001");
        Equipment equipment = inventory.find("CAMERA");
        RentalQuote quote = pricing.calculate(equipment, 2);
        inventory.reserve(equipment.id());
        payments.charge(student.id(), quote.total());
        RentalReceipt receipt = rentals.register(student, equipment, quote, LocalDate.of(2026, 10, 18));
        equal(LocalDate.of(2026, 10, 20), receipt.dueDate());
        equal(0, inventory.available("CAMERA"));
        money("39000.00", payments.balanceOf("ST1001"));
    }

    private static void immutableSnapshot() {
        Fixture fixture = new Fixture();
        Map<String, RentalReceipt> snapshot = fixture.rentals.allRentals();
        RentalReceipt receipt = fixture.facade.rentEquipment("ST1001", "CAMERA", 1);
        equal(0, snapshot.size());
        expect(UnsupportedOperationException.class,
                () -> fixture.rentals.allRentals().put("FAKE", receipt));
        equal(1, fixture.rentals.allRentals().size());
    }

    private static void unchanged(Fixture fixture) {
        money("50000.00", fixture.payments.balanceOf("ST1001"));
        equal(1, fixture.inventory.available("CAMERA"));
        equal(2, fixture.inventory.available("LAPTOP"));
        equal(1, fixture.inventory.available("PROJECTOR"));
        equal(0, fixture.rentals.allRentals().size());
    }

    private static void run(String name, Runnable test) {
        test.run();
        passed++;
        System.out.println("PASS: " + name);
    }

    private static void equal(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }

    private static void money(String expected, BigDecimal actual) {
        equal(new BigDecimal(expected), actual);
    }

    private static <T extends Throwable> T expect(Class<T> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) {
                return type.cast(failure);
            }
            throw new AssertionError("Expected " + type.getSimpleName(), failure);
        }
        throw new AssertionError("Expected " + type.getSimpleName() + " to be thrown.");
    }

    private static class Fixture {
        final StudentService students = new StudentService();
        final PricingService pricing = new PricingService();
        final PaymentService payments = new PaymentService();
        final InventoryService inventory;
        final RentalRegistry rentals;
        final EquipmentRentalFacade facade;

        Fixture() {
            this(new InventoryService(), new RentalRegistry());
        }

        Fixture(InventoryService inventory, RentalRegistry rentals) {
            this.inventory = inventory;
            this.rentals = rentals;
            facade = new EquipmentRentalFacade(students, inventory, pricing, payments, rentals, FIXED_CLOCK);
        }
    }
}
