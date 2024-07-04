package com.gentics.mesh.plugin.mustread.initializer;

import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.plugin.mustread.config.MustReadConfig;
import com.gentics.mesh.plugin.mustread.config.ProjectConfig;
import com.gentics.mesh.plugin.mustread.helper.LogHelper;
import com.gentics.mesh.rest.client.MeshRestClient;
import io.reactivex.Completable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SchemaInitializer extends BaseInitializer {

    private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

    private com.gentics.mesh.plugin.mustread.controller.SchemaController schemaController;

    public SchemaInitializer(MeshRestClient client, MustReadConfig config, LogHelper logHelper, com.gentics.mesh.plugin.mustread.controller.SchemaController schemaController) {
        super(client, config, logHelper);

        this.schemaController = schemaController;
    }


    private Completable assignSchemasToProjects() {
        return Completable.defer(() -> {
            List<Completable> completables = new ArrayList<>();
            // We run through our projects
            for (ProjectConfig project : getConfig().getProjects()) {
                for (SchemaResponse schema : schemaController.getCreatedSchemas()) {
                    completables.add(getClient()
                            .findProjects()
                            .toSingle()
                            .flatMapCompletable(list -> {
                                Optional<ProjectResponse> projects = list.getData().stream().filter(s -> s.getName().equals(project.getName())).findFirst();


                                if (projects.isPresent()) { // If the project exists we directly assign the schema
                                    log.info(getLogHelper().generateLogMessage(project.getName(), String.format("Start assigning schema '%s'.", schema.getName())));
                                    return schemaController.assignSchemaToProject(project.getName(), schema);
                                }

                                log.info(getLogHelper().generateLogMessage(project.getName(), String.format("Ignoring '%s'.", schema.getName())));
                                return Completable.complete();
                            })
                    );
                }
            }
            return Completable.merge(completables);
        });


    }

    @Override
    public Completable initialize() {
        List<Completable> completables = new ArrayList<>();

        completables.add(schemaController.createOrUpdateFolderSchema());
        completables.add(schemaController.createOrUpdateUserSchema());
        completables.add(schemaController.createOrUpdateNodeSchema());

        return Completable.merge(completables).andThen(assignSchemasToProjects());
    }

}
