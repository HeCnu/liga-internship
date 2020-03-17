package ru.liga;

public class Vocal {

    private final String name;
    private final Long startTick;
    private final Long endTick;

    public Vocal(String name, Long startTick, Long endTick) {
        this.name = name;
        this.startTick = startTick;
        this.endTick = endTick;
    }


    public String getName() {
        return name;
    }

    public Long getStartTick() {
        return startTick;
    }

    public Long getEndTick() {
        return endTick;
    }
}
