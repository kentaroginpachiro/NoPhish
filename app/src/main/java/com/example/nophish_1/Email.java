package com.example.nophish_1;

public class Email {
    private String email;

    public Email() {
        // Default constructor required for calls to DataSnapshot.getValue(Email.class)
    }

    public Email(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
