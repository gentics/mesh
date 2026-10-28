package com.gentics.mesh.plugin.mustread.consumer;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.plugin.mustread.MustReadPlugin;
import com.gentics.mesh.plugin.mustread.config.MustReadConfig;
import com.gentics.mesh.plugin.mustread.config.ProjectConfig;
import com.gentics.mesh.plugin.mustread.controller.NodeController;
import com.gentics.mesh.plugin.mustread.controller.SchemaController;
import com.gentics.mesh.plugin.mustread.helper.LogHelper;
import com.gentics.mesh.rest.client.MeshRestClient;
import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ProjectCreateConsumer extends BaseConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProjectCreateConsumer.class);

    private SchemaController schemaController;

    private NodeController nodeController;

    public ProjectCreateConsumer(MustReadPlugin plugin, MeshRestClient client, LogHelper logHelper, MustReadConfig config, SchemaController schemaController, NodeController nodeController) {
        super(plugin, client, logHelper, config);

        this.schemaController = schemaController;
        this.nodeController = nodeController;
    }

    @Override
    protected void consume(JsonObject object) {
        List<Completable> completables = new ArrayList<>();
        String projectName = object.getString("name");
        ProjectConfig project = getConfiguration().getProjectConfiguration(projectName);

        if (project != null) {
            List<Completable> current_completables = new ArrayList<>();
            // First we assign the schemas to the project
            for (SchemaResponse schema : schemaController.getCreatedSchemas()) {
                current_completables.add(schemaController.assignSchemaToProject(project.getName(), schema));
            }

            // then we create the plugin main folder inside of the project
            completables.add(Completable
                    .merge(current_completables)
                    .andThen(nodeController.createPluginFolderForProject(project.getName(), project.getFolderName()))
            );
        }

        Completable.merge(completables).subscribe();
    }


    public void register() {
        super.register(MeshEvent.PROJECT_CREATED);
    }
}
