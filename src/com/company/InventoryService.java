package com.company;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Owns equipment data, stock counts and reversible reservations. */
public class InventoryService {
    private final Map<String, Equipment> equipment = Map.of(
            "CAMERA", new Equipment("CAMERA", "Digital camera", money("1500"), money("8000")),
            "LAPTOP", new Equipment("LAPTOP", "Study laptop", money("2500"), money("10000")),
            "PROJECTOR", new Equipment("PROJECTOR", "Portable projector", money("2000"), money("7000")));
    private final Map<String, Integer> stock = new HashMap<>(Map.of(
            "CAMERA", 1, "LAPTOP", 2, "PROJECTOR", 1));
    private final Map<String, String> reservations = new HashMap<>();

    public Equipment find(String equipmentId) {
        if (equipmentId == null || equipmentId.isBlank()) {
            throw new IllegalArgumentException("Equipment ID is required.");
        }
        Equipment item = equipment.get(equipmentId);
        if (item == null) {
            throw new IllegalArgumentException("Unknown equipment: " + equipmentId);
        }
        return item;
    }

    public int available(String equipmentId) {
        find(equipmentId);
        return stock.get(equipmentId);
    }

    public String reserve(String equipmentId) {
        if (available(equipmentId) == 0) {
            throw new IllegalStateException("Equipment is unavailable: " + equipmentId);
        }
        String reservationId = UUID.randomUUID().toString();
        reservations.put(reservationId, equipmentId);
        stock.compute(equipmentId, (id, count) -> count - 1);
        return reservationId;
    }

    /** Repeating a release cannot accidentally increase the stock twice. */
    public void release(String reservationId) {
        String equipmentId = reservations.remove(reservationId);
        if (equipmentId != null) {
            stock.compute(equipmentId, (id, count) -> count + 1);
        }
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2);
    }
}
