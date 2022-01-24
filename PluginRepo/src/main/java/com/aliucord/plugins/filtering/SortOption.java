package com.aliucord.plugins.filtering;

public class SortOption {
    public SortOption (String optionName,String optionValue) {
        this.optionName = optionName;
        this.optionValue = optionValue;

    }
    public String optionName;
    public String optionValue;

    @Override
    public String toString() {
        return optionName;
    }
}
