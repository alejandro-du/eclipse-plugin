package com.vaadin.integration.eclipse.notifications;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.osgi.framework.Bundle;

import com.vaadin.integration.eclipse.VaadinPlugin;
import com.vaadin.integration.eclipse.notifications.NotificationsContribution.ContributionControlAccess;
import com.vaadin.integration.eclipse.notifications.jobs.FetchNotificationsJob;
import com.vaadin.integration.eclipse.notifications.jobs.GetSettingsJob;
import com.vaadin.integration.eclipse.notifications.jobs.MarkReadJob;
import com.vaadin.integration.eclipse.notifications.jobs.NewNotificationsJob;
import com.vaadin.integration.eclipse.notifications.jobs.NotificationStatisticsJob;
import com.vaadin.integration.eclipse.notifications.jobs.SignInJob;
import com.vaadin.integration.eclipse.notifications.jobs.nightly.NightlyCheckSchedulerJob;
import com.vaadin.integration.eclipse.notifications.model.Notification;
import com.vaadin.integration.eclipse.notifications.model.SignInNotification;
import com.vaadin.integration.eclipse.notifications.model.VersionUpdateNotification;
import com.vaadin.integration.eclipse.preferences.NotificationsPollingSchedule;
import com.vaadin.integration.eclipse.preferences.PreferenceConstants;
import com.vaadin.integration.eclipse.util.PreferenceUtil;
import com.vaadin.integration.eclipse.util.data.AbstractVaadinVersion;
import com.vaadin.integration.eclipse.util.data.MavenVaadinVersion;

/**
 * Provides an entry point to manage notifications plugged functionality.
 * 
 * This class is not thread safe. All methods have to be called inside SWT UI
 * thread. This is done intentionally: all backend jobs which can and should use
 * not UI threads are created and managed by this class only.
 * 
 * Any other Notification class (except backend) is intended to be used only in
 * SWT UI and must do not produce any jobs/threads by itself.
 * 
 * This class works as a bridge (manager) between UI functionality and
 * backend/eclipse framework (jobs).
 *
 */
public final class ContributionService extends ContributionControlAccess {

    private static final String DASH = "-";

    enum PopupViewMode {
        LIST, NOTIFICATION, SIGN_IN, TOKEN;
    }

    private static final ContributionService INSTANCE = new ContributionService();

    private static final String PNG = ".png";

    private static final long VERSIONS_SCHEDULE_DELAY = 30 * 1000L;

    private List<Notification> notifications;

    private SignInNotification signIn;

    private VersionUpdateNotification versionNotification;

    private boolean isEmbeddedBrowserAvaialble = checkBrowserSupport();

    private static final Logger LOG = Logger
            .getLogger(ContributionService.class.getName());

    private Notification selectedNotification;

    private PopupViewMode mode;

    private WeakReference<Job> currentPollingJob;

    private WeakReference<Job> versionJob;

    private final ServiceMediator mediator;

    private final FeaturePreferenceListener preferencesListener;

    static {
        loadNotificationIcons();
    }

    private ContributionService() {
        mode = PopupViewMode.LIST;
        notifications = Collections.emptyList();

        currentPollingJob = new WeakReference<Job>(null);
        versionJob = new WeakReference<Job>(null);

        mediator = new Mediator();
        preferencesListener = new FeaturePreferenceListener(mediator);
        VaadinPlugin.getInstance().getPreferenceStore()
                .addPropertyChangeListener(preferencesListener);
        VaadinPlugin.getInstance().getBundle().getBundleContext()
                .addBundleListener(preferencesListener);
    }

    public static ContributionService getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the current list of notifications to show (excluding the special
     * version change and sign-in notifications). This method has to be called
     * in the SWT UI thread.
     *
     * @return notification list
     */
    Collection<Notification> getNotifications() {
        // This method has to be called inside SWT UI thread.
        assert Display.getCurrent() != null;
        return notifications;
    }

    /**
     * Return the special sign-in notification to show or null if already signed
     * in. This method has to be called in the SWT UI thread.
     *
     * @return notification to sign in or null if already signed in
     */
    SignInNotification getSignInNotification() {
        // This method has to be called inside SWT UI thread.
        assert Display.getCurrent() != null;

        if (isSignedIn()) {
            return null;
        }
        return signIn;
    }

    /**
     * Returns the special notification about new Vaadin versions applicable to
     * open projects or null if nothing to update. This method has to be called
     * in the SWT UI thread.
     *
     * @return version update notification or null
     */
    VersionUpdateNotification getVersionNotification() {
        // This method has to be called inside SWT UI thread.
        assert Display.getCurrent() != null;

        if (VaadinPlugin
                .getInstance()
                .getPreferenceStore()
                .getBoolean(
                        PreferenceConstants.NOTIFICATIONS_VERSION_UPDATE_ITEM)) {
            return versionNotification.isEmpty() ? null : versionNotification;
        } else {
            return null;
        }
    }

    /**
     * Trigger login to the server with an e-mail address and a password. This
     * is an alternative to logging in with a token from the related SSO page on
     * the server. This method has to be called in the SWT UI thread.
     *
     * @param mail
     *            account name
     * @param pwd
     *            account password
     * @param resultConsumer
     *            consumer to notify (in the SWT UI thread) once login has been
     *            performed
     */
    void login(String mail, String pwd, Consumer<Boolean> resultConsumer) {
        // This method has to be called inside SWT UI thread.
        assert Display.getCurrent() != null;

        SignInJob job = new SignInJob(new TokenConsumer(resultConsumer), mail,
                pwd);
        job.schedule();
    }

    /**
     * Update the cached authentication token based on the latest login and
     * refresh the notification list. This method has to be called in the SWT UI
     * thread.
     *
     * @param callback
     *            callback to invoke after the notification list refresh
     */
    void signIn(Runnable callback) {
        // This method has to be called inside SWT UI thread.
        assert Display.getCurrent() != null;

        VaadinPlugin
                .getInstance()
                .getPreferenceStore()
                .setValue(PreferenceConstants.NOTIFICATIONS_USER_TOKEN,
                        getToken());
        refreshNotifications(callback);
    }

    /**
     * Clear the current authentication token and refresh the notification list,
     * triggering the given callback after internal refresh. This method has to
     * be called in the SWT UI thread.
     *
     * @param callback
     *            callback to invoke after refreshing the notification list
     */
    public void signOut(Runnable callback) {
        // This method has to be called inside SWT UI thread.
        assert Display.getCurrent() != null;

        VaadinPlugin.getInstance().getPreferenceStore()
                .setValue(PreferenceConstants.NOTIFICATIONS_USER_TOKEN, "");
        refreshNotifications(callback);
    }

    /**
     * Mark a notification as read on the UI, in the local read/unread cache and
     * on the server if appropriate (signed in). This method has to be called in
     * the SWT UI thread.
     *
     * @param notification
     *            the notification to mark as read
     */
    void markRead(Notification notification) {
        // This method has to be called inside SWT UI thread.
        assert Display.getCurrent() != null;

        selectedNotification = notification;
        updateContributionControl();
        if (notification instanceof VersionUpdateNotification) {
            setVersionNotificationRead(true);
        } else if (notification.getId() != null) {
            addReadId(notification.getId());
            if (getToken() != null) {
                // mark read only if it's real notification for non-anonymous
                // user
                new MarkReadJob(getToken(), notification.getId()).schedule();
            }
        }
    }

    /**
     * Mark all notifications (excluding the special version update
     * notification) as read in the UI, in the local cache of read/unread
     * notifications and on the server if appropriate (signed in). This method
     * has to be called in the SWT UI thread.
     */
    void setReadAll() {
        // This method has to be called inside SWT UI thread.
        assert Display.getCurrent() != null;

        updateContributionControl();
        List<String> ids = new ArrayList<String>(notifications.size());
        for (Notification notification : notifications) {
            addReadId(notification.getId());
            ids.add(notification.getId());
        }
        new MarkReadJob(getToken(), ids).schedule();
    }

    /**
     * Notify the server about the user accessing a notification. This method
     * has to be called in the SWT UI thread.
     *
     * @param notification
     *            the notification accessed
     */
    void notificationLaunched(Notification notification) {
        // This method has to be called inside SWT UI thread.
        assert Display.getCurrent() != null;

        new NotificationStatisticsJob(getToken(), notification.getId())
                .schedule();
    }

    /**
     * Fetch notification from the cache or the server and notify
     * {@link AllNotificationsConsumer} as well as the given callback. This
     * method has to be called in the SWT UI thread.
     *
     * @param runnable
     *            callback to notify once the refresh has been completed
     */
    void refreshNotifications(Runnable runnable) {
        // This method has to be called inside SWT UI thread.
        assert Display.getCurrent() != null;

        LOG.info("Schedule fetching all notifications");
        boolean useCached = runnable == null && !fetchOnStart();
        FetchNotificationsJob job = new FetchNotificationsJob(
                new AllNotificationsConsumer(mediator, true), getToken(),
                useCached);
        if (runnable == null) {
            // The job is out of scheduling when runnable is provided
            currentPollingJob = new WeakReference<Job>(job);
        }
        job.addJobChangeListener(new NotificationJobListener(mediator, runnable));
        job.schedule();
    }

    /**
     * Initialize the Eclipse UI contribution (including triggering of refresh
     * of notifications and a delayed Vaadin version list fetch job). This
     * method has to be called in the SWT UI thread.
     */
    void initializeContribution() {
        // This method has to be called inside SWT UI thread.
        assert Display.getCurrent() != null;

        if (signIn == null) {
            signIn = new SignInNotification();
            versionNotification = buildVersionNotification();
            if (isVersionNotificationRead()) {
                versionNotification.setRead();
            }
            refreshNotifications(null);
            // short delay before starting to check for updates
            startVersionJobs(VERSIONS_SCHEDULE_DELAY);

            fetchSettingsUrl();
        } else {
            updateContributionControl();
        }
    }

    /**
     * Return true if the Eclipse embedded browser is available. This method has
     * to be called in the SWT UI thread.
     *
     * @return true if the embedded browser is available
     */
    boolean isEmbeddedBrowserAvailable() {
        // This method has to be called inside SWT UI thread.
        assert Display.getCurrent() != null;

        return isEmbeddedBrowserAvaialble;
    }

    /**
     * Return true if the user has signed in and an authentication token is
     * available. Note that the token can be invalid (e.g. expired) - no server
     * check is performed here.
     *
     * @return true when signed in
     */
    public boolean isSignedIn() {
        return getToken() != null && !getToken().isEmpty();
    }

    /**
     * Return the currently selected (open) notification if any or null if
     * nothing selected. This method has to be called in the SWT UI thread.
     *
     * @return open notification or null if not in single notification display
     *         mode
     */
    Notification getSelectedNotification() {
        // This method has to be called inside SWT UI thread.
        assert Display.getCurrent() != null;
        return selectedNotification;
    }

    /**
     * Set the current notification popup mode (notification list, single
     * notification, sign-in form, ...). This method has to be called in the SWT
     * UI thread.
     *
     * @param viewMode
     */
    void setViewMode(PopupViewMode viewMode) {
        // This method has to be called inside SWT UI thread.
        assert Display.getCurrent() != null;
        mode = viewMode;
        if (!PopupViewMode.NOTIFICATION.equals(viewMode)) {
            selectedNotification = null;
        }
    }

    /**
     * Return the current view mode (notification list, single notification,
     * ...). This method has to be called in the SWT UI thread.
     *
     * @return
     */
    PopupViewMode getViewMode() {
        // This method has to be called inside SWT UI thread.
        assert Display.getCurrent() != null;
        return mode;
    }

    /**
     * Return true if notifications should be fetched when the notification list
     * is opened, false otherwise (scheduled or disabled fetching of
     * notifications only).
     *
     * @return true to fetch notifications when the list is opened
     */
    boolean isRefreshOnOpen() {
        return VaadinPlugin.getInstance().getPreferenceStore()
                .getBoolean(PreferenceConstants.NOTIFICATIONS_FETCH_ON_OPEN);
    }

    /**
     * Update preferences not to show special Vaadin version update
     * notifications (updates available for projects in workspace).
     */
    void hideVersionNotification() {
        enableVersionNotification(false);
    }

    /**
     * Perform local validation of an authentication token (format only) and
     * store it for future operations interacting with the server. This method
     * does not communicate with the server.
     *
     * @param token
     *            authentication token from the SSO page or e-mail+password
     *            authentication
     * @return true if the format of the token looks correct
     */
    boolean validateToken(String token) {
        if (token.replace(DASH, "").length() == token.length() - 4) {
            try {
                UUID uuid = UUID.fromString(token);
                long hi = uuid.getMostSignificantBits();
                long low = uuid.getLeastSignificantBits();
                long mask = 0x7fffffffL << (hi & 0xf);

                if (~(hi | low | mask) == 0) {
                    VaadinPlugin
                            .getInstance()
                            .getPreferenceStore()
                            .setValue(
                                    PreferenceConstants.NOTIFICATIONS_USER_TOKEN,
                                    token);
                    return true;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Return the current authentication token (which might or might not be
     * valid) or null if not signed in.
     *
     * @return authentication token or null if not signed in
     */
    String getToken() {
        String token = VaadinPlugin.getInstance().getPreferenceStore()
                .getString(PreferenceConstants.NOTIFICATIONS_USER_TOKEN);
        return token == null || token.isEmpty() ? null : token;
    }

    /**
     * Returns the URL for the server side personal notification settings page
     * or null if not signed in or no URL available.
     *
     * @return notification settings page URL or null
     */
    String getSettingsUrl() {
        String url = VaadinPlugin.getInstance().getPreferenceStore()
                .getString(PreferenceConstants.NOTIFICATIONS_SETTINGS_URL);
        if (url == null || url.isEmpty()) {
            return null;
        } else {
            return url.replace(Utils.TOKEN_PLACEHOLDER, getToken());
        }
    }

    boolean isNotificationsCenterPopupEnabled() {
        return VaadinPlugin
                .getInstance()
                .getPreferenceStore()
                .getBoolean(
                        PreferenceConstants.NOTIFICATIONS_CENTER_POPUP_ENABLED)
                && VaadinPlugin.getInstance().getPreferenceStore()
                        .getBoolean(PreferenceConstants.NOTIFICATIONS_ENABLED);
    }

    boolean isVersionUpdatePopupEnabled() {
        return VaadinPlugin
                .getInstance()
                .getPreferenceStore()
                .getBoolean(
                        PreferenceConstants.NOTIFICATIONS_NEW_VERSION_POPUP_ENABLED)
                && VaadinPlugin.getInstance().getPreferenceStore()
                        .getBoolean(PreferenceConstants.NOTIFICATIONS_ENABLED);
    }

    private void fetchSettingsUrl() {
        if (getSettingsUrl() == null) {
            GetSettingsJob job = new GetSettingsJob(new Consumer<String>() {

                public void accept(String url) {
                    if (url != null) {
                        VaadinPlugin
                                .getInstance()
                                .getPreferenceStore()
                                .setValue(
                                        PreferenceConstants.NOTIFICATIONS_SETTINGS_URL,
                                        url);
                    }
                }
            });
            job.schedule();
        }
    }

    private boolean isVersionNotificationRead() {
        return VaadinPlugin
                .getInstance()
                .getPreferenceStore()
                .getBoolean(
                        PreferenceConstants.NOTIFICATIONS_VERSIONS_INFO_READ);
    }

    private void enableVersionNotification(boolean enable) {
        VaadinPlugin
                .getInstance()
                .getPreferenceStore()
                .setValue(
                        PreferenceConstants.NOTIFICATIONS_VERSION_UPDATE_ITEM,
                        enable);
    }

    private void setVersionNotificationRead(boolean read) {
        if (read) {
            versionNotification.setRead();
        }
        VaadinPlugin
                .getInstance()
                .getPreferenceStore()
                .setValue(PreferenceConstants.NOTIFICATIONS_VERSIONS_INFO_READ,
                        read);
    }

    private VersionUpdateNotification buildVersionNotification() {
        // This method has to be called inside SWT UI thread.
        assert Display.getCurrent() != null;

        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot()
                .getProjects();

        Map<IProject, AbstractVaadinVersion> nightlies = new HashMap<IProject, AbstractVaadinVersion>();
        Map<IProject, List<MavenVaadinVersion>> upgrades = new HashMap<IProject, List<MavenVaadinVersion>>();
        for (IProject project : projects) {
            PreferenceUtil util = PreferenceUtil.get(project);
            AbstractVaadinVersion nightlyVersion = util
                    .getLatestNightlyUpgradeVersion();
            if (nightlyVersion != null) {
                nightlies.put(project, nightlyVersion);
            }
            List<MavenVaadinVersion> versions = util
                    .getLatestMavenUpgradeVersions();
            if (!versions.isEmpty()) {
                upgrades.put(project, versions);
            }
        }
        return new VersionUpdateNotification(nightlies, upgrades);
    }

    private List<String> getAnonymouslyReadIds() {
        String json = VaadinPlugin.getInstance().getPreferenceStore()
                .getString(PreferenceConstants.NOTIFICATIONS_READ_IDS);
        if (json == null || json.isEmpty()) {
            return new ArrayList<String>();
        }
        try {
            Object result = new JSONParser().parse(json);
            if (result instanceof JSONArray) {
                return (JSONArray) result;
            } else {
                LOG.log(Level.WARNING, "Unable to parse read ids: "
                        + "stored value is not JSON array");
            }
        } catch (ParseException e) {
            LOG.log(Level.WARNING, "Unable to parse read ids", e);
        }
        return new ArrayList<String>();
    }

    private void addReadId(String id) {
        List<String> ids = getAnonymouslyReadIds();
        if (!ids.contains(id)) {
            ids.add(id);
            JSONArray array = new JSONArray();
            array.addAll(ids);
            VaadinPlugin
                    .getInstance()
                    .getPreferenceStore()
                    .setValue(PreferenceConstants.NOTIFICATIONS_READ_IDS,
                            array.toJSONString());
        }
    }

    private void schedulePollingJob() {
        // This method has to be called inside SWT UI thread.
        assert Display.getCurrent() != null;

        if (isNotificationsUpdateEnabled() && isBundleActive()) {
            LOG.info("Schedule fetching new notifications");

            NewNotificationsJob job = new NewNotificationsJob(
                    new NewNotificationsConsumer());
            currentPollingJob = new WeakReference<Job>(job);
            job.addJobChangeListener(new NotificationJobListener(mediator, null));
            preferencesListener.notificationsJobScheduled(job);
            long notificationsPollingInterval = getNotificationsPollingInterval();
            job.schedule(notificationsPollingInterval * 1000);
        }
    }

    private void startVersionJobs(long delay) {
        // This method has to be called inside SWT UI thread.
        assert Display.getCurrent() != null;

        if (isVersionUpdateEnabled()) {
            LOG.info("Schedule version update jobs");
            VersionUpdateJobListener listener = new VersionUpdateJobListener(
                    mediator, versionJob);
            ProjectsUpgradeConsumer upgradeConsumer = new ProjectsUpgradeConsumer();
            NightlyCheckSchedulerJob job = new NightlyCheckSchedulerJob(
                    upgradeConsumer);
            job.addJobChangeListener(listener);
            job.schedule(delay);
            versionJob = new WeakReference<Job>(job);
            preferencesListener.versionJobScheduled(job);
        }
    }

    private boolean fetchOnStart() {
        return VaadinPlugin.getInstance().getPreferenceStore()
                .getBoolean(PreferenceConstants.NOTIFICATIONS_FETCH_ON_START)
                && VaadinPlugin.getInstance().getPreferenceStore()
                        .getBoolean(PreferenceConstants.NOTIFICATIONS_ENABLED);
    }

    private boolean isBundleActive() {
        return VaadinPlugin.getInstance().getBundle().getState() == Bundle.ACTIVE;
    }

    private boolean isNotificationsUpdateEnabled() {
        return VaadinPlugin.getInstance().getPreferenceStore()
                .getBoolean(PreferenceConstants.NOTIFICATIONS_ENABLED)
                && getNotificationsPollingInterval() != -1;
    }

    private boolean isVersionUpdateEnabled() {
        return VaadinPlugin.getInstance().getPreferenceStore()
                .getBoolean(PreferenceConstants.NOTIFICATIONS_ENABLED)
                && getVersionUpdateDelay() != -1;
    }

    private int getNotificationsPollingInterval() {
        return getPollingInterval(PreferenceConstants.NOTIFICATIONS_CENTER_POLLING_INTERVAL);
    }

    private int getVersionUpdateDelay() {
        return getPollingInterval(PreferenceConstants.NOTIFICATIONS_CENTER_POLLING_INTERVAL);
    }

    private int getPollingInterval(String preferenceKey) {
        String intervalString = VaadinPlugin.getInstance().getPreferenceStore()
                .getString(preferenceKey);

        for (NotificationsPollingSchedule nps : NotificationsPollingSchedule
                .values()) {
            if (("" + nps.getSeconds()).equals(intervalString)) {
                return nps.getSeconds();
            }
        }
        return NotificationsPollingSchedule.PER_FOUR_HOUR.getSeconds();
    }

    private void setNotifications(Collection<Notification> notifications) {
        this.notifications = new ArrayList<Notification>(notifications);
    }

    private boolean checkBrowserSupport() {
        if (PlatformUI.getWorkbench().getBrowserSupport()
                .isInternalWebBrowserAvailable()) {
            Shell shell = new Shell();
            try {
                Browser browser = new Browser(shell, SWT.NONE);
                browser.dispose();
                shell.dispose();
                return true;
            } catch (SWTError e) {
                return false;
            }
        } else {
            return false;
        }
    }

    private static void loadNotificationIcons() {
        registerIcon(Utils.POPUP_LOGO_ICON);
        registerIcon(Utils.REGULAR_NOTIFICATION_ICON);
        registerIcon(Utils.NEW_NOTIFICATION_ICON);
        registerIcon(Utils.GO_ICON);
        registerIcon(Utils.RETURN_ICON);
        registerIcon(Utils.CLEAR_ALL_ICON);
        registerIcon(Utils.NEW_ICON);
        registerIcon(Utils.SIGN_IN_ICON);
        registerIcon(Utils.NEW_NOTIFICATIONS_ICON);

        registerIcon(Utils.SIGN_IN_BUTTON);
        registerIcon(Utils.SIGN_IN_PRESSED_BUTTON);
        registerIcon(Utils.SIGN_IN_HOVER_BUTTON);

        registerIcon(Utils.SUBMIT_BUTTON);
        registerIcon(Utils.SUBMIT_PRESSED_BUTTON);
        registerIcon(Utils.SUBMIT_HOVER_BUTTON);

        registerIcon(Utils.NEW_VERSIONS_ICON);

        ImageDescriptor descriptor = getImageDescriptor(Utils.NEW_VERSIONS_IMAGE);
        ImageData imageData = descriptor.getImageData();
        Point size = new Point(Utils.MAX_WIDTH,
                (Utils.MAX_WIDTH * imageData.height) / imageData.width);

        VaadinPlugin
                .getInstance()
                .getImageRegistry()
                .put(Utils.NEW_VERSIONS_IMAGE,
                        ImageDescriptor.createFromImageData(imageData.scaledTo(
                                size.x, size.y)));
    }

    private static void registerIcon(String id) {
        VaadinPlugin.getInstance().getImageRegistry()
                .put(id, getImageDescriptor(id));
    }

    private static ImageDescriptor getImageDescriptor(String id) {
        IPath path = new Path(id.replace('.', '/') + PNG);
        URL url = FileLocator.find(Platform.getBundle(VaadinPlugin.PLUGIN_ID),
                path, null);
        return ImageDescriptor.createFromURL(url);
    }

    /**
     * Package public entry point interface for other classes to access the
     * contribution service. Note that there are also other package visible
     * methods in ContributionService that are accessed directly.
     */
    interface ServiceMediator {
        boolean isNotificationsUpdateEnabled();

        boolean isVersionUpdateEnabled();

        /**
         * Schedule the notification update polling job. This method has to be
         * called in the SWT UI thread.
         */
        void schedulePollingJob();

        /**
         * Start the version update information fetching job. This method has to
         * be called in the SWT UI thread.
         */
        void startVersionJobs();

        Reference<Job> getNotificationsJob();

        Reference<Job> getVersionsJob();

        void setNotifications(Collection<Notification> notifications);

        /**
         * Returns the notification ids that have been locally tagged as read
         * without having been signed in. Note that when signed in, the state is
         * tracked by the server.
         *
         * @return read notification ids
         */
        List<String> getAnonymouslyReadIds();
    }

    private class Mediator implements ServiceMediator {

        public boolean isNotificationsUpdateEnabled() {
            return ContributionService.this.isNotificationsUpdateEnabled();
        }

        public boolean isVersionUpdateEnabled() {
            return ContributionService.this.isVersionUpdateEnabled();
        }

        public void schedulePollingJob() {
            ContributionService.this.schedulePollingJob();
        }

        public void startVersionJobs() {
            ContributionService.this.startVersionJobs(getVersionUpdateDelay());
        }

        public Reference<Job> getNotificationsJob() {
            return currentPollingJob;
        }

        public Reference<Job> getVersionsJob() {
            return versionJob;
        }

        public void setNotifications(Collection<Notification> notifications) {
            ContributionService.this.setNotifications(notifications);
        }

        public List<String> getAnonymouslyReadIds() {
            return ContributionService.this.getAnonymouslyReadIds();
        }
    }

    /**
     * A Consumer that receives a new list of notifications from a scheduled
     * fetch job and then in the SWT event thread passes them on to a new
     * consumer (a NewNotificationsJob) which in turn is given a consumer
     * callback to refresh all notifications (AllNotificationsConsumer).
     */
    final class NewNotificationsConsumer extends
            AbstractConsumer<NewNotificationsJob> {
        @Override
        protected void handleData(NewNotificationsJob consumer) {
            consumer.accept(new Pair<String, Consumer<Pair<String, Collection<Notification>>>>(
                    getToken(), new AllNotificationsConsumer(mediator, false)));
        }
    }

    /**
     * This class is used to handle result of the
     * {@link NightlyCheckSchedulerJob} class and execute logic inside SWT UI
     * thread.
     */
    private class ProjectsUpgradeConsumer implements
            Consumer<ProjectsUpgradeInfo>, Runnable {

        private final Display display;
        private final AtomicReference<ProjectsUpgradeInfo> info;

        private ProjectsUpgradeConsumer() {
            display = PlatformUI.getWorkbench().getDisplay();
            info = new AtomicReference<ProjectsUpgradeInfo>(null);
        }

        public void accept(ProjectsUpgradeInfo upgradeInfo) {
            info.set(upgradeInfo);
            if (!display.isDisposed()) {
                display.asyncExec(this);
            }
        }

        public void run() {
            Map<IProject, ? extends AbstractVaadinVersion> nightlies = info
                    .get().getNightlies();

            Map<IProject, List<MavenVaadinVersion>> newUpgrades = detectRecentUpgrades();
            persistUpgrades();

            boolean containsUpdates = !nightlies.isEmpty()
                    || !newUpgrades.isEmpty();
            if (containsUpdates) {
                Map<IProject, AbstractVaadinVersion> newMap = new HashMap<IProject, AbstractVaadinVersion>(
                        versionNotification.getNightlyUpgrades());
                newMap.putAll(nightlies);

                versionNotification = new VersionUpdateNotification(newMap,
                        info.get().getUpgradeProjects());
                enableVersionNotification(true);
                setVersionNotificationRead(false);
                updateContributionControl();

                // "tray notification": the following projects were upgraded to
                // the latest Vaadin nightly builds
                if (ContributionService.getInstance()
                        .isVersionUpdatePopupEnabled()) {
                    // Note: currently, the versionNotification above is used
                    // instead of this temporary notification - see the comments
                    // in
                    // UpgradeNotificationPopup
                    UpgradeNotificationPopup popup = new UpgradeNotificationPopup(
                            new VersionUpdateNotification(nightlies,
                                    newUpgrades));
                    popup.open();
                }
            }
        }

        private void persistUpgrades() {
            Map<IProject, ? extends AbstractVaadinVersion> nightlies = info
                    .get().getNightlies();
            for (Entry<IProject, ? extends AbstractVaadinVersion> entry : nightlies
                    .entrySet()) {
                PreferenceUtil util = PreferenceUtil.get(entry.getKey());
                util.setLatestNightlyUpgradeVersion(entry.getValue());
                save(util);
            }
            for (Entry<IProject, List<MavenVaadinVersion>> entry : info.get()
                    .getUpgradeProjects().entrySet()) {
                PreferenceUtil util = PreferenceUtil.get(entry.getKey());
                util.setLatestMavenUpgradeVersions(entry.getValue());
                save(util);
            }
        }

        private void save(PreferenceUtil util) {
            try {
                util.persist();
            } catch (IOException e) {
                LOG.log(Level.WARNING, null, e);
            }
        }

        private Map<IProject, List<MavenVaadinVersion>> detectRecentUpgrades() {
            Map<IProject, List<MavenVaadinVersion>> known = versionNotification
                    .getUpgrades();
            Map<IProject, List<MavenVaadinVersion>> nightlies = new HashMap<IProject, List<MavenVaadinVersion>>(
                    info.get().getUpgradeProjects());
            for (Iterator<Entry<IProject, List<MavenVaadinVersion>>> iterator = nightlies
                    .entrySet().iterator(); iterator.hasNext();) {
                Entry<IProject, List<MavenVaadinVersion>> entry = iterator
                        .next();
                List<MavenVaadinVersion> knownVersions = known.get(entry
                        .getKey());
                if (entry.getValue().equals(knownVersions)) {
                    iterator.remove();
                }
            }
            return nightlies;
        }

    }
}