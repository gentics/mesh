package com.gentics.mesh.plugin.forms;

import static io.vertx.core.http.HttpMethod.GET;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.dagger.FormsPluginComponent;
import com.gentics.mesh.plugin.forms.model.FormtypeConfigsModel;
import com.gentics.mesh.plugin.forms.service.StatusService;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.vertx.ext.web.Router;

/**
 * Tested plugin, the configuration is loaded from the file "config.yml" in the classpath
 */
public class FormsTestPlugin extends FormsPlugin {
	/**
	 * Exposed singleton instance of deployed FormsTestPlugin
	 */
	public static FormsTestPlugin instance;

	/**
	 * Exposed configuration
	 */
	public FormsConfig configuration;

	public FormsTestPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
		instance = this;
	}

	@Override
	public Router createProjectRouter() {
		Router projectRouter = super.createProjectRouter();

		// add route to purge stale data
		projectRouter.route("/test/purge/:seconds").method(GET).handler(fromComponent(FormsPluginComponent::purgeStaleDataHandler));

		return projectRouter;
	}

	/**
	 * Call {@link StatusService#getFormtypeConfigs(String, MeshRestClient)} directly for testing.
	 */
	public FormtypeConfigsModel getFormtypeConfigs(String projectName, MeshRestClient client) {
		return component(null).statusService().getFormtypeConfigs(projectName, client).blockingGet();
	}

	/**
	 * The config file is read from the classpath
	 */
	@Override
	protected File getConfigFile() {
		try {
			return new File(AbstractFormsPluginIntegrationTest.class.getResource("config.yml").toURI());
		} catch (URISyntaxException e) {
			return null;
		}
	};

	/**
	 * Read the configuration and store in {@link #configuration}.
	 *
	 * @param clazz class of the configuration
	 */
	@Override
	public <T> T readConfig(Class<T> clazz) throws FileNotFoundException, IOException {
		T config = super.readConfig(clazz);
		if (config instanceof FormsConfig) {
			configuration = (FormsConfig) config;
			configuration.setTesting(true);
		}
		return config;
	}
}
