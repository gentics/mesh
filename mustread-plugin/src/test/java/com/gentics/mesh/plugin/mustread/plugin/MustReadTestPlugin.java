package com.gentics.mesh.plugin.mustread.plugin;

import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.mustread.MustReadPlugin;
import com.gentics.mesh.plugin.mustread.MustReadPluginTest;
import com.gentics.mesh.plugin.mustread.config.MustReadConfig;
import com.gentics.mesh.plugin.mustread.config.ProjectConfig;
import com.gentics.mesh.plugin.mustread.handler.ConfigurationHandler;
import io.vertx.ext.web.Router;
import org.pf4j.PluginWrapper;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class MustReadTestPlugin extends MustReadPlugin {

    public MustReadTestPlugin(PluginWrapper wrapper, PluginEnvironment env) {
        super(wrapper, env);
    }

    public static MustReadConfig generateTestConfiguration() {
        MustReadConfig config = new MustReadConfig();
        ArrayList<ProjectConfig> projects = new ArrayList<>();

        projects.add(
                new ProjectConfig()
                        .setName("EmptyGraphQl")
                        .setFolderName("must-read")
                        .setUuidQueryFile("")
                        .setDataQueryFile("")
                        .setUuidFieldName("uuid")
        );

        projects.add(
                new ProjectConfig()
                        .setName("EmptyFolderName")
                        .setFolderName("")
                        .setUuidQueryFile("Test/MustRead.graphql")
                        .setDataQueryFile("Test/Data.graphql")
                        .setUuidFieldName("uuid")
        );

        projects.add(
                new ProjectConfig()
                        .setName("NonExistingMustRead")
                        .setFolderName("must-read")
                        .setUuidQueryFile("Test2/MustRead.graphql")
                        .setDataQueryFile("Test/Data.graphql")
                        .setUuidFieldName("uuid")
        );

        projects.add(
                new ProjectConfig()
                        .setName("NonExistingData")
                        .setFolderName("must-read")
                        .setUuidQueryFile("Test/MustRead.graphql")
                        .setDataQueryFile("Test2/Data.graphql")
                        .setUuidFieldName("uuid")
        );

        projects.add(
                new ProjectConfig()
                        .setName("Test")
                        .setFolderName("Must-Read")
                        .setUuidQueryFile("Test/MustRead.graphql")
                        .setDataQueryFile("Test/Data.graphql")
                        .setUuidFieldName("testUuid")
        );

        projects.add(
                new ProjectConfig()
                        .setName("NoSetUUIDFieldName")
                        .setFolderName("Must-Read")
                        .setUuidQueryFile("Test/MustRead.graphql")
                        .setDataQueryFile("Test/Data.graphql")
                        .setUuidFieldName("")
        );

        config.setNodeListParameterName("nodes");
        config.setProjects(projects);

        return config;
    }

    @Override
    protected MustReadConfig generateDefaultConfiguration() {
        return generateTestConfiguration();
    }

    @Override
    protected File getPluginBaseDir() {
        try {
            return new File(MustReadPluginTest.class.getResource(".").toURI());
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public Router createGlobalRouter() {
        Router router = Router.router(vertx());

        router.get("/configuration").handler(new ConfigurationHandler(this, getConfiguration(), getLogHelper()));

        return router;
    }
}
