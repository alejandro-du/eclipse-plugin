package com.vaadin.integration.eclipse.flow.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.fluent.Request;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.vaadin.integration.eclipse.flow.FlowPlugin;
import com.vaadin.integration.eclipse.flow.pref.JavaPreferenceHandler;
import com.vaadin.integration.eclipse.flow.pref.JavaPreferenceKey;
import com.vaadin.integration.eclipse.flow.util.LogUtil;
import com.vaadin.pro.licensechecker.LocalProKey;
import com.vaadin.pro.licensechecker.ProKey;

public class AnalyticsService {

    public static final AnalyticsService INSTANCE = new AnalyticsService();

    public static final String INSTALL_EVENT_TYPE = "Install";
    public static final String CREATE_EVENT_TYPE = "Create project";

    private final String url = "https://api.amplitude.com/httpapi";

    private final String devKey = "1f0ca7c3a9b8e1b171631eaa30eef10a";
    private final String prodKey = "87bd4e7f802835b87ef07ad7bd763a87";

    private final String apiKeyParam = "api_key";
    private final String eventParam = "event";
    private final String userIdParam = "user_id";
    private final String deviceIdParam = "device_id";
    private final String eventTypeParam = "event_type";

    public static boolean track(String eventType) {
        return INSTANCE.internalTrack(eventType);
    }

    public boolean internalTrack(String eventType) {
        try {
            HttpResponse response = Request.Post(url)
                    .bodyForm(createBody(eventType), StandardCharsets.UTF_8)
                    .execute().returnResponse();
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException(
                        EntityUtils.toString(response.getEntity()));
            }
            return true;
        } catch (Exception e) {
            LogUtil.handleBackgroundException(
                    "Error happened while sending analytics to Amplitude", e);
            return false;
        }
    }

    private List<NameValuePair> createBody(String eventType)
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(apiKeyParam,
                FlowPlugin.prodMode() ? prodKey : devKey));
        params.add(
                new BasicNameValuePair(eventParam, createEventData(eventType)));
        return params;
    }

    private String createEventData(String eventType)
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        JsonObject event = new JsonObject();

        String idName = userIdParam;
        String idVal = getUserId();
        if (idVal == null) {
            idName = deviceIdParam;
            idVal = getDeviceId();
        }
        event.addProperty(idName, encode(idVal));
        event.addProperty(eventTypeParam, eventType);

        JsonArray events = new JsonArray();
        events.add(event);
        return events.toString();
    }

    private String getDeviceId() {
        String id = JavaPreferenceHandler.getStringValue(JavaPreferenceKey.ID);
        if (id.isEmpty()) {
            id = UUID.randomUUID().toString();
            JavaPreferenceHandler.saveStringValue(JavaPreferenceKey.ID, id);
        }
        return id;
    }

    private String getUserId() {
        ProKey proKey = LocalProKey.get();
        return proKey != null ? proKey.getProKey() : null;
    }

    private String encode(String id) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        StringBuilder hexString = new StringBuilder();
        for (byte b : digest.digest(id.getBytes(StandardCharsets.UTF_8))) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1) {
                hexString.append("0");
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
