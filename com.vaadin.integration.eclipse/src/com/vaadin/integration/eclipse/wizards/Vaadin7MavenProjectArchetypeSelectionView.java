package com.vaadin.integration.eclipse.wizards;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.vaadin.integration.eclipse.wizards.Vaadin7MavenProjectWizard.VaadinArchetype;

public class Vaadin7MavenProjectArchetypeSelectionView extends Composite {

    private final List<VaadinArchetype> vaadinArchetypes;
    private final ArchetypeSelectionCallback callback;

    public Vaadin7MavenProjectArchetypeSelectionView(
            ArchetypeSelectionCallback callback,
            List<VaadinArchetype> vaadinArchetypes, Composite parent, int style) {
        super(parent, SWT.NONE);

        this.callback = callback;
        this.vaadinArchetypes = vaadinArchetypes;

        createContents(parent);
    }

    private void createContents(Composite parent) {
        setLayout(new GridLayout(1, false));

        // TODO there should be more standard ways to do selection in Eclipse

        ScrolledComposite scrolledComposite = new ScrolledComposite(this,
                SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        Composite main = new Composite(scrolledComposite, SWT.NONE);
        main.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        main.setLayout(new GridLayout(1, false));

        scrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
                true, 1, 1));
        scrolledComposite.setSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        scrolledComposite.setExpandHorizontal(true);
        scrolledComposite.setExpandVertical(true);
        scrolledComposite.setContent(main);

        addMouseListener(new MouseClickHandler(this, -1));

        int i = 0;
        for (VaadinArchetype vaadinArch : vaadinArchetypes) {
            MouseClickHandler mouseClickHandler = new MouseClickHandler(this,
                    i++);

            Composite composite = new Composite(main, SWT.NONE);
            composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
                    false, 1, 1));
            composite.setBackground(parent.getDisplay().getSystemColor(
                    SWT.COLOR_WHITE));
            GridLayout glComposite = new GridLayout(1, false);
            glComposite.marginLeft = 32;
            composite.setLayout(glComposite);
            composite.addMouseListener(mouseClickHandler);

            Label lblTitle = new Label(composite, SWT.NONE);
            lblTitle.setText(vaadinArch.getTitle());
            // TODO use default font for now - otherwise, need to update the
            // code below and handle disposal of font
            // lblTitle.setFont(SWTResourceManager.getFont(
            // ".Helvetica Neue DeskInterface", 16, SWT.BOLD));
            lblTitle.addMouseListener(mouseClickHandler);

            Label lblDescription = new Label(composite, SWT.WRAP);
            GridData lblDescriptionData = new GridData(SWT.FILL, SWT.CENTER,
                    true, false, 1, 1);
            lblDescriptionData.widthHint = 300;
            lblDescription.setLayoutData(lblDescriptionData);
            lblDescription.setText(vaadinArch.getDescription());
            lblDescription.addMouseListener(mouseClickHandler);
        }
    }

    private void selectVaadinArchetype(int index) {
        callback.onArchetypeSelect(vaadinArchetypes.get(index));
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

    private static class MouseClickHandler extends MouseAdapter {

        private static int downItem = -1;

        private Vaadin7MavenProjectArchetypeSelectionView view;
        private int currentItem;

        public MouseClickHandler(
                Vaadin7MavenProjectArchetypeSelectionView view, int currentItem) {
            this.view = view;
            this.currentItem = currentItem;
        }

        @Override
        public void mouseDown(MouseEvent e) {
            downItem = currentItem;
        }

        @Override
        public void mouseUp(MouseEvent e) {
            if (downItem == currentItem && currentItem > -1) {
                view.selectVaadinArchetype(currentItem);
            } else {
                downItem = -1;
            }
        }
    }
}
