package com.vaadin.integration.eclipse.wizards;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

import com.vaadin.integration.eclipse.wizards.Vaadin7MavenProjectWizard.VaadinArchetype;

public class Vaadin7MavenProjectArchetypeSelectionView extends
        ScrolledComposite {

    private final Composite archetypesComposite;
    private final List<Button> vaadinArchetypeButtons;

    public Vaadin7MavenProjectArchetypeSelectionView(
            List<VaadinArchetype> vaadinArchetypes, Composite parent, int style) {
        super(parent, SWT.V_SCROLL | style);
        
        this.vaadinArchetypeButtons = new ArrayList<Button>(
                vaadinArchetypes.size());

        archetypesComposite = createContents(vaadinArchetypes);
        setContent(archetypesComposite);
        setExpandHorizontal(true);
        setExpandVertical(true);

        // this default selection should be done by the wizard
        selectVaadinArchetype(vaadinArchetypes.get(0));
    }

    private Composite createContents(List<VaadinArchetype> vaadinArchetypes) {
        setLayout(new FillLayout());

        final Composite main = new Composite(this, SWT.NONE );
        main.setLayout(new GridLayout(1, false));
        int stdHeight = main.getFont().getFontData()[0].getHeight();

        for (VaadinArchetype vaadinArch : vaadinArchetypes) {
            final Button btnArchetype = new Button(main, SWT.RADIO | SWT.WRAP
                    | SWT.TOP);

            btnArchetype.setText(vaadinArch.getTitle());
            btnArchetype.setData(vaadinArch);
            GridData btnGridData = new GridData(SWT.DEFAULT, SWT.DEFAULT, true,
                    false, 1, 1);
            btnGridData.horizontalIndent = stdHeight * 1;
            if (!vaadinArchetypeButtons.isEmpty())
                btnGridData.verticalIndent = stdHeight * 2;
            btnArchetype.setLayoutData(btnGridData);
            vaadinArchetypeButtons.add(btnArchetype);

            MouseListener btnActivateListener = new MouseAdapter() {

                public void mouseDown(MouseEvent arg0) {
                    btnArchetype.setFocus();
                    btnArchetype.setSelection(true);

                }
            };

            for (StringTokenizer st = new StringTokenizer(
                    vaadinArch.getDescription(), "\n"); st.hasMoreTokens();) {
                GridData labelGridData = new GridData(SWT.DEFAULT, SWT.DEFAULT,
                        true, false, 1, 1);
                labelGridData.horizontalIndent = stdHeight * 5;
                labelGridData.verticalIndent = (int) (stdHeight * 0.8);

                Label descriptionLine = new Label(main, SWT.WRAP);
                descriptionLine.setLayoutData(labelGridData);
                String string = st.nextToken();
                descriptionLine.setText(string);
                descriptionLine.addMouseListener(btnActivateListener);
            }
        }
        main.addListener(SWT.Resize, new Listener() {
            int width = -1;

            public void handleEvent(Event e) {
                int newWidth = main.getSize().x;
                if (newWidth != width) {
                    Vaadin7MavenProjectArchetypeSelectionView.this
                            .setMinHeight(main.computeSize(newWidth,
                                    SWT.DEFAULT).y);
                    width = newWidth;
                }
            }
        });

        return main;
    }

    public VaadinArchetype getVaadinArchetype() {
        for (Button btn : vaadinArchetypeButtons) {
            if (btn.getSelection())
                return (VaadinArchetype) btn.getData();
        }
        return (VaadinArchetype) vaadinArchetypeButtons.get(0).getData();
    }

    public void selectVaadinArchetype(VaadinArchetype archetype) {
        for (Button btn : vaadinArchetypeButtons) {
            btn.setSelection(archetype == btn.getData());
        }
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }

}
