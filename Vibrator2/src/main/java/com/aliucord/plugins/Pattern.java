package com.aliucord.plugins;

import java.util.Arrays;

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

    @Override
    public String toString() {
        return "Pattern{" +
                "ID=" + ID +
                ", patternName='" + patternName + '\'' +
                ", patternData=" + Arrays.toString(patternData) +
                ", repeat=" + repeat +
                '}';
    }
}
