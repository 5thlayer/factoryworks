package com.factoryworks.core.wreck;

/** Factorio's three crash-site debris classes, each with its own mining time (#550). */
public enum DebrisSize {
    BIG("big"),
    MEDIUM("medium"),
    SMALL("small");

    private final String id;

    DebrisSize(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
