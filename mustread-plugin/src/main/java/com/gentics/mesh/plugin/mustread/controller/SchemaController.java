package com.gentics.mesh.plugin.mustread.controller;

import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.*;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.mustread.helper.LogHelper;
import com.gentics.mesh.rest.client.MeshRestClient;
import io.reactivex.Completable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SchemaController extends BaseController {

    public static final String folderSchemaName = "must_read_folder";
    public static final String folderSchemaFieldName = "name";
    public static final String userSchemaName = "must_read_user";
    public static final String userSchemaFieldUserId = "user_id";
    public static final String nodeSchemaName = "must_read_node";
    public static final String nodeSchemaFieldNodeId = "node_id";
    public static final String nodeSchemaFieldTimestamp = "timestamp";
    private static final Logger log = LoggerFactory.getLogger(SchemaController.class);
    private Map<String, SchemaResponse> createdSchemas = new HashMap<>();

    public SchemaController(MeshRestClient client, LogHelper logHelper) {
        super(client, logHelper);
    }

    private SchemaCreateRequest getFolderSchemaCreateRequest() {

        SchemaCreateRequest request = new SchemaCreateRequest();
        List<FieldSchema> fields = new ArrayList<>();

        fields.add(new StringFieldSchemaImpl().setName(folderSchemaFieldName).setLabel("Name of the Must Read Folder").setRequired(true));

        request.setName(folderSchemaName);
        request.setFields(fields);
        request.setContainer(true);
        request.setDescription("Folder Schema to store read nodes for the must read plugin");
        request.setDisplayField(folderSchemaFieldName);
        request.setSegmentField(folderSchemaFieldName);

        return request;
    }

    private SchemaCreateRequest getUserSchemaCreateRequest() {
        SchemaCreateRequest request = new SchemaCreateRequest();
        List<FieldSchema> fields = new ArrayList<>();

        fields.add(new StringFieldSchemaImpl().setName(userSchemaFieldUserId).setLabel("UserId of who read a node").setRequired(true));

        request.setName(userSchemaName);
        request.setFields(fields);
        request.setContainer(true);
        request.setDescription("Folder Schema to store users who read nodes for the must read plugin");
        request.setDisplayField(userSchemaFieldUserId);
        request.setSegmentField(userSchemaFieldUserId);

        return request;
    }

    private SchemaCreateRequest getNodeSchemaCreateRequest() {
        SchemaCreateRequest request = new SchemaCreateRequest();
        List<FieldSchema> fields = new ArrayList<>();

        fields.add(new StringFieldSchemaImpl().setName(nodeSchemaFieldNodeId).setLabel("NodeId of the read node").setRequired(true));
        fields.add(new DateFieldSchemaImpl().setName(nodeSchemaFieldTimestamp).setLabel("Timestamp when the node was read").setRequired(true));

        request.setName(nodeSchemaName);
        request.setFields(fields);
        request.setContainer(false);
        request.setDescription("Node schema to store the a read node");
        request.setDisplayField(nodeSchemaFieldNodeId);
        request.setSegmentField(nodeSchemaFieldNodeId);

        return request;
    }

    private Completable createOrUpdateSchema(String schemaName, SchemaCreateRequest request) {
        return getClient()
                .findSchemas()
                .toSingle()
                .flatMap(list -> {
                    SchemaResponse schema = extractSchema(list, schemaName);
                    if (schema != null) {
                        log.info(getLogHelper().generateLogMessage("", String.format("Updating schema %s", schema.getName())));

                        SchemaUpdateRequest updateRequest = JsonUtil.readValue(request.toJson(), SchemaUpdateRequest.class);
                        return getClient()
                                .updateSchema(schema.getUuid(), updateRequest)
                                .toCompletable()
                                .andThen(getClient()
                                        .findSchemaByUuid(schema.getUuid())
                                        .toSingle()
                                );
                    } else {
                        log.info(getLogHelper().generateLogMessage("", String.format("Creating schema %s", schemaName)));
                        return getClient().createSchema(request).toSingle();

                    }
                }).doOnSuccess(schema -> {
                    createdSchemas.put(schema.getName(), schema);
                    log.info(getLogHelper().generateLogMessage("", String.format("Schema %s created/updated successfully", schema.getName())));
                }).doOnError(error -> {
                    log.info(getLogHelper().generateLogMessage("", String.format("Error finding schema %s", schemaName)), error);
                }).ignoreElement();
    }

    public Completable createOrUpdateFolderSchema() {
        return createOrUpdateSchema(folderSchemaName, getFolderSchemaCreateRequest());
    }

    public Completable createOrUpdateUserSchema() {
        return createOrUpdateSchema(userSchemaName, getUserSchemaCreateRequest());
    }

    public Completable createOrUpdateNodeSchema() {
        return createOrUpdateSchema(nodeSchemaName, getNodeSchemaCreateRequest());
    }

    /**
     * Filters out if the provided SchemaName is inside of the SchemaListResponse and
     * returns the SchemaResponse for it
     *
     * @param list
     * @param schemaName
     * @return
     */
    private SchemaResponse extractSchema(SchemaListResponse list, String schemaName) {
        Optional<SchemaResponse> schemas = list.getData().stream().filter(s -> s.getName().equals(schemaName)).findFirst();

        return schemas.orElse(null);
    }

    public Completable assignSchemaToProject(String projectName, String schemaName) {
        SchemaResponse schema = createdSchemas.get(schemaName);

        return assignSchemaToProject(projectName, schema);
    }

    public Completable assignSchemaToProject(String projectName, SchemaResponse schema) {
        log.info(getLogHelper().generateLogMessage(projectName, String.format("Start Assigning Schema '%s'", schema.getName())));
        return getClient().assignSchemaToProject(projectName, schema.getUuid())
                .toCompletable()
                .doOnComplete(() -> {
                    log.info(getLogHelper().generateLogMessage(projectName, String.format("Assigned Schema '%s' successfully", schema.getName())));
                }).doOnError((error) -> {
                    log.error(getLogHelper().generateLogMessage(projectName, String.format("Failed to assign '%s'", schema.getName())), error);
                });
    }

    /**
     * Returns a list of all created schemas by this controller
     *
     * @return
     */
    public Collection<SchemaResponse> getCreatedSchemas() {
        return createdSchemas.values();
    }

    SchemaReference getFolderSchemaReference() {
        return createdSchemas.get(folderSchemaName).toReference();
    }

    SchemaReference getUserSchemaReference() {
        return createdSchemas.get(userSchemaName).toReference();
    }

    SchemaReference getNodeSchemaReference() {
        return createdSchemas.get(nodeSchemaName).toReference();
    }
}
