package com.example.turniring.user.entity;

public enum UserRole {
    TEAM,
    JURY,
    ADMIN,
    ORGANIZER,
    USER;

    public boolean isTeamLike() {
        return this == TEAM || this == USER;
    }

    public boolean isSelfAssignable() {
        return this == TEAM || this == USER || this == ADMIN;
    }
}
