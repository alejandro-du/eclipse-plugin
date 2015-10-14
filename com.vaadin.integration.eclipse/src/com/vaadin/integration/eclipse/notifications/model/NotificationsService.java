package com.vaadin.integration.eclipse.notifications.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.notifications.Consumer;
import com.vaadin.integration.eclipse.notifications.Utils;
import com.vaadin.integration.eclipse.util.ErrorUtil;

/**
 * Provides all backend services for notifications.
 * 
 * All server communication is inside this class only.
 *
 */
public final class NotificationsService {

    private static final String SETTINGS_URL_KEY = "settingsUrl";

    private static final String SKIP_NOTIFICATION_PARAM = "s";

    private static final String NOTIFICATION_IDS_PARAM = "p";

    private static final String TOKEN = "token";

    private static final String TOKEN_PARAM = "t";

    private static final int OK_CODE = 200;

    private static final int ABOVE_OK_CODE = 300;

    private static final String PWD_PARAM = "p";

    private static final String REFERRER_PARAM = "r"; // anonymous plugin user
                                                      // id

    private static final String EMAIL_PARAM = "e";

    private static final String IMAGES_CACHE = "notification-images-cache";

    private static final String CACHE_FILE = "notifications-cache.json";

    private static final String ICON_URL = "iconUrl";

    private static final String LINK_TEXT = "linkText";

    private static final String IMAGE_URL = "imageUrl";

    private static final String VALID_FROM = "validFrom";

    private static final String READ = "read";

    private static final String CATEGORY = "category";

    private static final String ID = "id";

    private static final String NOTIFICATIONS = "notifications";

    private static final String LINK_URL = "linkUrl";

    private static final String TITLE = "title";

    private static final String BODY = "body";

    private static final String ALL_NOTIFICATIONS_URL = "https://vaadin.com/delegate/notifications/personal";

    private static final String TOKEN_URL = "https://vaadin.com/delegate/notifications/token";

    private static final String MARK_READ_URL = "https://vaadin.com/delegate/notifications/markasread";

    private static final String READ_MORE_URL = "https://vaadin.com/delegate/notifications/readmore";

    private static final String USER_INFO_URL = "https://vaadin.com/delegate/notifications/userinfo";

    private static final NotificationsService INSTANCE = new NotificationsService();

    private static final String UTF8 = "UTF-8";

    private static final int ICON_SIZE = 40;

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssX");

    private final Object lock;

    private static final Logger LOG = Logger
            .getLogger(NotificationsService.class.getName());

    private NotificationsService() {
        lock = new Object();
    }

    public Collection<Notification> getAllNotifications(String token) {
        return getNotifications(ALL_NOTIFICATIONS_URL, token);
    }

    public Collection<Notification> getCachedNotifications(String token) {
        synchronized (lock) {
            if (getCacheFile().exists()) {
                return getCachedNotifications();
            } else {
                return getAllNotifications(token);
            }
        }
    }

    public void downloadImages(Collection<Notification> notifications) {
        HttpClient client = createHttpClient();
        downloadImages(client, notifications, false);
        HttpClientUtils.closeQuietly(client);
    }

    public void downloadIcons(Collection<Notification> notifications) {
        HttpClient client = createHttpClient();
        downloadImages(client, notifications, true);
        HttpClientUtils.closeQuietly(client);
    }

    public String signIn(String login, String passwd)
            throws InvalidCredentialsException {
        Map<String, String> params = new HashMap<String, String>();
        params.put(EMAIL_PARAM, login);
        params.put(PWD_PARAM, passwd);
        final String[] token = new String[1];
        post(TOKEN_URL, params, new Consumer<InputStream>() {
            public void accept(InputStream stream) {
                InputStreamReader reader = new InputStreamReader(stream);
                try {
                    JSONParser parser = new JSONParser();
                    Object object = parser.parse(reader);
                    if (object instanceof JSONObject) {
                        object = ((JSONObject) object).get(TOKEN);
                        token[0] = object == null ? null : object.toString();
                    }
                } catch (ParseException e) {
                    handleException(Level.WARNING, e);
                } catch (IOException e) {
                    handleException(Level.WARNING, e);
                }
            }
        }, "Acquiring user token", "Authentication failed");
        if (token[0] == null || token[0].isEmpty()) {
            throw new InvalidCredentialsException();
        }
        return token[0];
    }

    /**
     * Usage statistics method: user identified by {@code token} requested full
     * info for notification with given {@code notificationId} (navigated to the
     * read me link, f.e. via opening Web page in external browser).
     * 
     * @param token
     *            User token.
     * @param notificationId
     *            Notification id.
     */
    public void infoRequested(String token, String notificationId) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(NOTIFICATION_IDS_PARAM, notificationId);
        params.put(TOKEN_PARAM, token);
        if (!post(READ_MORE_URL, params, null, "Sending 'read more' request",
                "Unexpected response code")) {
            LOG.warning("Read more request has failed");
        }
    }

    /**
     * Mark notification identified by {@code notificationId} as read for
     * user/session identified by given {@code token}.
     * 
     * @param token
     *            User token.
     * @param notificationId
     *            Notification id.
     */
    public void markRead(String token, String notificationId) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(NOTIFICATION_IDS_PARAM, notificationId);
        params.put(TOKEN_PARAM, token);
        StringBuilder startLog = new StringBuilder("Marking notification ");
        startLog.append(notificationId).append("as read");
        if (!post(MARK_READ_URL, params, null, startLog.toString(),
                "Mark as read request has failed")) {
            StringBuilder error = new StringBuilder(
                    "Couldn't mark notification ");
            error.append(notificationId).append(" as read");
            LOG.warning(error.toString());
        }
    }

    /**
     * Mark notifications as read (and skipped) for user/session identified by
     * given {@code token}.
     * 
     * @param token
     *            User token.
     * @param ids
     *            notification identifiers
     */
    public void skipNotifications(String token, List<String> ids) {
        Map<String, String> params = new HashMap<String, String>();
        StringBuilder idsValue = new StringBuilder();
        for (String id : ids) {
            idsValue.append(id).append(',');
        }
        if (idsValue.length() > 0) {
            idsValue.delete(idsValue.length() - 1, idsValue.length());
        }
        params.put(NOTIFICATION_IDS_PARAM, idsValue.toString());
        params.put(TOKEN_PARAM, token);
        params.put(SKIP_NOTIFICATION_PARAM, Boolean.TRUE.toString());
        if (!post(MARK_READ_URL, params, null,
                "Marking a list of notifications as read/skipped",
                "Mark as read/skipped request has failed")) {
            StringBuilder error = new StringBuilder(
                    "Couldn't mark notifications ");
            error.append(idsValue).append(" as read");
            LOG.warning(error.toString());
        }
    }

    /**
     * Return notification settings url.
     */
    public String getSettingsUrl() {
        final String[] url = new String[1];
        doGet(USER_INFO_URL, new Consumer<InputStreamReader>() {
            public void accept(InputStreamReader reader) {
                url[0] = getSettingsUrl(reader);
            }

        }, "Requesting user info (notification settings URL)");
        return url[0];
    }

    private String getSettingsUrl(InputStreamReader reader) {
        JSONParser parser = new JSONParser();
        try {
            Object object = parser.parse(reader);
            if (object instanceof JSONObject) {
                Object url = ((JSONObject) object).get(SETTINGS_URL_KEY);
                if (url == null) {
                    return null;
                }
                String result = url.toString();
                if (result.isEmpty()) {
                    return null;
                } else {
                    return result;
                }
            } else {
                LOG.warning(
                        "User info URL has returned not JSONObject (unexpectedly)");
            }
        } catch (IOException e) {
            handleException(Level.WARNING, e);
        } catch (ParseException e) {
            handleException(Level.INFO, e);
        }
        return null;
    }

    private boolean post(String url, Map<String, String> params,
            Consumer<InputStream> streamConsumer, String logStartMsg,
            String badStatusCodeMsg) {
        HttpClient client = createHttpClient();
        HttpPost request = new HttpPost(url);
        InputStreamReader reader = null;
        try {
            LOG.info(logStartMsg);

            List<NameValuePair> pairs = new ArrayList<NameValuePair>();
            for (Entry<String, String> entry : params.entrySet()) {
                BasicNameValuePair pair = new BasicNameValuePair(entry.getKey(),
                        entry.getValue());
                pairs.add(pair);
            }
            pairs.add(
                    new BasicNameValuePair(REFERRER_PARAM, Utils.ANONYMOUS_ID));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(pairs);
            request.setEntity(entity);

            HttpResponse response = client.execute(request);

            boolean isOK = false;
            int statusCode = getStatusCode(response);
            if (statusCode == OK_CODE) {
                isOK = true;
            } else if (statusCode < ABOVE_OK_CODE) {
                isOK = true;
                LOG.info("Status code for request is " + statusCode);
            }
            if (isOK) {
                if (streamConsumer != null) {
                    streamConsumer.accept(response.getEntity().getContent());
                }
                return true;
            } else {
                LOG.warning(badStatusCodeMsg);
            }
            HttpClientUtils.closeQuietly(response);

            return false;
        } catch (ClientProtocolException e) {
            handleException(Level.WARNING, e);
        } catch (IOException e) {
            handleException(Level.WARNING, e);
        } catch (IllegalStateException e) {
            handleException(Level.WARNING, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    handleException(Level.INFO, e);
                }
            }
        }
        HttpClientUtils.closeQuietly(client);
        return false;
    }

    private Collection<Notification> getNotifications(String url,
            String token) {
        String uri = url;
        if (token != null) {
            StringBuilder builder = new StringBuilder(url);
            builder.append('?').append(URLEncodedUtils.format(Arrays.asList(
                    new BasicNameValuePair(TOKEN_PARAM, token),
                    new BasicNameValuePair(REFERRER_PARAM, Utils.ANONYMOUS_ID)),
                    UTF8));
            uri = builder.toString();
        }
        final List<Collection<Notification>> result = new ArrayList<Collection<Notification>>(
                1);
        Consumer<InputStreamReader> consumer = new Consumer<InputStreamReader>() {

            public void accept(InputStreamReader reader) {
                try {
                    result.add(getNotifications(reader, true));
                } catch (IOException e) {
                    handleException(Level.WARNING, e);
                } catch (ParseException e) {
                    handleException(Level.INFO, e);
                }
            }
        };
        doGet(uri, consumer, "Fetching all notifications");
        if (result.isEmpty()) {
            return getCachedNotifications();
        } else {
            return result.get(0);
        }
    }

    private void doGet(String url, Consumer<InputStreamReader> consumer,
            String startLogMsg) {
        HttpClient client = createHttpClient();
        HttpGet request = new HttpGet(url);
        InputStreamReader reader = null;
        try {
            LOG.info(startLogMsg);
            HttpResponse response = client.execute(request);

            getStatusCode(response);
            InputStream inputStream = response.getEntity().getContent();
            reader = new InputStreamReader(inputStream, UTF8);

            consumer.accept(reader);
            HttpClientUtils.closeQuietly(response);
        } catch (ClientProtocolException e) {
            handleException(Level.WARNING, e);
        } catch (IOException e) {
            handleException(Level.WARNING, e);
        } catch (IllegalStateException e) {
            handleException(Level.WARNING, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    handleException(Level.INFO, e);
                }
            }
        }
        HttpClientUtils.closeQuietly(client);
    }

    private int getStatusCode(HttpResponse response) {
        int code = response.getStatusLine().getStatusCode();
        LOG.info("HTTP Response code :"
                + response.getStatusLine().getStatusCode());
        return code;
    }

    private Collection<Notification> getCachedNotifications() {
        synchronized (lock) {
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(getCacheFile());
                return getNotifications(inputStream, false);
            } catch (IOException e) {
                handleException(Level.WARNING, e);
            } catch (ParseException e) {
                handleException(Level.WARNING, e);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        handleException(Level.INFO, e);
                    }
                }
            }
        }
        return Collections.emptyList();
    }

    private Collection<Notification> getNotifications(InputStream inputStream,
            boolean cache) throws IOException, ParseException {
        return getNotifications(new InputStreamReader(inputStream, UTF8),
                cache);
    }

    private Collection<Notification> getNotifications(InputStreamReader reader,
            boolean cache) throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        try {
            parser = new JSONParser();

            LOG.info("Parse notifications");
            JSONObject object = (JSONObject) parser.parse(reader);

            if (cache) {
                saveCache(object);
            }

            JSONArray array = (JSONArray) object.get(NOTIFICATIONS);
            List<Notification> list = new ArrayList<Notification>(array.size());
            for (int i = 0; i < array.size(); i++) {
                list.add(buildNotification((JSONObject) array.get(i)));
            }

            return list;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    handleException(Level.INFO, e);
                }
            }
        }
    }

    private void saveCache(JSONObject object) throws IOException {
        synchronized (lock) {
            FileOutputStream outputStream = new FileOutputStream(
                    getCacheFile());
            try {
                LOG.info("Save JSON notifications content to cache file "
                        + getCacheFile());
                IOUtils.write(object.toJSONString(), outputStream);
            } finally {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    handleException(Level.INFO, e);
                }
            }
        }
    }

    private File getCacheFile() {
        IPath location = VaadinPlugin.getInstance().getStateLocation();
        location = location.append(CACHE_FILE);
        return location.makeAbsolute().toFile();
    }

    private HttpClient createHttpClient() {
        HttpClientBuilder builder = HttpClientBuilder.create();
        return builder.build();
    }

    private void downloadImages(HttpClient client,
            Collection<Notification> notifications, boolean icons) {
        File folder = getCacheFolder();
        synchronized (lock) {
            for (Notification notification : notifications) {
                String url = icons ? notification.getIconUrl()
                        : notification.getImageUrl();
                ensureImageRegistered(client, folder, url, icons);
            }
        }
    }

    private void ensureImageRegistered(HttpClient client, File folder,
            String url, boolean icon) {
        // NB: this code is not thread safe ! But impact of this is low:
        // Only current thread registers the images with such keys so most
        // likely it won't be modified by any other thread. But even if it
        // happens it will cause overridden image and this is not an issue.
        // There will be just extra download operation.
        Image image = VaadinPlugin.getInstance().getImageRegistry().get(url);
        if (image == null) {
            registerImage(client, folder, url, icon);
        }
    }

    private void registerImage(HttpClient client, File folder, String url,
            boolean icon) {
        try {
            ImageData data = getImage(client, url, folder, icon);
            if (data != null) {
                VaadinPlugin.getInstance().getImageRegistry().put(url,
                        ImageDescriptor.createFromImageData(data));
            }
        } catch (SWTException e) {
            handleException(Level.WARNING, e);
        }
    }

    /**
     * Image infrastructure has bad design: it doesn't declare thrown exception
     * in method signatures but it throws unchecked exceptions. So SWTException
     * is explicitly declared to be able to catch it.
     */
    private ImageData getImage(HttpClient client, String url, File cacheFolder,
            boolean icon) throws SWTException {
        String id = getUniqueId(url);
        if (cacheFolder == null || id == null) {
            return downloadImage(client, url, icon);
        }
        File file = new File(cacheFolder, id);
        try {
            if (file.exists()) {
                try {
                    // this can throw unchecked exception. Consider this as
                    // a broken cache file and reset it via downloading.
                    ImageData data = new ImageData(new FileInputStream(file));

                    // check sizes. If required width/height has been updated in
                    // development version then cache has to be recreated.
                    if (icon) {
                        if (ICON_SIZE == Math.max(data.width, data.height)) {
                            return data;
                        }
                    } else if (data.width == Utils.MAX_WIDTH) {
                        return data;
                    }
                } catch (SWTException e) {
                    handleException(Level.WARNING, e);
                }
            }
            ImageData data = downloadImage(client, url, icon);
            cacheImage(file, data);
            return data;
        } catch (FileNotFoundException e) {
            handleException(Level.WARNING, e);
        }
        return null;
    }

    private void cacheImage(File file, ImageData data)
            throws FileNotFoundException {
        FileOutputStream stream = new FileOutputStream(file);
        ImageLoader loader = new ImageLoader();
        loader.data = new ImageData[] { data };
        loader.save(stream, data.type);
        try {
            stream.close();
        } catch (IOException e) {
            handleException(Level.INFO, e);
        }
    }

    private String getUniqueId(String urlString) {
        try {
            URL url = new URL(urlString);
            String file = url.getFile();
            if (file.charAt(0) == '/') {
                file = file.substring(1);
            }
            return URLEncoder.encode(file, UTF8);
        } catch (MalformedURLException e) {
            handleException(Level.WARNING, e);
        } catch (UnsupportedEncodingException e) {
            handleException(Level.WARNING, e);
        }

        return null;

    }

    private ImageData downloadImage(HttpClient client, String url,
            boolean icon) {
        HttpGet request = new HttpGet(url);
        HttpResponse response = null;
        InputStream stream = null;
        try {
            response = client.execute(request);

            stream = response.getEntity().getContent();
            ImageData data = new ImageData(stream);
            Point size;
            if (icon) {
                size = scaleIconSize(new Point(data.width, data.height));
            } else {
                size = scaleImageSize(new Point(data.width, data.height));
            }

            if (size == null) {
                log(Level.INFO, "{0} {1} has zero size and is not downloaded",
                        icon ? "Icon" : "Image", url);
                return null;
            } else {
                log(Level.INFO, "{0} {1} is downloaded",
                        icon ? "Icon" : "Image", url);
                return data.scaledTo(size.x, size.y);
            }
        } catch (ClientProtocolException e) {
            handleException(Level.WARNING, e);
        } catch (IOException e) {
            handleException(Level.WARNING, e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    handleException(Level.INFO, e);
                }
            }
            if (response != null) {
                HttpClientUtils.closeQuietly(response);
            }
        }
        return null;
    }

    private void log(Level level, String pattern, Object... params) {
        LOG.log(level, MessageFormat.format(pattern, params));
    }

    private Point scaleIconSize(Point original) {
        int max = Math.max(original.x, original.y);
        if (max == 0) {
            return null;
        }
        return new Point((ICON_SIZE * original.x) / max,
                (ICON_SIZE * original.y) / max);
    }

    private Point scaleImageSize(Point original) {
        return new Point(Utils.MAX_WIDTH,
                (Utils.MAX_WIDTH * original.y) / original.x);
    }

    private File getCacheFolder() {
        IPath location = VaadinPlugin.getInstance().getStateLocation();
        location = location.append(IMAGES_CACHE);
        File file = location.makeAbsolute().toFile();
        if (!file.exists()) {
            file.mkdirs();
        }
        if (file.exists()) {
            return file;
        } else {
            log(Level.WARNING,
                    "Cache directory {} has not been created. Proceed without cache.",
                    file.getPath());
            return null;
        }
    }

    private Notification buildNotification(JSONObject info) {
        Notification.Builder builder = new Notification.Builder();
        builder.setId(getString(info, ID));
        builder.setCategory(getString(info, CATEGORY));
        builder.setTitle(getString(info, TITLE));
        builder.setDescription(getString(info, BODY));
        builder.setLink(getString(info, LINK_URL));
        builder.setRead(getBoolean(info, READ));

        builder.setDate(getDate(info, VALID_FROM));
        builder.setImageUrl(getString(info, IMAGE_URL));
        builder.setLinkText(getString(info, LINK_TEXT));
        builder.setIcon(getString(info, ICON_URL));
        return builder.build();
    }

    private String getString(JSONObject object, String property) {
        Object value = object.get(property);
        return value == null ? null : value.toString();
    }

    private boolean getBoolean(JSONObject object, String property) {
        return Boolean.TRUE.toString().equals(getString(object, property));
    }

    private Date getDate(JSONObject object, String property) {
        String value = getString(object, property);
        try {
            return value == null ? null : DATE_FORMAT.parse(value);
        } catch (java.text.ParseException e) {
            handleException(Level.WARNING, e);
            return null;
        }
    }

    private void handleException(Level level, Exception exception) {
        if (level.intValue() >= Level.WARNING.intValue()) {
            ErrorUtil.handleBackgroundException(
                    "Exception while data querying operation", exception);
        } else {
            LOG.log(level, null, exception);
        }
    }

    public static NotificationsService getInstance() {
        return INSTANCE;
    }

    public static class InvalidCredentialsException extends Exception {

        private static final long serialVersionUID = -5466608327464552050L;
    }

}
