package com.mycompany.bookbridge.model;

public final class FacultyMember extends Member {

    public FacultyMember(String id, String name) {
        super(id, name);
    }

    @Override
    public MembershipType getMembershipType() {
        return MembershipType.FACULTY;
    }

    @Override
    public int getMaximumLoanDays() {
        return 30;
    }

    @Override
    public int getDailyFine() {
        return 2;
    }
}