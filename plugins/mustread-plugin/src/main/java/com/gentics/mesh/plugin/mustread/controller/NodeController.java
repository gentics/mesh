package com.gentics.mesh.plugin.mustread.controller;

import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.plugin.mustread.exception.PluginPathNotFoundException;
import com.gentics.mesh.plugin.mustread.helper.LogHelper;
import com.gentics.mesh.plugin.mustread.object.User;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.rest.client.MeshWebrootResponse;

import io.reactivex.Completable;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang.StringUtils;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
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

    /**
     * Creates the main plugin folder in the project if it is configured and doesn't exist
     *
     * @param projectName      Name of the project
     * @param pluginFolderName folder plugin folder inside of the project
     * @return
     */
    public Completable createPluginFolderForProject(String projectName, String pluginFolderName) {
        return Completable.defer(() -> {
            NodeCreateRequest request = new NodeCreateRequest();
            FieldMap fields = new FieldMapImpl();

            fields.put(SchemaController.folderSchemaFieldName, new StringFieldImpl().setString(pluginFolderName));

            request.setSchema(schemaController.getFolderSchemaReference());
            request.setFields(fields);
            request.setLanguage("en");

            if (StringUtils.isBlank(pluginFolderName)) {
                log.info(getLogHelper().generateLogMessage(projectName, String.format("Configured Folder '%s' is blank, therefor none will be created", pluginFolderName)));

                return Completable.complete();
            }

            return createPathInProject(projectName, pluginFolderName, request)
                    .doOnSuccess(response -> {
                        pluginFolders.put(projectName, response);
                    }).ignoreElement();
        });
    }

    /**
     * Creates the folder which stores the read uuids, for the provides user if it doesn't exist already
     *
     * @param projectName
     * @param user
     * @return
     */
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

    /**
     * Creates the node identifying that the nodeId has already been read by the user
     *
     * @param projectName
     * @param user
     * @param nodeId
     *
     * @return
     */
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

    /**
     * Creates a path inside of the project
     *
     * @param projectName
     * @param path
     * @param request
     *
     * @return
     */
    private Single<NodeResponse> createPathInProject(String projectName, String path, NodeCreateRequest request) {
        // We first check if the path already exists and we only react to 404 error which means that the path
        // doesn't exist. Everything else is either an error or fine (folder exists)
        ParameterProvider parameters = new NodeParametersImpl().setResolveLinks(LinkType.MEDIUM);
        return getClient()
                .webroot(projectName, "/" + path, parameters)
                .toSingle()
                .doAfterSuccess(MeshWebrootResponse::close)
                .map(response -> {
                    log.info(getLogHelper().generateLogMessage(projectName, String.format("Path '%s' already exists.", stringProjectName(response.getNodeResponse().getPath()))));
                    return response.getNodeResponse();
                })
                .onErrorResumeNext(error1 -> {
                    if (error1 instanceof MeshRestClientMessageException && ((MeshRestClientMessageException) error1).getStatusCode() == 404) {
                        // We create the folder because it doesn't exist
                        return getClient()
                                .webrootCreate(projectName, "/" + path, request, parameters)
                                .toSingle()
                                .doOnSuccess(response -> {
                                    log.info(getLogHelper().generateLogMessage(projectName, String.format("Created path '%s' successfully", stringProjectName(response.getPath()))));
                                }).doOnError(error2 -> {
                                    log.error(getLogHelper().generateLogMessage(projectName, String.format("Failed creating path '%s'", path)), error2);
                                });
                    } else {
                        return Single.error(error1);
                    }
                });
    }

    /**
     * Generates the name for the folder containing the read nodes for the provided user
     *
     * @param projectName
     * @param user
     * @return
     * @throws PluginPathNotFoundException
     */
    public String generateUserReadFolder(String projectName, User user) throws PluginPathNotFoundException {
        NodeResponse pluginFolder = pluginFolders.get(projectName);

        if (pluginFolder == null) {
            throw new PluginPathNotFoundException(getLogHelper().generateLogMessage(projectName, "Plugin folder not found stopping creation of UserFolder"));
        } else {
            return String.format("%s/%s", stringProjectName(pluginFolder.getPath()), user.getUUID());
        }
    }

    /**
     * Strips out the projectname of the url so that we can use the provided url in a webroot request
     *
     * @param url
     * @return
     */
    private String stringProjectName(String url) {
        List<String> workurl = Arrays.asList(url.split("/"));

        return String.join("/", workurl.subList(2, workurl.size()));
    }
}
