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
                        .setMustReadGraphQlFile("")
                        .setDataGraphQlFile("")
                        .setUuidFieldName("uuid")
        );

        projects.add(
                new ProjectConfig()
                        .setName("EmptyFolderName")
                        .setFolderName("")
                        .setMustReadGraphQlFile("Test/MustRead.graphql")
                        .setDataGraphQlFile("Test/Data.graphql")
                        .setUuidFieldName("uuid")
        );

        projects.add(
                new ProjectConfig()
                        .setName("NonExistingMustRead")
                        .setFolderName("must-read")
                        .setMustReadGraphQlFile("Test2/MustRead.graphql")
                        .setDataGraphQlFile("Test/Data.graphql")
                        .setUuidFieldName("uuid")
        );

        projects.add(
                new ProjectConfig()
                        .setName("NonExistingData")
                        .setFolderName("must-read")
                        .setMustReadGraphQlFile("Test/MustRead.graphql")
                        .setDataGraphQlFile("Test2/Data.graphql")
                        .setUuidFieldName("uuid")
        );

        projects.add(
                new ProjectConfig()
                        .setName("Test")
                        .setFolderName("Must-Read")
                        .setMustReadGraphQlFile("Test/MustRead.graphql")
                        .setDataGraphQlFile("Test/Data.graphql")
                        .setUuidFieldName("testUuid")
        );

        projects.add(
                new ProjectConfig()
                        .setName("NoSetUUIDFieldName")
                        .setFolderName("Must-Read")
                        .setMustReadGraphQlFile("Test/MustRead.graphql")
                        .setDataGraphQlFile("Test/Data.graphql")
                        .setUuidFieldName("")
        );

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
