package com.mixpanel.java;

/**
 * 24.07.12 by IntelliJ IDEA 10 CE.
 * User: Zhdanov K, scalascope@gmail.com
 */
public class MPException extends Exception {

    @SuppressWarnings("FieldCanBeLocal")
    private final String request;

    public MPException(String error, String request) {
        super(error);
        this.request = request;
    }

    public MPException(String error) {
        super(error);
        request = null;
    }

    public MPException(Throwable throwable) {
        super(throwable);
        this.request = null;
    }

    public String getRequest() {
        return request;
    }
}
