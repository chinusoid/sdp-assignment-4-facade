package com.company;

public record Student(String id, String name, boolean active) {
    public Student {
        if (id == null || id.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Student ID and name are required.");
        }
    }
}
