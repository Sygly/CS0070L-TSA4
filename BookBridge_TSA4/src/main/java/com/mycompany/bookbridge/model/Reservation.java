package com.mycompany.bookbridge.model;

public class Reservation {
    private final Member member;
    private final LibraryBook book;
    private final int requestedDays;

    public Reservation(Member member, LibraryBook book,
            int requestedDays) {
        if (member == null || book == null) {
            throw new IllegalArgumentException(
                    "Member and book are required.");
        }
        if (requestedDays < 1
                || requestedDays > member.getMaximumLoanDays()) {
            throw new IllegalArgumentException(
                    "Invalid reservation loan days.");
        }
        this.member = member;
        this.book = book;
        this.requestedDays = requestedDays;
    }

    public Member getMember() {
        return member;
    }

    public LibraryBook getBook() {
        return book;
    }

    public int getRequestedDays() {
        return requestedDays;
    }
}