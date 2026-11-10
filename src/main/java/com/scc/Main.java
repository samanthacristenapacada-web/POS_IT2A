package com.scc;

import com.scc.dao.DatabaseInitializer;
import com.scc.ui.Authentication;

public class Main {

    public static void main(String[] args) {

        // Initialize database tables
        DatabaseInitializer.initialize();

        // Start authentication system
        Authentication.start();
    }
}
