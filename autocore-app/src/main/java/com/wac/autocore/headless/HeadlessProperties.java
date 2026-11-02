package com.wac.autocore.headless;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Holds the account the headless runner logs in with. The values come from autocore.headless,
// or the optional application-local.properties.
@ConfigurationProperties("autocore.headless")
public class HeadlessProperties {

    private String username;
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean hasCredentials() {
        return username != null && !username.isBlank() && password != null && !password.isEmpty();
    }

    // The password is left out on purpose so it can never end up in a log.
    @Override
    public String toString() {
        return "HeadlessProperties{username=" + username + "}";
    }
}
