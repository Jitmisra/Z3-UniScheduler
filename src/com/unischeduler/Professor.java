package com.unischeduler;

public class Professor {
    public final String name;
    public final int[] preferredSlots;

    public Professor(String name, int... preferredSlots) {
        this.name = name;
        this.preferredSlots = preferredSlots;
    }
}
