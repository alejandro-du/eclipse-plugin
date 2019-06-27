package com.vaadin.integration.eclipse.flow.wizard;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.maven.model.Model;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.wizards.ImportMavenProjectsJob;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vaadin.integration.eclipse.flow.service.AnalyticsService;
import com.vaadin.integration.eclipse.flow.util.LogUtil;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public class StarterManager {

    private static final String START_SERVICE_URL = "https://vaadin.com/vaadincom/start-service/";
    private static final String STARTER_DOWNLOAD_URL = START_SERVICE_URL
            + "%s/%s";

    private static final String TMP_STARTER_SPLIT_SUFFIX = "-tmp-starter-split-suffix-";

    public static List<Starter> fetchStarters() throws IOException {
        Response response = Request.Get(START_SERVICE_URL)
                .addHeader("accept", "application/json").execute();
        String content = response.returnContent().asString();
        Type listType = new TypeToken<ArrayList<Starter>>() {
        }.getType();
        return new Gson().fromJson(content, listType);
    }

    public static List<Starter> getSupportedStarters(List<Starter> starters) {
        List<Starter> supportedStarters = new ArrayList<Starter>(
                starters.size());
        for (Starter starter : starters) {
            Starter supportedStarter = getSupportedStarter(starter);
            if (supportedStarter != null) {
                supportedStarters.add(supportedStarter);
            }
        }
        return supportedStarters;
    }

    public static Starter getSupportedStarter(Starter starter) {
        if (!"pre-release".equals(starter.getRelease())
                || starter.isCommercial()
                || "component".equals(starter.getId())) {
            return null;
        }
        List<String> techStacks = starter.getTechStacks();
        techStacks.remove("html");
        return techStacks.size() != 0 ? starter : null;
    }

    public static File download(String release, String id, String appName,
            String groupId, String stack)
            throws IOException, URISyntaxException, FileNotFoundException {
        URIBuilder uriBuilder = new URIBuilder(
                String.format(STARTER_DOWNLOAD_URL, release, id));
        uriBuilder.addParameter("appName", appName);
        uriBuilder.addParameter("groupId", groupId);
        uriBuilder.addParameter("techStack", stack);

        HttpResponse response = Request.Get(uriBuilder.build().toString())
                .addHeader("accept", "application/json").execute()
                .returnResponse();
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new FileNotFoundException(
                    EntityUtils.toString(response.getEntity()));
        }

        Header contentDispositionHeader = response
                .getFirstHeader("content-disposition");
        String fileNameFull = contentDispositionHeader.getElements()[0]
                .getParameterByName("filename").getValue();

        int extensionIndex = fileNameFull.indexOf(".zip");
        String fileName = fileNameFull.substring(0, extensionIndex);

        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File tmpFile = File.createTempFile(fileName + TMP_STARTER_SPLIT_SUFFIX,
                ".zip", tmpDir);
        tmpFile.deleteOnExit();

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(tmpFile);
            response.getEntity().writeTo(out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
        return tmpFile;
    }

    public static File unzip(String path, File starter) throws ZipException {
        int splitIndex = starter.getName().indexOf(TMP_STARTER_SPLIT_SUFFIX);
        String fileName = starter.getName().substring(0, splitIndex);
        String directoryPath = path + File.separatorChar + fileName;
        String unzipPath = findUnzipPath(directoryPath);

        ZipFile zipFile = new ZipFile(starter);
        zipFile.extractAll(unzipPath);
        return new File(unzipPath);
    }

    private static String findUnzipPath(String directoryPath) {
        if (new File(directoryPath).exists()) {
            int counter = 1;
            while (new File(directoryPath + counter).exists()) {
                counter++;
            }
            directoryPath = directoryPath + counter;
        }
        return directoryPath;
    }

    public static void scheduleStarterImport(final Starter starter,
            final String projectName, final String groupId,
            final String stack) {
        IRunnableWithProgress op = new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException {
                try {
                    monitor.beginTask("Downloading starter project", 100);
                    monitor.internalWorked(50);
                    File starterFile = StarterManager.download(
                            starter.getRelease(), starter.getId(), projectName,
                            groupId, stack);
                    monitor.internalWorked(80);
                    File starterDirectory = StarterManager
                            .unzip(ResourcesPlugin.getWorkspace().getRoot()
                                    .getLocation().toOSString(), starterFile);
                    StarterManager.scheduleMavenImport(starterDirectory);
                    AnalyticsService.trackProjectCreate(starter.getId(), stack);
                    monitor.done();
                } catch (CoreException e) {
                    throw new InvocationTargetException(e,
                            "Error occured during Maven import of the starter project.");
                } catch (FileNotFoundException e) {
                    throw new InvocationTargetException(e,
                            "Starter project download failed. Invalid server response.");
                } catch (Exception e) {
                    throw new InvocationTargetException(e,
                            "Error occured during starter project download.");
                }
            }
        };

        IWorkbenchWindow win = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow();
        try {
            new ProgressMonitorDialog(win.getShell()).run(true, false, op);
        } catch (InvocationTargetException e) {
            LogUtil.handleBackgroundException(e.getMessage(), e);
            LogUtil.displayError(e.getMessage(), null, win.getShell());
        } catch (InterruptedException e) {
            LogUtil.handleBackgroundException(
                    "Starter project download was interrupted.", e);
        }
    }

    @SuppressWarnings(value = { "restriction", "unchecked" })
    private static void scheduleMavenImport(File starterDirectory)
            throws CoreException {
        File pomFile = new File(starterDirectory,
                IMavenConstants.POM_FILE_NAME);

        Model model = MavenPlugin.getMavenModelManager()
                .readMavenModel(pomFile);
        MavenProjectInfo mavenProjectInfo = new MavenProjectInfo(
                "/" + IMavenConstants.POM_FILE_NAME, pomFile, model, null);

        ImportMavenProjectsJob job = new ImportMavenProjectsJob(
                Collections.singleton(mavenProjectInfo), Collections.EMPTY_LIST,
                new ProjectImportConfiguration());
        job.setRule(MavenPlugin.getProjectConfigurationManager().getRule());
        job.schedule();
    }
}
