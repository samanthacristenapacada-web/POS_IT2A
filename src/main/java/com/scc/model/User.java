package com.scc.model;

public record User(int UserID, String FullName, String Username, String Password,
                   String Role, String ContactNumber, String Status) {}
