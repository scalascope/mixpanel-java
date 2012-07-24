package com.mixpanel.java.export;

/**
 * 24.07.12 by IntelliJ IDEA 10 CE.
 * User: Zhdanov K, scalascope@gmail.com
 */
public enum MPExportType {

    General("general"), Unique("unique"), Average("average");

    @SuppressWarnings("FieldCanBeLocal")
    private final String type;

    private MPExportType(String type) {
        this.type = type;
    }


    @Override
    public String toString() {
        return type;
    }
}
