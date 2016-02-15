package com.vaadin.integration.eclipse.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Abstract base class for theme and widgetset compilation handlers
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public abstract class AbstractVaadinCompileHandler extends AbstractHandler {

    protected AbstractVaadinCompileHandler() {
    }

    /**
     * the command has been executed, so extract extract the needed information
     * from the application context.
     */
    public Object execute(final ExecutionEvent event) throws ExecutionException {

        final ISelection currentSelection = HandlerUtil
                .getCurrentSelection(event);
        final IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);

        startCompileJob(currentSelection, activeEditor);

        return null;
    }

    protected abstract void startCompileJob(ISelection currentSelection,
            IEditorPart activeEditor);

}
