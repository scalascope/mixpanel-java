package com.mixpanel.java.export;

/**
 * 24.07.12 by IntelliJ IDEA 10 CE.
 * User: Zhdanov K, scalascope@gmail.com
 */
public enum MPExportUnit {

    Minute("minute"), Hour("hour"), Day("day"), Week("week"), Month("month");

    @SuppressWarnings("FieldCanBeLocal")
    private final String unit;

    private MPExportUnit(String unit) {
        this.unit = unit;
    }


    @Override
    public String toString() {
        return unit;
    }
}
