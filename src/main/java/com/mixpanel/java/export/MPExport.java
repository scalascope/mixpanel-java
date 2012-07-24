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

    private JSONObject doRequestJSONObject(String path, Map<String, Object> params) throws MPException {
        try {
            return new JSONObject(doRequest(path, params));
        } catch (JSONException e) {
            throw new MPException(e);
        }
    }

    private JSONArray doRequestJSONArray(String path, Map<String, Object> params) throws MPException {
        try {
            return new JSONArray(doRequest(path, params));
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
            URI uri = URIUtils.createURI("http", MPConfig.EXPORT_BASE_ENDPOINT, -1, "/" + path,
                    URLEncodedUtils.format(queryParams, "UTF-8"), null
            );

            log.debug("Mixpanel request uri: " + uri.toString());
            HttpGet httpGet = new HttpGet(uri);

            HttpResponse response = httpclient.execute(httpGet);
            HttpEntity entity = response.getEntity();

            String result = StringUtils.inputStreamToString(entity.getContent());

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
