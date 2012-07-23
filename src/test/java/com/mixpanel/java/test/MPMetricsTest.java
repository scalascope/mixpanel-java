package com.mixpanel.java.test;

import com.mixpanel.java.mpmetrics.MPMetrics;
import junit.framework.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class MPMetricsTest {

    private static final String testToken = "";

    @Test
    public void testEvent() {

        MPMetrics mpMetrics = MPMetrics.getInstance(testToken);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("ip", "123.123.123.123");
        properties.put("action", "test");

        mpMetrics.track("testEvent", properties);
        mpMetrics.shutdown();

        Assert.assertTrue(true);
    }

    @Test
    public void testUser() {

        MPMetrics mpMetrics = MPMetrics.getInstance(testToken);
        mpMetrics.identify("123456789");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("name", "Konstantin");
        properties.put("$email", "scalascope@gmail.com");

        mpMetrics.set(properties);
        mpMetrics.shutdown();

        Assert.assertTrue(true);
    }

    @Test
    public void testEventWithUser() {

        MPMetrics mpMetrics = MPMetrics.getInstance(testToken);
        mpMetrics.identify("123456789");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("ip", "123.123.123.123");
        properties.put("action", "test");

        mpMetrics.track("testEventWithUser", properties);
        mpMetrics.shutdown();

        Assert.assertTrue(true);
    }

}
