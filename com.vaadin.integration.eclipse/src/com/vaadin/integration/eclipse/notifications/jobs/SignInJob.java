package com.vaadin.integration.eclipse.notifications.jobs;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.vaadin.integration.eclipse.notifications.Consumer;
import com.vaadin.integration.eclipse.notifications.model.NotificationsService;
import com.vaadin.integration.eclipse.notifications.model.NotificationsService.InvalidCredentialsException;

/**
 * A job performing a login on vaadin.com with a username and a password and
 * retrieving an authentication token. The alternative for this is using SSO and
 * explicitly copying the token from the website - see PopupUpdateManager and
 * TokenInputComposite.
 */
public class SignInJob extends AbstractNotificationJob<String> {

    private final String login;
    private final String passwd;

    private static final Logger LOG = Logger
            .getLogger(SignInJob.class.getName());

    /**
     * @param consumer
     *            Consumer which accepts resulting token. null value for the
     *            token means auth failure.
     * @param login
     *            Login
     * @param pwd
     *            Password
     */
    public SignInJob(Consumer<String> consumer, String login, String pwd) {
        super(Messages.Notifications_SignInJob, consumer);
        setUser(false);
        setSystem(true);

        this.login = login;
        passwd = pwd;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask(Messages.Notifications_SignInTask, 1);

        String token = null;

        try {
            token = NotificationsService.getInstance().signIn(login, passwd);
        } catch (InvalidCredentialsException e) {
            LOG.log(Level.INFO, "Authentification failed", e);
        } finally {
            getConsumer().accept(token);
            monitor.worked(1);
            monitor.done();
        }

        return Status.OK_STATUS;
    }

}
