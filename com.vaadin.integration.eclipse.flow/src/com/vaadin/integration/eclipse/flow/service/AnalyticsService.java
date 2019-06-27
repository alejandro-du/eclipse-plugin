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
    private static final String CREATE_EVENT_TYPE = "Create project";

    private final String url = "https://api.amplitude.com/httpapi";

    private final String devKey = "1f0ca7c3a9b8e1b171631eaa30eef10a";
    private final String prodKey = "87bd4e7f802835b87ef07ad7bd763a87";

    private final String platform = "Desktop, Eclipse";

    private final String apiKeyParam = "api_key";
    private final String eventParam = "event";
    private final String userIdParam = "user_id";
    private final String deviceIdParam = "device_id";
    private final String eventTypeParam = "event_type";

    private final String eventPropsParam = "event_properties";
    private final String starterPropParam = "Starter";
    private final String stackPropParam = "Tech stack";

    private final String appVersionParam = "app_version";
    private final String platformParam = "platform";
    private final String osNameParam = "os_name";
    private final String osVersionParam = "os_version";

    public static boolean track(String eventType) {
        return INSTANCE.internalTrack(eventType, null, null);
    }

    public static boolean trackProjectCreate(String starter, String techStack) {
        return INSTANCE.internalTrack(CREATE_EVENT_TYPE, starter, techStack);
    }

    public boolean internalTrack(String eventType, String starter,
            String techStack) {
        try {
            HttpResponse response = Request.Post(url)
                    .bodyForm(createBody(eventType, starter, techStack),
                            StandardCharsets.UTF_8)
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

    private List<NameValuePair> createBody(String eventType, String starter,
            String techStack)
            throws UnsupportedEncodingException, NoSuchAlgorithmException {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(apiKeyParam,
                FlowPlugin.prodMode() ? prodKey : devKey));
        params.add(new BasicNameValuePair(eventParam,
                createEventData(eventType, starter, techStack)));
        return params;
    }

    private String createEventData(String eventType, String starter,
            String techStack)
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

        if (starter != null && techStack != null) {
            JsonObject eventProps = new JsonObject();
            eventProps.addProperty(starterPropParam, starter);
            eventProps.addProperty(stackPropParam, techStack);
            event.add(eventPropsParam, eventProps);
        }

        event.addProperty(appVersionParam, FlowPlugin.getVersion());
        event.addProperty(platformParam, platform);
        event.addProperty(osNameParam, System.getProperty("os.name"));
        event.addProperty(osVersionParam,
                ", " + System.getProperty("os.version") + ", "
                        + System.getProperty("os.arch"));

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
