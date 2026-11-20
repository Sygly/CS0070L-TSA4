package com.mycompany.bookbridge.model;

import java.util.Map;

public record CirculationSummary(
        long availableBooks,
        long checkedOutBooks,
        Map<MembershipType, Long> activeLoansByType,
        long totalEstimatedFines) {

    public CirculationSummary {
        activeLoansByType = Map.copyOf(activeLoansByType);
    }
}