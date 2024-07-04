package com.gentics.mesh.plugin.export;

import static io.vertx.ext.web.Router.router;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.gentics.mesh.RestAPIVersion;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.AbstractPlugin;
import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.plugin.common.license.LicenseService;
import com.gentics.mesh.plugin.common.license.LicensedPlugin;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.export.dagger.DaggerExportPluginComponent;
import com.gentics.mesh.plugin.export.dagger.ExportPluginComponent;
import com.gentics.mesh.plugin.export.extension.ExportConfigValuePlugin;
import com.gentics.mesh.plugin.export.extension.ConfigValueProvider;
import com.gentics.mesh.plugin.export.handler.CsvHandler;
import com.gentics.mesh.plugin.export.handler.DecodeValidateHandler;
import com.gentics.mesh.plugin.export.handler.RoleCheckHandler;
import com.gentics.mesh.plugin.export.model.CsvRequest;
import com.gentics.mesh.plugin.export.model.PluginConfiguration;
import com.gentics.mesh.plugin.export.validaton.ConstraintViolationSerializer;
import com.gentics.mesh.rest.client.MeshRestClient;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.core.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.commons.io.IOUtils;
import org.pf4j.PluginWrapper;

import javax.validation.ConstraintViolation;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExportPlugin extends AbstractPlugin implements RestPlugin, ExportConfigValuePlugin {

	private static final Logger log = LoggerFactory.getLogger(ExportPlugin.class);

	private ExportPluginComponent component;

	public ExportPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Completable initialize() {
		return Completable.defer(() -> {
			try {
				// Setup the configuration
				PluginConfiguration config = readConfig(PluginConfiguration.class);
				config.overrideWithEnv();

				LicenseService licenseService = new LicenseService(config);
				licenseService.isLicenseSufficient(LicensedPlugin.MEXP);

				// Setup JSON Validation and serialization
				ObjectMapper mapper = JsonUtil.getMapper();
				SimpleModule module = new SimpleModule("export-plugin");
				module.addSerializer(ConstraintViolation.class, new ConstraintViolationSerializer());
				mapper.registerModule(module);

				if (config == null) {
					//get JSON
					config = loadDefaultConfigFrom("default-config.json");

					//write into config.yml
					writeConfig(config);
				}

				if (config == null) {
					throw new IOException();
				}

				// Get the Environment data
				config.overrideWithEnv();

				// Create the component
				this.component = DaggerExportPluginComponent.builder()
					.setMeshClient(this.environment().createAdminClient(RestAPIVersion.V2))
					.setPluginConfiguration(config)
					.setPluginManager(getWrapper().getPluginManager())
					.build();

				return setupRoles();
			} catch (IOException e) {

				log.info("Could not find Config file, reading in default config ...");
				return Completable.error(e);
			}
		});
	}

	@Override
	public Router createProjectRouter() {
		Router router = router(vertx());
		RoleCheckHandler roleCheck = new RoleCheckHandler(this.component.getConfiguration().getRoleName());
		CsvHandler handler = component.getCsvHandler();

		router.route("/csv")
			.method(HttpMethod.POST)
			.handler(BodyHandler.create())
			.handler(wrapHandler(roleCheck))
			.handler(new DecodeValidateHandler<>(CsvRequest.class))
			.handler(wrapHandler(handler));

		return router;
	}

	@Override
	public List<ConfigValueProvider> getConfigValueProviders() {
		return Collections.singletonList(component.getWebrootMappingProvider());
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
			inputStream = PluginConfiguration.class.getResourceAsStream("/" + filename);
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

	private static PluginConfiguration loadDefaultConfigFrom(String filename) {
		InputStream ins = loadFile(filename);

		if (ins == null) {
			log.info("Configuration file {" + filename + "} not found");
			return new PluginConfiguration();
		} else {
			try {
				log.info("Loaded {" + filename + "} configuration");
				return JsonUtil.readValue(IOUtils.toString(ins, Charset.defaultCharset()), PluginConfiguration.class);
			} catch (IOException var3) {
				log.error("Could not load {" + filename + "} configuration", var3);
				return new PluginConfiguration();
			}
		}
	}

	private Completable setupRoles() {
		return Completable.defer(() -> {
			if (!this.component.getConfiguration().isRoleCheck()) {
				log.info("Role check is getting skipped!");
				return Completable.complete();
			}

			MeshRestClient client = component.getMeshClient();
			Set<String> rolesToCheck = new HashSet<>(Arrays.asList(
				component.getConfiguration().getRoleName()
			));

			log.debug("Setting up roles for Export Plugin: {} ...", rolesToCheck);

			return client.findRoles().toSingle().flatMapObservable(response -> {
				response.getData().forEach(role -> rolesToCheck.remove(role.getName()));
				return Observable.fromIterable(rolesToCheck);
			}).flatMapCompletable(role -> {
				log.debug("Role {} is missing, therefore creating it ...", role);
				RoleCreateRequest req = new RoleCreateRequest();
				req.setName(role);

				return client.createRole(req).toCompletable()
					.doOnComplete(() -> log.debug("Successfully created role {}!", role))
					.doOnError(err -> log.error("Error while creating role {}!", role));
			});
		})
			.doOnComplete(() -> log.debug("Successfully setup all roles!"))
			.doOnError(err -> log.error("Error while setting up roles!"));
	}
}
