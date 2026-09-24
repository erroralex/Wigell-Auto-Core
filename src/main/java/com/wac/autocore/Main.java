package com.wac.autocore;

import com.wac.autocore.data.ConnectionManager;
import com.wac.autocore.view.AutoCoreApp;

public class Main {
    public static void main(String[] args) {
        ConnectionManager.initializeSchema(); // Initialize the database schema before launching the application
        AutoCoreApp.main(args);
    }
}