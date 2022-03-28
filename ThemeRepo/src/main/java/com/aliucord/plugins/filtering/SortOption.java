package com.aliucord.plugins.filtering;

public class SortOption {
    public String optionName;
    public String optionValue;
    public SortOption(String optionName, String optionValue) {
        this.optionName = optionName;
        this.optionValue = optionValue;

    }

    @Override
    public String toString() {
        return optionName;
    }
}
