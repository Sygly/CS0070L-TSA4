package com.mycompany.bookbridge.model;

import java.time.LocalDate;

public class LoanRecord {
    private final String loanId;
    private final Member member;
    private final LibraryBook book;
    private final LocalDate dueDate;
    private final int requestedDays;
    private int lateDays;
    private LoanStatus status;

    public LoanRecord(String loanId, Member member,
            LibraryBook book, int requestedDays,
            LocalDate startDate) {

        if (loanId == null || loanId.isBlank()
                || member == null || book == null
                || startDate == null) {
            throw new IllegalArgumentException(
                    "Invalid loan information.");
        }
        if (requestedDays < 1 || requestedDays > 30) {
            throw new IllegalArgumentException(
                    "Requested days must be 1-30.");
        }

        this.loanId = loanId.trim();
        this.member = member;
        this.book = book;
        this.requestedDays = requestedDays;
        this.dueDate = startDate.plusDays(requestedDays);
        this.status = LoanStatus.ACTIVE;
        this.lateDays = 0;
    }

    public String getLoanId() {
        return loanId;
    }

    public Member getMember() {
        return member;
    }

    public LibraryBook getBook() {
        return book;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public int getRequestedDays() {
        return requestedDays;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public int getLateDays() {
        return lateDays;
    }

    public boolean isOverdue(LocalDate today) {
        return status == LoanStatus.ACTIVE
                && dueDate.isBefore(today);
    }

    public long getEstimatedFine(LocalDate today) {
        int days = status == LoanStatus.ACTIVE
                ? (int) Math.max(0,
                    java.time.temporal.ChronoUnit.DAYS
                        .between(dueDate, today))
                : lateDays;
        return member.calculateFine(days);
    }

    public void markReturned(LocalDate returnDate) {
        if (returnDate == null || status != LoanStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Loan cannot be returned.");
        }
        lateDays = (int) Math.max(0,
                java.time.temporal.ChronoUnit.DAYS
                        .between(dueDate, returnDate));
        status = LoanStatus.RETURNED;
    }
}