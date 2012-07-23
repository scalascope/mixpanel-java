package com.mixpanel.java.mpmetrics;

import com.mixpanel.java.util.Base64Coder;
import com.mixpanel.java.util.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MPMetrics {

    private final static Logger log = LoggerFactory.getLogger(MPMetrics.class);

    // Maps each token to a singleton MPMetrics instance
    private static Map<String, MPMetrics> mInstanceMap = new ConcurrentHashMap<String, MPMetrics>();

    @SuppressWarnings("FieldCanBeLocal")
    private final MPConfig mpConfig;
    private final ThreadPoolExecutor executor;

    private final String mToken;

    private String distinct_id;

    private JSONObject mSuperProperties;

    @SuppressWarnings("FieldCanBeLocal")
    private static int FLUSH_EVENTS = 0;
    private static int FLUSH_PEOPLE = 1;

    private MPMetrics(final String token) {
        mToken = token;
        mSuperProperties = new JSONObject();

        mpConfig = new MPConfig();
        executor = new ThreadPoolExecutor(
                mpConfig.getThreadNumber(),
                mpConfig.getThreadNumber(),
                mpConfig.getSubmitThreadTTL(),
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new LowPriorityThreadFactory()
        );
    }

    public static MPMetrics getInstance(String token) {
        MPMetrics instance = mInstanceMap.get(token);
        if (instance == null) {
            instance = new MPMetrics(token);
            mInstanceMap.put(token, instance);
        }

        return instance;
    }

    public void registerSuperProperties(Map<String, Object> superPropertiesMap) {
        this.registerSuperProperties(new JSONObject(superPropertiesMap));
    }

    private void registerSuperProperties(JSONObject superProperties) {
        for (@SuppressWarnings("unchecked") Iterator<String> iter = superProperties.keys(); iter.hasNext(); ) {
            String key = iter.next();
            try {
                mSuperProperties.put(key, superProperties.get(key));
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public void clearSuperProperties() {
        mSuperProperties = new JSONObject();
    }

    public void identify(String distinct_id) {
        this.distinct_id = distinct_id;
    }

    public void track(String eventName) {
        executor.submit(new EventsQueueTask(eventName, null));
    }

    public void track(String eventName, Map<String, Object> propertiesMap) {
        executor.submit(new EventsQueueTask(eventName, new JSONObject(propertiesMap)));
    }

    private void track(String eventName, JSONObject properties) {
        executor.submit(new EventsQueueTask(eventName, properties));
    }

    public void set(Map<String, Object> propertiesMap) {
        this.set(new JSONObject(propertiesMap));
    }

    private void set(JSONObject properties) {
        if (this.distinct_id == null) {
            return;
        }

        executor.submit(new PeopleQueueTask("$set", properties));
    }

    public void set(String property, Object value) {
        try {
            set(new JSONObject().put(property, value));
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void increment(Map<String, Long> properties) {
        JSONObject json = new JSONObject(properties);
        if (this.distinct_id == null) {
            return;
        }

        executor.submit(new PeopleQueueTask("$add", json));
    }

    public void increment(String property, long value) {
        Map<String, Long> map = new HashMap<String, Long>();
        map.put(property, value);
        increment(map);
    }

    public void delete() {
        if (this.distinct_id == null) {
            return;
        }

        executor.submit(new PeopleQueueTask("$delete", null));
    }

    private void flushEvents(JSONObject data) {
        executor.submit(new SubmitTask(FLUSH_EVENTS, data));
    }

    private void flushPeople(JSONObject data) {
        executor.submit(new SubmitTask(FLUSH_PEOPLE, data));
    }

    private class SubmitTask implements Runnable {

        private final JSONObject data;
        private final int messageType;

        public SubmitTask(int messageType, JSONObject data) {
            this.messageType = messageType;
            this.data = data;
        }

        @Override
        public void run() {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost;
            if (messageType == FLUSH_PEOPLE) {
                httppost = new HttpPost(MPConfig.BASE_ENDPOINT + "/engage");
            } else {
                httppost = new HttpPost(MPConfig.BASE_ENDPOINT + "/track?ip=1");
            }

            try {
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);

                String dataEncoded = Base64Coder.encodeString(data.toString());
                nameValuePairs.add(new BasicNameValuePair("data", dataEncoded));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                HttpResponse response = httpclient.execute(httppost);
                HttpEntity entity = response.getEntity();

                String result = StringUtils.inputStreamToString(entity.getContent());
                if (result != null && result.contains("1")) {
                    log.debug("Data was logged");
                } else {
                    log.warn("Data was not logged");
                }

            } catch (ClientProtocolException e) {
                log.error(e.getMessage(), e);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private class EventsQueueTask implements Runnable {
        private String eventName;
        private JSONObject properties;

        public EventsQueueTask(String eventName, JSONObject properties) {
            this.eventName = eventName;
            this.properties = properties;
        }

        public void run() {
            JSONObject dataObj = new JSONObject();
            try {
                dataObj.put("event", eventName);
                JSONObject propertiesObj = new JSONObject();
                propertiesObj.put("token", mToken);
                propertiesObj.put("time", System.currentTimeMillis() / 1000);
                propertiesObj.put("mp_lib", "java");

                for (@SuppressWarnings("unchecked") Iterator<String> iter = mSuperProperties.keys(); iter.hasNext(); ) {
                    String key = iter.next();
                    propertiesObj.put(key, mSuperProperties.get(key));
                }

                if (distinct_id != null) {
                    propertiesObj.put("distinct_id", distinct_id);
                }

                if (properties != null) {
                    for (@SuppressWarnings("unchecked") Iterator<String> iter = properties.keys(); iter.hasNext(); ) {
                        String key = iter.next();
                        propertiesObj.put(key, properties.get(key));
                    }
                }

                dataObj.put("properties", propertiesObj);
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
                return;
            }

            flushEvents(dataObj);
        }
    }

    private class PeopleQueueTask implements Runnable {
        private String actionType;
        private JSONObject properties;

        public PeopleQueueTask(String actionType, JSONObject properties) {
            this.actionType = actionType;
            this.properties = properties;
        }

        public void run() {
            JSONObject dataObj = new JSONObject();
            try {
                dataObj.put(this.actionType, properties);
                dataObj.put("$token", mToken);
                dataObj.put("$distinct_id", distinct_id);
                dataObj.put("$time", System.currentTimeMillis());
            } catch (JSONException e) {
                log.error(e.getMessage(), e);
                return;
            }

            flushPeople(dataObj);
        }
    }

    public void shutdown() {
        try {
            executor.awaitTermination(7, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }


}
