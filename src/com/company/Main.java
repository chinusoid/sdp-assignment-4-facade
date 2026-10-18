package com.company;

/** The application client creates and calls only the facade. */
public class Main {
    public static void main(String[] args) {
        EquipmentRentalFacade facade = new EquipmentRentalFacade();

        System.out.println("CAMPUS EQUIPMENT RENTAL | Facade Pattern\n");
        demonstrate(facade, "Successful rental", "ST1001", "CAMERA", 3);
        demonstrate(facade, "Equipment already rented", "ST1001", "CAMERA", 1);
        demonstrate(facade, "Payment declined; reservation is released", "ST1002", "PROJECTOR", 2);
        demonstrate(facade, "The released projector can be rented", "ST1001", "PROJECTOR", 2);
        demonstrate(facade, "Weekly rental with a 10% fee discount", "ST1001", "LAPTOP", 7);
        demonstrate(facade, "Inactive student", "ST1003", "LAPTOP", 1);
        demonstrate(facade, "Invalid duration", "ST1001", "LAPTOP", 0);
    }

    private static void demonstrate(EquipmentRentalFacade facade, String scenario,
                                    String studentId, String equipmentId, int days) {
        System.out.println("--- " + scenario + " ---");
        try {
            RentalReceipt receipt = facade.rentEquipment(studentId, equipmentId, days);
            System.out.printf("%s | %s | %s | %d days | due %s%n",
                    receipt.rentalId(), receipt.studentName(), receipt.equipmentName(),
                    receipt.days(), receipt.dueDate());
            System.out.printf("Fee: %s KZT | Deposit: %s KZT | Paid: %s KZT%n",
                    receipt.rentalFee(), receipt.deposit(), receipt.totalPaid());
        } catch (RentalException exception) {
            System.out.println(exception.getMessage());
        }
        System.out.println();
    }
}
