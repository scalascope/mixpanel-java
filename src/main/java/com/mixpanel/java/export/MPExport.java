package com.mixpanel.java.export;


import com.mixpanel.java.MPException;
import com.mixpanel.java.mpmetrics.MPConfig;
import com.mixpanel.java.util.StringUtils;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

public class MPExport {

    @SuppressWarnings("UnusedDeclaration")
    private static final Logger log = LoggerFactory.getLogger(MPExport.class);

    private final String api_key;
    private final String api_secret;

    private Long expire;

    public MPExport(String api_key, String api_secret) {
        this.api_key = api_key;
        this.api_secret = api_secret;
    }

    // Exporting raw data you inserted into Mixpanel
    public List<JSONObject> export(Date fromDate, Date toDate) throws MPException {
        return this.export(fromDate, toDate, null, null, null);
    }

    public List<JSONObject> export(Date fromDate, Date toDate, String event) throws MPException {
        return this.export(fromDate, toDate, Arrays.asList(event), null, null);
    }

    public List<JSONObject> export(Date fromDate, Date toDate, List<String> events) throws MPException {
        return this.export(fromDate, toDate, events, null, null);
    }

    public List<JSONObject> export(Date fromDate, Date toDate, List<String> events, String where, String bucket) throws MPException {
        Map<String, Object> params = getParams();

        if (events != null) {
            JSONArray eventsJson = new JSONArray();

            for (String event : events) {
                eventsJson.put(event);
            }

            params.put("event", eventsJson.toString());
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        params.put("from_date", dateFormat.format(fromDate));
        params.put("to_date", dateFormat.format(toDate));
        if (where != null) {
            params.put("where", where);
        }
        if (bucket != null) {
            params.put("bucket", bucket);
        }

        return doRequestRawJSONObject("export", params);
    }

    public JSONObject events(String event, MPExportType type, MPExportUnit unit, int interval) throws MPException {
        return this.events(Arrays.asList(event), type, unit, interval);
    }

    // Get unique, total, or average data for a set of events over the last N days, weeks, or months.
    public JSONObject events(List<String> events, MPExportType type, MPExportUnit unit, int interval) throws MPException {
        Map<String, Object> params = getParams();

        JSONArray eventsJson = new JSONArray();

        for (String event : events) {
            eventsJson.put(event);
        }

        params.put("event", eventsJson.toString());
        params.put("type", type);
        params.put("unit", unit);
        params.put("interval", interval);

        return doRequestJSONObject("events", params);
    }

    public JSONObject topEvents(MPExportType type) throws MPException {
        Map<String, Object> params = getParams();
        params.put("type", type);

        return doRequestJSONObject("events/top", params);
    }

    public JSONObject topEvents(MPExportType type, int limit) throws MPException {
        Map<String, Object> params = getParams();
        params.put("type", type);
        params.put("limit", limit);

        return doRequestJSONObject("events/top", params);
    }

    public JSONArray names(MPExportType type) throws MPException {
        Map<String, Object> params = getParams();
        params.put("type", type);

        return doRequestJSONArray("events/names", params);
    }

    public JSONArray names(MPExportType type, int limit) throws MPException {
        Map<String, Object> params = getParams();
        params.put("type", type);
        params.put("limit", limit);

        return doRequestJSONArray("events/names", params);
    }

    public JSONObject properties(String event, String name, MPExportType type, MPExportUnit unit, int interval) throws MPException {
        return this.properties(event, name, null, type, unit, interval, -1);
    }

    public JSONObject properties(String event, String name, List<String> values, MPExportType type, MPExportUnit unit, int interval, int limit) throws MPException {
        Map<String, Object> params = getParams();
        params.put("event", event);
        params.put("name", name);

        if (values != null) {
            JSONArray valuesJson = new JSONArray();

            for (String value : values) {
                valuesJson.put(value);
            }
        }

        params.put("type", type);
        params.put("unit", unit);
        params.put("interval", interval);
        if (limit > -1) {
            params.put("limit", limit);
        }

        return doRequestJSONObject("events/properties", params);
    }

    public JSONObject topProperties(String event) throws MPException {
        return this.topProperties(event, -1);
    }

    public JSONObject topProperties(String event, int limit) throws MPException {
        Map<String, Object> params = getParams();
        params.put("event", event);
        if (limit > -1) {
            params.put("limit", limit);
        }
        return doRequestJSONObject("events/properties/top", params);
    }

    private List<JSONObject> doRequestRawJSONObject(String path, Map<String, Object> params) throws MPException {
        try {
            String resp = doRequest(path, params);
            if (resp == null || resp.trim().isEmpty()) {
                return Collections.emptyList();
            } else {
                List<JSONObject> jsonObjects = new ArrayList<JSONObject>();
                for (String jsonString : resp.split("\n")) {
                    jsonObjects.add(new JSONObject(jsonString));
                }
                return jsonObjects;
            }
        } catch (JSONException e) {
            throw new MPException(e);
        }
    }


    private JSONObject doRequestJSONObject(String path, Map<String, Object> params) throws MPException {
        try {
            String resp = doRequest(path, params);
            return resp == null || resp.trim().isEmpty() ? new JSONObject() : new JSONObject(resp);
        } catch (JSONException e) {
            throw new MPException(e);
        }
    }

    private JSONArray doRequestJSONArray(String path, Map<String, Object> params) throws MPException {
        try {
            String resp = doRequest(path, params);
            return resp == null || resp.trim().isEmpty() ? new JSONArray() : new JSONArray(resp);
        } catch (JSONException e) {
            throw new MPException(e);
        }
    }

    @SuppressWarnings("deprecation")
    private String doRequest(String path, Map<String, Object> params) throws MPException {

        HttpClient httpclient = new DefaultHttpClient();

        List<NameValuePair> queryParams = new ArrayList<NameValuePair>();
        for (Map.Entry<String, Object> param : params.entrySet()) {
            queryParams.add(new BasicNameValuePair(param.getKey(), param.getValue().toString()));
        }
        try {
            queryParams.add(new BasicNameValuePair("sig", sig(params)));
        } catch (Throwable e) {
            throw new MPException(e);
        }

        try {

            URI uri = URIUtils.createURI("http",
                    path.equals("export") ? MPConfig.EXPORT_RAW_BASE_ENDPOINT : MPConfig.EXPORT_BASE_ENDPOINT,
                    -1, "/" + path,
                    URLEncodedUtils.format(queryParams, "UTF-8"), null
            );

            log.debug("Mixpanel request uri: " + uri.toString());
            HttpGet httpGet = new HttpGet(uri);

            HttpResponse response = httpclient.execute(httpGet);
            HttpEntity entity = response.getEntity();

            String result = StringUtils.inputStreamToString(entity.getContent());

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new MPException(result);
            }

            if (result.contains("'error':")) {
                JSONObject responseJson = new JSONObject(result);
                throw new MPException(responseJson.getString("error"), responseJson.getString("request"));
            }

            return result;
        } catch (Throwable e) {
            throw new MPException(e);
        }
    }

    private String sig(Map<String, Object> parameters) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        StringBuilder sig = new StringBuilder();
        for (Map.Entry<String, Object> parameter : parameters.entrySet()) {
            sig.append(parameter.getKey()).append("=").append(parameter.getValue());
        }
        sig.append(api_secret);
        return md5(sig.toString());
    }

    private String md5(String text) throws NoSuchAlgorithmException {
        final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        messageDigest.reset();
        messageDigest.update(text.getBytes(Charset.forName("UTF8")));
        final byte[] resultByte = messageDigest.digest();
        return new String(Hex.encodeHex(resultByte));
    }

    private Map<String, Object> getParams() {
        Map<String, Object> params = new TreeMap<String, Object>();
        params.put("api_key", api_key);
        params.put("expire", getExpire());
        params.put("format", "json");
        return params;
    }

    public long getExpire() {
        return expire == null ? System.currentTimeMillis() / 1000 + 60 : expire;
    }

    public void setExpire(long expire) {
        this.expire = expire;
    }
}
