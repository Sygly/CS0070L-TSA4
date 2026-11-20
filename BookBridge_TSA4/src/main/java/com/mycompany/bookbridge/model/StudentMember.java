package com.mycompany.bookbridge.model;

public final class StudentMember extends Member {

    public StudentMember(String id, String name) {
        super(id, name);
    }

    @Override
    public MembershipType getMembershipType() {
        return MembershipType.STUDENT;
    }

    @Override
    public int getMaximumLoanDays() {
        return 14;
    }

    @Override
    public int getDailyFine() {
        return 5;
    }
}