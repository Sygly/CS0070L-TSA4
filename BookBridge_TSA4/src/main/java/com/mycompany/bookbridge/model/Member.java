package com.mycompany.bookbridge.model;

public abstract class Member {
    private final String memberId;
    private final String memberName;

    protected Member(String memberId, String memberName) {
        if (memberId == null || memberId.isBlank()
                || memberName == null || memberName.isBlank()) {
            throw new IllegalArgumentException(
                    "Member ID and name are required.");
        }
        this.memberId = memberId.trim();
        this.memberName = memberName.trim();
    }

    public String getMemberId() {
        return memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public abstract MembershipType getMembershipType();

    public abstract int getMaximumLoanDays();

    public abstract int getDailyFine();

    public long calculateFine(int lateDays) {
        if (lateDays < 0) {
            throw new IllegalArgumentException(
                    "Late days cannot be negative.");
        }
        return (long) lateDays * getDailyFine();
    }
}