package com.mixpanel.java.mpmetrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Stores global configuration options for the Mixpanel library.
 */
public class MPConfig {

    private final static Logger log = LoggerFactory.getLogger(MPConfig.class);

    // Base url that track requests will be sent to. Events will be sent to /track
    // and people requests will be sent to /engage
    public static final String BASE_ENDPOINT = "http://api.mixpanel.com";

    public static final String EXPORT_BASE_ENDPOINT = "mixpanel.com/api/2.0";

    // Time in milliseconds that the submission thread must be idle for before it dies.
    // Must be reconfigured before the library is initialized for the first time.
    @SuppressWarnings("FieldCanBeLocal")
    private static final int DEFAULT_SUBMIT_THREAD_TTL = 180 * 1000;

    // Number of active threads that will be used for sending requests
    @SuppressWarnings("FieldCanBeLocal")
    private static final Integer DEFAULT_THREAD_NUMBER = 1;

    private final static String FILE = "/mixpanel.properties";

    private Integer submitThreadTTL;
    private Integer threadNumber;

    public MPConfig() {
        InputStream inStream = this.getClass().getResourceAsStream(FILE);
        Properties properties = new Properties();
        try {
            properties.load(inStream);

            try {
                submitThreadTTL = Integer.valueOf(properties.getProperty("mixpanel.submitThreadTTL"));
                threadNumber = Integer.valueOf(properties.getProperty("mixpanel.threadNumber"));
            } catch (Throwable ignore) {
            }
        } catch (IOException e) {
            log.debug("Can't find {}.", FILE);
        }

        if (submitThreadTTL == null) {
            submitThreadTTL = DEFAULT_SUBMIT_THREAD_TTL;
        }
        if (threadNumber == null) {
            threadNumber = DEFAULT_THREAD_NUMBER;
        }
    }

    public Integer getSubmitThreadTTL() {
        return submitThreadTTL;
    }

    public Integer getThreadNumber() {
        return threadNumber;
    }
}