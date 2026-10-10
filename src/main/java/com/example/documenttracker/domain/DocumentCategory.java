package com.example.documenttracker.domain;

public enum DocumentCategory {
    IDENTIFICATION("Identification"),
    VEHICLE("Vehicle"),
    INSURANCE("Insurance"),
    PROFESSIONAL("Professional"),
    EDUCATION("Education"),
    PERMIT("Permit"),
    MEMBERSHIP("Membership"),
    REGISTRATION("Registration"),
    PROPERTY("Property & rental"),
    OTHER("Other");

    private final String label;

    DocumentCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}