package com.gentics.mesh.plugin.mustread;

import static com.gentics.vertx.openapi.misc.UtilsAndConstants.APPLICATION_JSON;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.plugin.AbstractCommercialRestPlugin;
import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.plugin.common.MeshPluginException;
import com.gentics.mesh.plugin.common.license.LicenseService;
import com.gentics.mesh.plugin.common.license.LicensedPlugin;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.mustread.config.FieldNamesConfig;
import com.gentics.mesh.plugin.mustread.config.MustReadConfig;
import com.gentics.mesh.plugin.mustread.config.ProjectConfig;
import com.gentics.mesh.plugin.mustread.config.QueryFilesConfig;
import com.gentics.mesh.plugin.mustread.consumer.ProjectCreateConsumer;
import com.gentics.mesh.plugin.mustread.controller.NodeController;
import com.gentics.mesh.plugin.mustread.controller.SchemaController;
import com.gentics.mesh.plugin.mustread.handler.MakeReadHandler;
import com.gentics.mesh.plugin.mustread.handler.UnreadHandler;
import com.gentics.mesh.plugin.mustread.helper.LogHelper;
import com.gentics.mesh.plugin.mustread.initializer.FolderInitializer;
import com.gentics.mesh.plugin.mustread.initializer.SchemaInitializer;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.vertx.openapi.model.MessageResponse;
import com.gentics.vertx.openapi.route.InternalEndpointBuilder;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

public class MustReadPlugin extends AbstractCommercialRestPlugin<MustReadConfig> implements RestPlugin {

    private static final Logger log = LoggerFactory.getLogger(MustReadPlugin.class);

    private MustReadConfig configuration;

    private LogHelper logHelper;

    private SchemaController schemaController;

    private NodeController nodeController;

    private ProjectCreateConsumer projectCreateConsumer;

    private MeshRestClient fakeAdminClient;

    public MustReadPlugin(PluginWrapper wrapper, PluginEnvironment env) {
        super(wrapper, env);
    }

    @Override
    public Completable initialize() {
        try {
            getProjectCreateConsumer().register();

            MustReadConfig configuration = getConfiguration();
            configuration.overrideWithEnv();

            LicenseService licenseService = new LicenseService(configuration);
            licenseService.isLicenseSufficient(LicensedPlugin.MMRD);
            // FolderInitializer after SchemaInitializer because folder is based on schema
            SchemaInitializer schemaInitializer = new SchemaInitializer(adminClient(), configuration, getLogHelper(), getSchemaController());
            FolderInitializer folderInitializer = new FolderInitializer(adminClient(), configuration, getLogHelper(), getNodeController());

            return schemaInitializer.initialize().andThen(folderInitializer.initialize());
        } catch (MeshPluginException e) {
            return Completable.error(e);
        }
    }

    private void initializeConfig() throws IOException {
        File config = getConfigFile();
        log.info(logHelper.generateLogMessage("", String.format("Configuration file is in %s", config.getAbsolutePath())));

        if (config.exists()) {
            log.info(logHelper.generateLogMessage("", "Configuration file exists, loading configuration!"));
            configuration = readConfig(MustReadConfig.class);
        } else {
            log.info(logHelper.generateLogMessage("", "Configuration file doesn't exists, creating configuration"));

            configuration = generateDefaultConfiguration();

            writeConfig(configuration);
        }
    }

    @Override
    public Router createProjectRouter() {
        Router router = Router.router(vertx());

        log.info(logHelper.generateLogMessage("", "Registering Endpoints for projects"));
        InternalEndpointBuilder.wrap(router)
        	.withMethod(HttpMethod.GET)
        	.withPath("/unread")
        	.withRAMLPath("/unread")
        	.withDescription("Get unread pages")
        	.withHandler(new UnreadHandler(this, getConfiguration(), getLogHelper(), getNodeController()))
        	.produces(APPLICATION_JSON)
        	.withExampleResponse(HttpResponseStatus.OK, new JsonObject(), "JSON response")
        	.build();
        
        InternalEndpointBuilder.wrap(router)
        	.withMethod(HttpMethod.POST)
        	.withPath("/read")
        	.withRAMLPath("/read")
        	.consumes(APPLICATION_JSON)
        	.withDescription("Post an array of page UUIDs to be marked as read")
        	.withExampleRequest(new JsonObject().put(getConfiguration().getNodeListParameterName(), new JsonArray()))
        	.withExampleResponse(HttpResponseStatus.OK, new MessageResponse("successful"), APPLICATION_JSON)
        	.withHandler(new MakeReadHandler(this, getConfiguration(), getLogHelper(), getNodeController()))
        	.build();

        return router;
    }

    /**
     * Generates the default configuration
     *
     * @return
     */
    protected MustReadConfig generateDefaultConfiguration() {
        MustReadConfig config = new MustReadConfig();
        ArrayList<ProjectConfig> projects = new ArrayList<>();

        projects.add(
                new ProjectConfig()
                        .setName("")
                        .setFolderName("")
                        .setQueryFiles(new QueryFilesConfig().setData("").setUuid(""))
                        .setFieldNames(new FieldNamesConfig().setUuid("").setPublished(""))
        );

        config.setProjects(projects);

        return config;
    }

    protected MustReadConfig getConfiguration() {
        if (configuration == null) {
            try {
                initializeConfig();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return configuration;
    }

    public String getGraphQlDir() {
        return new File(getStorageDir(), "graphql").getAbsolutePath();
    }

    protected LogHelper getLogHelper() {
        if (logHelper == null) {
            logHelper = new LogHelper(this);
        }

        return logHelper;
    }

    private SchemaController getSchemaController() {
        if (schemaController == null) {
            schemaController = new SchemaController(adminClient(), getLogHelper());
        }

        return schemaController;
    }

    private NodeController getNodeController() {
        if (nodeController == null) {
            nodeController = new NodeController(adminClient(), getLogHelper(), getSchemaController());
        }

        return nodeController;
    }

    private ProjectCreateConsumer getProjectCreateConsumer() {
        if (projectCreateConsumer == null) {
            projectCreateConsumer = new ProjectCreateConsumer(this, adminClient(), getLogHelper(), getConfiguration(), getSchemaController(), getNodeController());
        }

        return projectCreateConsumer;
    }

    // We override these function because we need to use a special adminClient to be able to access v2 api
    @Override
    protected void createAdminClient() {

        int port = environment().options().getHttpServerOptions().getPort();
        String host = "127.0.0.1";

        MeshRestClientConfig conf = new MeshRestClientConfig.Builder()
                .setHost(host)
                .setPort(port)
                .setBasePath("/api/v2")
                .setSsl(false)
                .build();
        fakeAdminClient = MeshRestClient.create(conf);
        fakeAdminClient.setAPIKey(environment().adminToken());

    }

    @Override
    public Completable shutdown() {
        return super.shutdown().andThen(Completable.defer(() -> {
            if (fakeAdminClient != null) {
                fakeAdminClient.close();
            }

            return Completable.complete();
        }));
    }

    @Override
    public MeshRestClient adminClient() {
        return fakeAdminClient;
    }
}
