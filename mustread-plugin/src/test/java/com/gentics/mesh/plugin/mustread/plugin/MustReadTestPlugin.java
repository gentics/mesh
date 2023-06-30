package com.gentics.mesh.plugin.mustread.plugin;

import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.mustread.MustReadPlugin;
import com.gentics.mesh.plugin.mustread.MustReadPluginTest;
import com.gentics.mesh.plugin.mustread.config.FieldNamesConfig;
import com.gentics.mesh.plugin.mustread.config.MustReadConfig;
import com.gentics.mesh.plugin.mustread.config.ProjectConfig;
import com.gentics.mesh.plugin.mustread.config.QueryFilesConfig;
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
        config.setLicenseKey(System.getenv("LICENSEKEY"));
        ArrayList<ProjectConfig> projects = new ArrayList<>();

        QueryFilesConfig emptyQueryFiles = new QueryFilesConfig().setData("").setUuid("");
        QueryFilesConfig testQueryFiles = new QueryFilesConfig().setData("Test/Data.graphql").setUuid("Test/MustRead.graphql");
        QueryFilesConfig nonExistingDataQueryFiles = new QueryFilesConfig().setData("Test2/Data.graphql").setUuid("Test/MustRead.graphql");
        QueryFilesConfig nonExistingUUIDQueryFiles = new QueryFilesConfig().setData("Test/Data.graphql").setUuid("Test2/MustRead.graphql");

        FieldNamesConfig emptyFieldNames = new FieldNamesConfig().setUuid("").setPublished("");
        FieldNamesConfig testFieldNames = new FieldNamesConfig().setUuid("testUuid").setPublished("testPublished");

        projects.add(
                new ProjectConfig()
                        .setName("EmptyGraphQl")
                        .setFolderName("must-read")
                        .setQueryFiles(emptyQueryFiles)
                        .setFieldNames(testFieldNames)
                        .setPublishedOnly(false)
        );

        projects.add(
                new ProjectConfig()
                        .setName("EmptyFolderName")
                        .setFolderName("")
                        .setQueryFiles(testQueryFiles)
                        .setFieldNames(testFieldNames)
                        .setPublishedOnly(false)
        );

        projects.add(
                new ProjectConfig()
                        .setName("NonExistingUUIDQueryFile")
                        .setFolderName("must-read")
                        .setQueryFiles(nonExistingUUIDQueryFiles)
                        .setFieldNames(testFieldNames)
                        .setPublishedOnly(false)
        );

        projects.add(
                new ProjectConfig()
                        .setName("NonExistingDataQueryFile")
                        .setFolderName("must-read")
                        .setQueryFiles(nonExistingDataQueryFiles)
                        .setFieldNames(testFieldNames)
                        .setPublishedOnly(false)
        );

        projects.add(
                new ProjectConfig()
                        .setName("Test")
                        .setFolderName("Must-Read")
                        .setQueryFiles(testQueryFiles)
                        .setFieldNames(testFieldNames)
                        .setPublishedOnly(false)
        );

        projects.add(
                new ProjectConfig()
                        .setName("NoSetUUIDFieldName")
                        .setFolderName("Must-Read")
                        .setQueryFiles(testQueryFiles)
                        .setFieldNames(emptyFieldNames)
                        .setPublishedOnly(false)
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
