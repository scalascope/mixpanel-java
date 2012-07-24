package com.mixpanel.java.test;

import com.mixpanel.java.MPException;
import com.mixpanel.java.export.MPExport;
import com.mixpanel.java.export.MPExportType;
import com.mixpanel.java.export.MPExportUnit;
import junit.framework.Assert;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 24.07.12 by IntelliJ IDEA 10 CE.
 * User: Zhdanov K, scalascope@gmail.com
 */
public class MPExportTest {

    private final static Logger log = LoggerFactory.getLogger(MPExportTest.class);

    private static final String testApiKey = "";
    private static final String testApiSecret = "";

    @Test
    public void events() throws MPException {
        MPExport mpExport = new MPExport(testApiKey, testApiSecret);
        JSONObject response = mpExport.events(Arrays.asList("testEvent"), MPExportType.General, MPExportUnit.Minute, 1);

        log.debug(response.toString());
        Assert.assertNotNull(response);
    }

    @Test
    public void topEvents() throws MPException {
        MPExport mpExport = new MPExport(testApiKey, testApiSecret);
        JSONObject response = mpExport.topEvents(MPExportType.General, 10);

        log.debug(response.toString());
        Assert.assertNotNull(response);
    }

    @Test
    public void names() throws MPException {
        MPExport mpExport = new MPExport(testApiKey, testApiSecret);
        JSONArray response = mpExport.names(MPExportType.General, 10);

        log.debug(response.toString());
        Assert.assertNotNull(response);
    }

    @Test
    public void export() throws MPException {
        MPExport mpExport = new MPExport(testApiKey, testApiSecret);

        Calendar from_date = Calendar.getInstance();
        from_date.add(Calendar.DAY_OF_YEAR, -30);

        List<JSONObject> response = mpExport.export(from_date.getTime(), new Date());

        for (JSONObject jsonObject : response) {
            log.debug(jsonObject.toString());
        }
        Assert.assertNotNull(response);
    }

}
