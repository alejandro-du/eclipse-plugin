package com.vaadin.integration.eclipse.flow.wizard.ui;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.vaadin.integration.eclipse.flow.wizard.Starter;
import com.vaadin.integration.eclipse.flow.wizard.StarterManager;
import com.vaadin.integration.eclipse.flow.wizard.TechStack;

public class PlatformStarterSelectionComposite extends Composite {

    private static final String SELECTED_STARTER_ID = "project-base";
    private static final String SPRING_STACK_ID = "spring";

    private Text groupIdText;
    private Text projectNameText;
    private ComboViewer starterCombo;
    private Label starterDescriptionLabel;
    private ComboViewer stackCombo;

    private Map<String, Starter> idToStarter;
    private Map<String, String> starterToDesc = new HashMap<String, String>();
    {
        starterToDesc.put("project-base",
                "Starting point to create your own Vaadin Flow application.");
        starterToDesc.put("simple-ui",
                "Fully functional Vaadin 14 Java application highlighting all the core features of Vaadin Flow.");
    }

    public PlatformStarterSelectionComposite(Composite parent) {
        super(parent, SWT.NONE);
        configureLayout();
        createContents();
    }

    public void init() throws IOException {
        setInitialData();
    }

    private void configureLayout() {
        GridLayout gridLayout = new GridLayout(2, false);
        gridLayout.marginLeft = 2;
        gridLayout.horizontalSpacing = 10;
        gridLayout.verticalSpacing = 10;
        setLayout(gridLayout);
    }

    private void createContents() {
        Label groupIdLabel = new Label(this, SWT.NONE);
        groupIdLabel.setText("Group ID:");

        groupIdText = new Text(this, SWT.BORDER);
        groupIdText
                .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label projectNameLabel = new Label(this, SWT.NONE);
        projectNameLabel.setText("Project Name:");

        projectNameText = new Text(this, SWT.BORDER);
        projectNameText
                .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label starterLabel = new Label(this, SWT.NONE);
        starterLabel.setText("Starter:");

        starterCombo = new ComboViewer(this, SWT.READ_ONLY);
        starterCombo.getCombo()
                .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        starterCombo.setContentProvider(new ArrayContentProvider());
        starterCombo.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                return ((Starter) element).getDescription();
            }
        });

        starterCombo
                .addSelectionChangedListener(new ISelectionChangedListener() {
                    @Override
                    public void selectionChanged(SelectionChangedEvent event) {
                        StructuredSelection selection = (StructuredSelection) event
                                .getSelection();
                        Starter selectedStarter = (Starter) selection
                                .getFirstElement();
                        selectStack(selectedStarter);
                        updateDescription(selectedStarter);
                    }
                });

        new Label(this, SWT.NONE);
        starterDescriptionLabel = new Label(this, SWT.NONE);

        Label stackLabel = new Label(this, SWT.NONE);
        stackLabel.setText("Tech stack:");

        stackCombo = new ComboViewer(this, SWT.READ_ONLY);
        stackCombo.getCombo()
                .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        stackCombo.setContentProvider(new ArrayContentProvider());
        stackCombo.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                return ((TechStack) element).getId();
            }
        });
    }

    private void updateDescription(Starter selectedStarter) {
        String description = starterToDesc.get(selectedStarter.getId());
        starterDescriptionLabel.setText(description);
        layout(true);
    }

    private void setInitialData() throws IOException {
        groupIdText.setText(Starter.DEFAULT_GROUP_ID);
        projectNameText.setText(Starter.DEFAULT_PROJECT_NAME);
        if (idToStarter == null) {
            List<Starter> starters = StarterManager
                    .getSupportedStarters(StarterManager.fetchStarters());
            idToStarter = new HashMap<String, Starter>();
            for (Starter starter : starters) {
                idToStarter.put(starter.getId(), starter);
            }
            initCombos(starters);
        }
    }

    private void initCombos(List<Starter> starters) {
        Starter selectedStarter = idToStarter.get(SELECTED_STARTER_ID);
        starterCombo.setInput(starters);
        starterCombo.setSelection(new StructuredSelection(selectedStarter));
        updateDescription(selectedStarter);
        selectStack(selectedStarter);
    }

    private void selectStack(Starter selectedStarter) {
        List<TechStack> techStacks = selectedStarter.getTechStacks();
        TechStack selectedStack = getInitialStack(techStacks);
        stackCombo.setInput(techStacks);
        stackCombo.setSelection(new StructuredSelection(selectedStack));
    }

    private TechStack getInitialStack(List<TechStack> techStacks) {
        for (TechStack stack : techStacks) {
            if (SPRING_STACK_ID.equals(stack.getId())) {
                return stack;
            }
        }
        return techStacks.get(0);
    }

    public String getGroupId() {
        return groupIdText.getText();
    }

    public String getProjectName() {
        return projectNameText.getText();
    }

    public Starter getStarter() {
        StructuredSelection selection = (StructuredSelection) starterCombo
                .getSelection();
        return (Starter) selection.getFirstElement();
    }

    public TechStack getStack() {
        StructuredSelection selection = (StructuredSelection) stackCombo
                .getSelection();
        return (TechStack) selection.getFirstElement();
    }
}
