package com.gentics.mesh.plugin.mustread.initializer;

import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.plugin.mustread.config.MustReadConfig;
import com.gentics.mesh.plugin.mustread.config.ProjectConfig;
import com.gentics.mesh.plugin.mustread.controller.NodeController;
import com.gentics.mesh.plugin.mustread.helper.LogHelper;
import com.gentics.mesh.rest.client.MeshRestClient;
import io.reactivex.Completable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class FolderInitializer extends BaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(FolderInitializer.class);

    private NodeController nodeController;

    public FolderInitializer(MeshRestClient client, MustReadConfig config, LogHelper logHelper, NodeController nodeController) {
        super(client, config, logHelper);

        this.nodeController = nodeController;
    }

    public Completable initialize() {
        return Completable.defer(() -> {
            Set<Completable> completables = new HashSet<>();

            // We run through our projects
            for (ProjectConfig project : getConfig().getProjects()) {
                String folderName = project.getFolderName();
                String projectName = project.getName();

                completables.add(getClient()
                        .findProjects()
                        .toSingle()
                        .flatMapCompletable(list -> {
                            Optional<ProjectResponse> projects = list.getData().stream().filter(s -> s.getName().equals(project.getName())).findFirst();

                            if (projects.isPresent()) { // If the project exists we directly assign the schema
                                log.info(getLogHelper().generateLogMessage(projectName, String.format("Initializing folder '%s'.", folderName)));
                                return nodeController.createPluginFolderForProject(projectName, folderName);
                            }

                            return Completable.complete();
                        })
                );

            }

            return Completable.merge(completables);
        });
    }
}
