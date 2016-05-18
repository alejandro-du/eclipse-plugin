package com.vaadin.integration.eclipse.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.vaadin.integration.eclipse.builder.WidgetsetBuildManager;
import com.vaadin.integration.eclipse.util.PreferenceUtil;
import com.vaadin.integration.eclipse.util.VaadinPluginUtil;
import com.vaadin.integration.eclipse.util.WidgetsetUtil;

/**
 * Widgetset compilation preferences in project properties.
 */
public class WidgetsetParametersComposite extends Composite {

    private Combo styleCombo;
    private Combo parallelismCombo;
    private Button suspendAutomaticBuilds;
    private Button verboseCompilation;
    private Text extraParameters;
    private Text extraJvmParameters;
    private Button createDevelopmentModeLaunchButton;
    private Button createSuperDevelopmentModeLaunchButton;

    private IProject project = null;

    private String OBF_LABEL = "Obfuscated";
    private String PRETTY_LABEL = "Pretty";
    private String DETAILED_LABEL = "Detailed";
    private String DRAFT_LABEL = "Pretty + draft compile (Vaadin 6.3+)";

    public WidgetsetParametersComposite(Composite parent, int style) {
        super(parent, style);
    }

    public void setProject(IProject project) {
        this.project = project;

        // get values from project or defaults if none stored

        boolean enabled = WidgetsetUtil.isWidgetsetManagedByPlugin(project);
        setWidgetsetManagedByPlugin(enabled);

        boolean suspendBuilds = WidgetsetBuildManager
                .isWidgetsetBuildsSuspended(project);

        suspendAutomaticBuilds.setSelection(suspendBuilds);

        PreferenceUtil preferences = PreferenceUtil.get(project);
        boolean verboseOutput = preferences.isWidgetsetCompilationVerboseMode();

        verboseCompilation.setSelection(verboseOutput);

        String style = preferences.getWidgetsetCompilationStyle();
        if ("DETAILED".equals(style)) {
            styleCombo.setText(DETAILED_LABEL);
        } else if ("PRETTY".equals(style)) {
            styleCombo.setText(PRETTY_LABEL);
        } else if ("DRAFT".equals(style)) {
            styleCombo.setText(DRAFT_LABEL);
        } else {
            styleCombo.setText(OBF_LABEL);
        }

        String parallelism = preferences.getWidgetsetCompilationParallelism();
        parallelismCombo.setText(parallelism);

        String extraParams = preferences
                .getWidgetsetCompilationExtraParameters();
        extraParameters.setText(extraParams);

        String extraJvmParams = preferences
                .getWidgetsetCompilationExtraJvmParameters();
        extraJvmParameters.setText(extraJvmParams);

        boolean superDevModeSupported = VaadinPluginUtil
                .isSuperDevModeSupported(project);
        createSuperDevelopmentModeLaunchButton
                .setEnabled(superDevModeSupported);
    }

    private void setWidgetsetManagedByPlugin(boolean enabled) {
        setEnabled(enabled);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (styleCombo != null) {
            styleCombo.setEnabled(enabled);
        }
        if (parallelismCombo != null) {
            parallelismCombo.setEnabled(enabled);
        }
        if (suspendAutomaticBuilds != null) {
            suspendAutomaticBuilds.setEnabled(enabled);
        }
        if (verboseCompilation != null) {
            verboseCompilation.setEnabled(enabled);
        }
        if (extraParameters != null) {
            extraParameters.setEnabled(enabled);
        }
        if (extraJvmParameters != null) {
            extraJvmParameters.setEnabled(enabled);
        }
        if (createDevelopmentModeLaunchButton != null) {
            createDevelopmentModeLaunchButton.setEnabled(enabled);
        }
        if (createSuperDevelopmentModeLaunchButton != null) {
            createSuperDevelopmentModeLaunchButton.setEnabled(enabled);
        }
    }

    public Composite createContents() {
        setLayout(new GridLayout(1, false));
        setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

        createOptionsComposite(this);
        createHostedModeComposite(this);
        createSuperDevModeComposite(this);
        createInstructionsComposite(this);

        return this;
    }

    /**
     * Configurable options
     */
    private void createOptionsComposite(Composite parent) {
        Composite options = new Composite(parent, SWT.NULL);
        options.setLayout(new GridLayout(2, false));
        options.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

        suspendAutomaticBuilds = new Button(options, SWT.CHECK);
        suspendAutomaticBuilds.setText("Suspend automatic widgetset builds");
        GridData gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        gd.horizontalSpan = 2;
        suspendAutomaticBuilds.setLayoutData(gd);

        verboseCompilation = new Button(options, SWT.CHECK);
        verboseCompilation.setText("Verbose compilation output");
        gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        gd.horizontalSpan = 2;
        verboseCompilation.setLayoutData(gd);

        // compilation style (obfuscated/pretty)
        Label label = new Label(options, SWT.NULL);
        label.setText("Javascript style:");

        styleCombo = new Combo(options, SWT.BORDER | SWT.DROP_DOWN
                | SWT.READ_ONLY);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        styleCombo.setLayoutData(gd);

        styleCombo.add(OBF_LABEL);
        styleCombo.add(PRETTY_LABEL);
        styleCombo.add(DETAILED_LABEL);
        styleCombo.add(DRAFT_LABEL);

        // compiler parallelism

        label = new Label(options, SWT.NULL);
        label.setText("Compiler threads:");

        parallelismCombo = new Combo(options, SWT.BORDER | SWT.DROP_DOWN
                | SWT.READ_ONLY);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        parallelismCombo.setLayoutData(gd);

        parallelismCombo.add("");
        for (int i = 1; i <= 8; ++i) {
            parallelismCombo.add("" + i);
        }

        // compilation style (obfuscated/pretty)
        label = new Label(options, SWT.NULL);
        label.setText("Additional parameters for widgetset compiler:");

        extraParameters = new Text(options, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        gd.horizontalSpan = 2;
        extraParameters.setLayoutData(gd);

        label = new Label(options, SWT.NULL);
        label.setText("Additional JVM parameters for widgetset compiler:");
        extraJvmParameters = new Text(options, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        gd.horizontalSpan = 2;
        extraJvmParameters.setLayoutData(gd);
    }

    private void createInstructionsComposite(Composite parent) {
        Composite instructions = new Composite(parent, SWT.NULL);
        instructions.setLayout(new GridLayout(1, false));
        instructions.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true,
                false));

        // TODO label wrap would require specifying widthHint (really the
        // absolute width in pixels)!

        Label label = new Label(instructions, SWT.WRAP);
        label.setText("To optimize widgetset compilation time, modify the \"user.agent\" parameter in the\n"
                + "widgetset module file (.gwt.xml).");

    }

    /**
     * SuperDevMode launch configuration and instructions.
     */
    private void createSuperDevModeComposite(Composite parent) {
        Composite hosted = new Composite(parent, SWT.NULL);
        hosted.setLayout(new GridLayout(2, false));
        hosted.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

        // hosted mode launch creation button on the right
        createSuperDevelopmentModeLaunchButton = new Button(hosted, SWT.NULL);
        createSuperDevelopmentModeLaunchButton
                .setText("Create SuperDevMode launch");
        createSuperDevelopmentModeLaunchButton.setLayoutData(new GridData(
                SWT.RIGHT, SWT.BEGINNING, true, false));
        createSuperDevelopmentModeLaunchButton
                .addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        VaadinPluginUtil.createSuperDevModeLaunch(project);
                        // TODO add <set-configuration-property
                        // name="devModeRedirectEnabled" value="true" /> to
                        // widgetset or give instructions

                        // need to set as dirty to recompile it once
                        WidgetsetUtil.setWidgetsetDirty(project, true);
                    }
                });
    }

    /**
     * Hosted mode launch configuration and instructions.
     */
    private void createHostedModeComposite(Composite parent) {
        Composite hosted = new Composite(parent, SWT.NULL);
        hosted.setLayout(new GridLayout(2, false));
        hosted.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

        // hosted mode launch creation button on the right
        createDevelopmentModeLaunchButton = new Button(hosted, SWT.NULL);
        createDevelopmentModeLaunchButton
                .setText("Create development mode launch");
        createDevelopmentModeLaunchButton.setLayoutData(new GridData(SWT.RIGHT,
                SWT.BEGINNING, true, false));
        createDevelopmentModeLaunchButton
                .addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        VaadinPluginUtil.createHostedModeLaunch(project);
                        // need to set as dirty for Vaadin 6.2 and earlier (need
                        // to recompile with OOPHM)
                        WidgetsetUtil.setWidgetsetDirty(project, true);
                    }
                });
    }

    /**
     * Gets the user-selected GWT compilation style. Default is "OBF".
     * 
     * @return "OBF"/"PRETTY"/"DETAILED" - never null
     */
    public String getCompilationStyle() {
        String text = styleCombo.getText();
        if (DETAILED_LABEL.equals(text)) {
            return "DETAILED";
        } else if (PRETTY_LABEL.equals(text)) {
            return "PRETTY";
        } else if (DRAFT_LABEL.equals(text)) {
            return "DRAFT";
        } else {
            return "OBF";
        }
    }

    /**
     * Gets the user-selected number of GWT compiler threads. Default is no
     * selection (empty string).
     * 
     * @return String containing a positive number or empty string if none
     *         specified, not null
     */
    public String getParallelism() {
        return parallelismCombo.getText();
    }

    /**
     * Returns whether automatic widgetset build requests should be suspended.
     * 
     * @return true to disable widgetset build requests when making changes or
     *         e.g. publishing the project
     */
    public boolean areWidgetsetBuildsSuspended() {
        return suspendAutomaticBuilds.getSelection();
    }

    /**
     * Returns whether verbose output of widgetset compilation is used.
     * 
     * @return
     */
    public boolean isVerboseOutput() {
        return verboseCompilation.getSelection();
    }

    /**
     * Returns extra parameters for the widgetset compiler.
     * 
     * @return String extra parameters for widgetset compiler, not null
     */
    public String getExtraParameters() {
        return extraParameters.getText();
    }
    /**
     * Returns extra JVM parameters for the widgetset compiler.
     * 
     * @return String extra JVM parameters for widgetset compiler, not null
     */
    public String getExtraJvmParameters() {
        return extraJvmParameters.getText();
    }
}
