package com.scc.model;

public record ActivityLog(
        int logId,
        int userId,
        String action,
        String tableAffected,
        String description,
        String timestamp
) {}