package com.mycompany.bookbridge.model;

public final class GuestMember extends Member {

    public GuestMember(String id, String name) {
        super(id, name);
    }

    @Override
    public MembershipType getMembershipType() {
        return MembershipType.GUEST;
    }

    @Override
    public int getMaximumLoanDays() {
        return 7;
    }

    @Override
    public int getDailyFine() {
        return 10;
    }
}