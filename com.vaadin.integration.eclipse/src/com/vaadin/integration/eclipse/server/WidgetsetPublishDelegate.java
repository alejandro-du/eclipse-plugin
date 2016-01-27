package com.vaadin.integration.eclipse.server;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.PublishOperation;
import org.eclipse.wst.server.core.model.PublishTaskDelegate;

import com.vaadin.integration.eclipse.VaadinFacetUtils;
import com.vaadin.integration.eclipse.util.WidgetsetUtil;

/**
 * A delegate that is called when publishing a project to a server, and asks
 * about recompilation of widgetsets.
 * 
 * TODO publishing tab of server: turning off widgetset compilation question? -
 * server type specific?
 */
public class WidgetsetPublishDelegate extends PublishTaskDelegate {

    @SuppressWarnings("rawtypes")
    @Override
    public PublishOperation[] getTasks(IServer server, int kind, List modules,
            List kindList) {
        if (modules == null || modules.size() == 0) {
            return null;
        }

        // keep order, even if not really necessary
        Set<IProject> vaadinProjects = new LinkedHashSet<IProject>();

        for (int i = 0; i < modules.size(); i++) {
            IModule[] moduleArray = (IModule[]) modules.get(i);

            // state is PUBLISH_STATE_NONE here if only the GWT module has
            // changed, so cannot filter much

            // int state = server.getModulePublishState(moduleArray);
            // if (state != IServer.PUBLISH_STATE_NONE
            // || kind == IServer.PUBLISH_CLEAN) {
            // Integer moduleKind = (Integer) kindList.get(i);
            // if (moduleKind != ServerBehaviourDelegate.NO_CHANGE
            // || kind == IServer.PUBLISH_CLEAN) {
            for (IModule module : moduleArray) {
                IProject p = module.getProject();
                if (p != null && VaadinFacetUtils.isVaadinProject(p)) {
                    vaadinProjects.add(p);
                }
            }
            // }
            // }
        }

        List<PublishOperation> tasks = new ArrayList<PublishOperation>();
        for (IProject project : vaadinProjects) {
            // check if project widgetset is dirty (not compiled)
            if (WidgetsetUtil.isWidgetsetDirty(project)) {
                tasks.add(new WidgetsetPublishTask(project));
            }
        }
        return tasks.toArray(new PublishOperation[tasks.size()]);
    }
}
