package com.gentics.mesh.plugin.mustread.initializer;

import com.gentics.mesh.plugin.mustread.config.MustReadConfig;
import com.gentics.mesh.plugin.mustread.config.ProjectConfig;
import com.gentics.mesh.plugin.mustread.controller.NodeController;
import com.gentics.mesh.plugin.mustread.helper.LogHelper;
import com.gentics.mesh.rest.client.MeshRestClient;
import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class FolderInitializer extends BaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(FolderInitializer.class);

    private NodeController nodeController;

    public FolderInitializer(MeshRestClient client, MustReadConfig config, LogHelper logHelper, NodeController nodeController) {
        super(client, config, logHelper);

        this.nodeController = nodeController;
    }

    public Completable initialize() {
        Set<Completable> results = new HashSet<>();

        for (ProjectConfig project : getConfig().getProjects()) {
            String folderName = project.getFolderName();
            String projectName = project.getName();

            log.info(getLogHelper().generateLogMessage(projectName, String.format("Initializing folder '%s'.", folderName)));
            results.add(nodeController.createPluginFolderForProject(projectName, folderName));
        }

        return Completable.merge(results);
    }
}
