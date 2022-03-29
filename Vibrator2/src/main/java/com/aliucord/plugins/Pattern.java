package com.aliucord.plugins;

public class Pattern {
    int ID;
    String patternName;
    long[] patternData;
    boolean repeat;

    public Pattern() {}
    public Pattern(int ID,String patternName, long[] patternData, boolean repeat) {
        this.ID = ID;
        this.patternName = patternName;
        this.patternData = patternData;
        this.repeat = repeat;
    }
}
