package com.scc.model;

public record User(
        int userId,
        String fullName,
        String username,
        String password,
        String role,
        String contactNumber,
        String status
) {
    public boolean isAdmin() {
        return role.equalsIgnoreCase("SYSTEM_ADMINISTRATOR");
    }

    public boolean isActive() {
        return status.equalsIgnoreCase("ACTIVE");
    }

    public boolean isPending() {
        return status.equalsIgnoreCase("PENDING");
    }
}