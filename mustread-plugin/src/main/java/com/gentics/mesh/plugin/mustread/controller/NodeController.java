package com.gentics.mesh.plugin.mustread.controller;

import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.plugin.mustread.exception.PluginPathNotFoundException;
import com.gentics.mesh.plugin.mustread.helper.LogHelper;
import com.gentics.mesh.plugin.mustread.object.User;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang.StringUtils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(NodeController.class);

    private SchemaController schemaController;

    private Map<String, NodeResponse> pluginFolders = new HashMap<>();


    public NodeController(MeshRestClient client, LogHelper logHelper, SchemaController schemaController) {
        super(client, logHelper);

        this.schemaController = schemaController;
    }

    public Completable createPluginFolderForProject(String projectName, String pluginFolderName) {
        return Completable.defer(() -> {
            NodeCreateRequest request = new NodeCreateRequest();
            FieldMap fields = new FieldMapImpl();

            fields.put(SchemaController.folderSchemaFieldName, new StringFieldImpl().setString(pluginFolderName));

            request.setSchema(schemaController.getFolderSchemaReference());
            request.setFields(fields);
            request.setLanguage("en");

            if (pluginFolderName.isEmpty()) {
                log.info(getLogHelper().generateLogMessage(projectName, String.format("Folder '%s' empty therefor nothing is created.", pluginFolderName)));

                return Completable.complete();
            }

            return createPathInProject(projectName, pluginFolderName, request)
                    .doOnSuccess((response -> {
                        pluginFolders.put(projectName, response);
                    })).ignoreElement();
        });
    }

    public Completable createUserFolderForProject(String projectName, User user) {
        return Completable.defer(() -> {
            NodeCreateRequest request = new NodeCreateRequest();
            FieldMap fields = new FieldMapImpl();

            fields.put(SchemaController.userSchemaFieldUserId, new StringFieldImpl().setString(user.getUUID()));

            request.setSchema(schemaController.getUserSchemaReference());
            request.setFields(fields);
            request.setLanguage("en");

            String path = generateUserReadFolder(projectName, user);

            return createPathInProject(projectName, path, request)
                    .ignoreElement();
        });
    }

    public Completable createReadNodeForProjectAndUser(String projectName, User user, String nodeId) {
        return Completable.defer(() -> {
            NodeCreateRequest request = new NodeCreateRequest();
            FieldMap fields = new FieldMapImpl();

            fields.put(SchemaController.nodeSchemaFieldNodeId, new StringFieldImpl().setString(nodeId));
            fields.put(SchemaController.nodeSchemaFieldTimestamp, new DateFieldImpl().setDate(ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT)));

            request.setSchema(schemaController.getNodeSchemaReference());
            request.setFields(fields);
            request.setLanguage("en");

            String path = String.format("%s/%s", generateUserReadFolder(projectName, user), nodeId);

            return createPathInProject(projectName, path, request)
                    .ignoreElement();
        });
    }

    private Single<NodeResponse> createPathInProject(String projectName, String path, NodeCreateRequest request) {
        // We first check if the path already exists and we only react to 404 error which means that the path
        // doesn't exist. Everything else is either an error or fine (folder exists)
        return getClient()
                .webroot(projectName, "/" + path)
                .toSingle()
                .map(response -> {
                    log.info(getLogHelper().generateLogMessage(projectName, String.format("Path '%s' already exists.", path)));
                    return response.getNodeResponse();
                })
                .onErrorResumeNext(error1 -> {
                    if (error1 instanceof MeshRestClientMessageException && ((MeshRestClientMessageException) error1).getStatusCode() == 404) {
                        // We create the folder because it doesn't exist
                        return getClient()
                                .webrootCreate(projectName, "/" + path, request)
                                .toSingle()
                                .doOnSuccess(response -> {
                                    log.info(getLogHelper().generateLogMessage(projectName, String.format("Created path '%s'", convertBreadcrumbToPath(response.getBreadcrumb()))));
                                }).doOnError(error2 -> {
                                    log.error(getLogHelper().generateLogMessage(projectName, String.format("Failed creating path '%s'", path)));
                                    error2.printStackTrace();
                                });
                    } else {
                        return Single.error(error1);
                    }
                });
    }

    private String convertBreadcrumbToPath(List<NodeReference> breadcrumbs) {
        List<String> parts = new ArrayList<>();
        for (NodeReference reference : breadcrumbs) {
            parts.add(reference.getDisplayName());
        }

        return StringUtils.join(parts, "/");
    }

    public String generateUserReadFolder(String projectName, User user) throws PluginPathNotFoundException {
        NodeResponse pluginFolder = pluginFolders.get(projectName);

        if (pluginFolder == null) {
            throw new PluginPathNotFoundException(getLogHelper().generateLogMessage(projectName, "Plugin folder not found stopping creation of UserFolder"));
        } else {
            String path = convertBreadcrumbToPath(pluginFolder.getBreadcrumb());

            return String.format("%s/%s", path, user.getUUID());
        }
    }
}
