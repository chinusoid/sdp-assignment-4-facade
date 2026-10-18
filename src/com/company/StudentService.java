package com.company;

import java.util.Map;

/** Checks membership and eligibility independently of any facade. */
public class StudentService {
    private final Map<String, Student> students = Map.of(
            "ST1001", new Student("ST1001", "Aida", true),
            "ST1002", new Student("ST1002", "Dias", true),
            "ST1003", new Student("ST1003", "Miras", false));

    public Student requireEligible(String studentId) {
        if (studentId == null || studentId.isBlank()) {
            throw new IllegalArgumentException("Student ID is required.");
        }
        Student student = students.get(studentId);
        if (student == null) {
            throw new IllegalArgumentException("Unknown student: " + studentId);
        }
        if (!student.active()) {
            throw new IllegalStateException("Student membership is inactive: " + studentId);
        }
        return student;
    }
}
