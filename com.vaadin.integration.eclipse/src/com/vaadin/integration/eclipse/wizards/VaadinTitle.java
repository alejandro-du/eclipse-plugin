package com.vaadin.integration.eclipse.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

/*
 * Handle the wizard page change to set the vaadin title colors.
 */
public class VaadinTitle implements IPageChangedListener {

    /*
     * List of controls to switch color from blue/white to default.
     */
    private List<Control> colorControls;

    /*
     * Wizard container displaying our Vaadin wizard. This is a WizardDialog
     * instance.
     */
    private IWizardContainer wizardContainer;

    public VaadinTitle(IPageChangeProvider pageChangeProvider,
            IWizardContainer wizardContainer) {
        pageChangeProvider.addPageChangedListener(this);

        this.wizardContainer = wizardContainer;
    }

    /*
     * Initialize the color controls.
     */
    private void ensureColorControls() {
        if (colorControls == null) {
            colorControls = new ArrayList<Control>();

            Control control = wizardContainer.getShell().getChildren()[0];

            // Add the composite containing the title labels and all the
            // other main composites of the WizardDialog.
            colorControls.add(control);

            // Add all controls which are not composites, so only Labels
            // mainly.
            Composite composite = (Composite) control;
            Control[] children = composite.getChildren();
            for (Control child : children) {
                if (!(child instanceof Composite)) {
                    colorControls.add(child);
                }
            }
        }
    }

    /*
     * Sets the vaadin style.
     */
    private void setVaadinStyle() {

        // It's enough to ensure this here, instead also on the default
        // style, because there is no point of setting the default one if
        // vaadin style was not previously set.
        ensureColorControls();

        Display display = getDisplay();

        if (display != null) {
            Color background = new Color(display, new RGB(0, 164, 240));
            Color foreground = new Color(display, new RGB(255, 255, 255));

            setColors(background, foreground);
        }
    }

    /*
     * Sets the default style.
     */
    private void setDefaultStyle() {
        Display display = getDisplay();

        if (display != null) {
            Color background = JFaceColors.getBannerBackground(display);
            Color foreground = JFaceColors.getBannerForeground(display);

            setColors(background, foreground);
        }
    }

    /*
     * Set the colors on the colected components representing the title.
     */
    private void setColors(Color background, Color foreground) {
        for (Control colorControl : colorControls) {
            setColors(colorControl, background, foreground);
        }
    }

    /**
     * Sets the background and foreground colors for the specified control.
     *
     * @param control
     *            the control to set the colors to.
     * @param background
     *            the background color.
     * @param foreground
     *            the foreground color.
     */
    public static void setColors(Control control, Color background,
            Color foreground) {
        control.setBackground(background);
        control.setForeground(foreground);
    }

    /*
     * Gets the correct display to generate the color.
     */
    private Display getDisplay() {
        if (colorControls != null && colorControls.size() > 0) {
            return colorControls.get(0).getDisplay();
        } else {
            return null;
        }
    }

    /**
     * Destroy the vaadin title handler.
     */
    public void destroy() {
        setDefaultStyle();
    }

    public void pageChanged(PageChangedEvent event) {
        Object selectedPage = event.getSelectedPage();
        if (selectedPage.getClass().getName().startsWith("com.vaadin")) {
            setVaadinStyle();
        } else {
            setDefaultStyle();
        }
    }

}
