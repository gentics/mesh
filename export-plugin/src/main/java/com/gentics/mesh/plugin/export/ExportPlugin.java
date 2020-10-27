package com.gentics.mesh.plugin.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.gentics.mesh.RestAPIVersion;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.AbstractPlugin;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.plugin.export.validaton.ConstraintViolationSerializer;
import com.gentics.mesh.plugin.export.handler.DecodeValidateHandler;
import com.gentics.mesh.plugin.export.handler.CsvHandler;
import com.gentics.mesh.plugin.export.model.CsvRequest;
import com.gentics.mesh.plugin.export.model.PluginConfig;
import io.reactivex.Completable;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.commons.io.IOUtils;
import org.pf4j.PluginWrapper;

import javax.validation.ConstraintViolation;
import java.io.*;
import java.nio.charset.Charset;

import static io.vertx.ext.web.Router.router;

public class ExportPlugin extends AbstractPlugin implements RestPlugin {

    private static final Logger log = LoggerFactory.getLogger(ExportPlugin.class);

    public PluginConfig config = null;

    public ExportPlugin(PluginWrapper wrapper, PluginEnvironment env) {
        super(wrapper, env);
    }

    private static InputStream loadFile(String filename) {
        InputStream inputStream = null;
        File configFile = new File(filename);
        if (configFile.canRead()) {
            if (log.isInfoEnabled()) {
                log.info("Loading configuration file {" + configFile + "}.");
            }

            try {
                inputStream = new FileInputStream(configFile);
            } catch (FileNotFoundException fnfe) {
                log.error("Could not load configuration file {" + configFile.getAbsolutePath() + "}.", fnfe);
            }
        } else {
            inputStream = PluginConfig.class.getResourceAsStream("/" + filename);
            if (log.isInfoEnabled()) {
                log.info("Configuration file {" + filename + "} was not found within filesystem.");
                if (inputStream == null) {
                    log.info("Configuration file {" + filename + "} was not found within classpath.");
                } else {
                    log.info("Loading configuration file {" + filename + "} from classpath.");
                }
            }
        }

        return inputStream;
    }

    public static PluginConfig loadDefaultConfigFrom(String filename) {
        InputStream ins = loadFile(filename);

        if (ins == null) {
            log.info("Configuration file {" + filename + "} not found");
            return new PluginConfig();
        } else {
            try {
                log.info("Loaded {" + filename + "} configuration");
                return JsonUtil.readValue(IOUtils.toString(ins, Charset.defaultCharset()), PluginConfig.class);
            } catch (IOException var3) {
                log.error("Could not load {" + filename + "} configuration", var3);
                return new PluginConfig();
            }
        }
    }

    @Override
    public Completable initialize() {
        return Completable.defer(() -> {
            try {
                ObjectMapper mapper = JsonUtil.getMapper();
                SimpleModule module = new SimpleModule("export-plugin");
                module.addSerializer(ConstraintViolation.class, new ConstraintViolationSerializer());
                mapper.registerModule(module);

                config = readConfig(PluginConfig.class);

                if (config == null) {
                    //get JSON
                    config = loadDefaultConfigFrom("default-config.json");

                    //write into config.yml
                    writeConfig(config);
                }

                if (config == null) {
                    throw new IOException();
                }

                return Completable.complete();
            } catch (IOException e) {

                log.info("Could not find Config file, reading in default config ...");
                return Completable.error(e);
            }
        });
    }

    @Override
    public Router createProjectRouter() {
        Router router = router(vertx());
        MeshRestClient adminClient = this.environment().createAdminClient(RestAPIVersion.V2);
        CsvHandler handler = new CsvHandler(adminClient, this.config);

        router.route("/csv")
                .method(HttpMethod.POST)
                .handler(BodyHandler.create())
                .handler(new DecodeValidateHandler<>(CsvRequest.class))
                .handler(rc -> {
                    PluginContext context = wrap(rc);
                    handler.handle(context);
                });

        return router;
    }
}
