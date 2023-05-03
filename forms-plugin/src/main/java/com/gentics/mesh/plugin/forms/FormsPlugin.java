package com.gentics.mesh.plugin.forms;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import com.gentics.mesh.plugin.common.license.LicenseService;
import com.gentics.mesh.plugin.common.license.LicensedPlugin;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.AbstractPlugin;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.dagger.DaggerFormsPluginComponent;
import com.gentics.mesh.plugin.forms.dagger.FormsPluginComponent;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;

import io.reactivex.Completable;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * Main class for the forms plugin
 */
public class FormsPlugin extends AbstractPlugin implements RestPlugin {
	private static final Logger log = LoggerFactory.getLogger(FormsPlugin.class);

	private FormsConfig configuration;

	private Map<String, String> rootNodeUuids = new ConcurrentHashMap<>();

	private Map<String, String> baseUrls = new ConcurrentHashMap<>();

	public FormsPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Router createProjectRouter() {
		Router projectRouter = Router.router(vertx());

		// routes for the activation service
		projectRouter.route("/active").method(GET).handler(fromComponent(FormsPluginComponent::checkStatusHandler));
		projectRouter.route("/active").method(PUT).handler(fromComponent(FormsPluginComponent::activateHandler));
		projectRouter.route("/active").method(DELETE).handler(fromComponent(FormsPluginComponent::deactivateHandler));

		// routes for getting/setting supported languages
		projectRouter.route("/languages").method(GET).handler(fromComponent(FormsPluginComponent::getLanguagesHandler));
		projectRouter.route("/languages").method(PUT).handler(fromComponent(FormsPluginComponent::setLanguagesHandler));

		// TODO cors
		projectRouter.route("/forms").method(GET).handler(fromComponent(FormsPluginComponent::listFormsHandler));

		projectRouter.route("/forms/:formUuid").method(GET).produces(APPLICATION_JSON).handler(fromComponent(FormsPluginComponent::loadFormHandler));
		projectRouter.route("/forms/:formUuid").method(GET).produces("text/html").handler(fromComponent(FormsPluginComponent::renderFormHandler));
		projectRouter.route("/forms/:formUuid").method(POST).produces(APPLICATION_JSON).handler(fromComponent(FormsPluginComponent::upsertFormHandler));
		projectRouter.route("/forms/:formUuid").method(DELETE).handler(fromComponent(FormsPluginComponent::deleteFormHandler));

		// routes for handling the online status of forms
		projectRouter.route("/forms/:formUuid/online/:lang").method(GET).produces(APPLICATION_JSON).handler(fromComponent(FormsPluginComponent::getFormOnlineStatusHandler));
		projectRouter.route("/forms/:formUuid/online/:lang").method(PUT).produces(APPLICATION_JSON).handler(fromComponent(FormsPluginComponent::setFormOnlineHandler));
		projectRouter.route("/forms/:formUuid/online/:lang").method(DELETE).produces(APPLICATION_JSON).handler(fromComponent(FormsPluginComponent::setFormOfflineHandler));

		// routes for rendering previews of forms
		projectRouter.route("/forms/:formUuid/preview").method(POST).produces("text/html").handler(fromComponent(FormsPluginComponent::renderFormPreviewHandler));
		projectRouter.route("/forms/:formUuid/preview").method(POST).produces(APPLICATION_JSON).handler(fromComponent(FormsPluginComponent::renderFormPreviewAsJsonHandler));

		projectRouter.route("/forms/:formUuid/elements").method(GET).produces(APPLICATION_JSON).handler(fromComponent(FormsPluginComponent::loadFormElementsHandler));

		projectRouter.route("/forms/:formUuid/schema").method(GET).produces(APPLICATION_JSON).handler(fromComponent(FormsPluginComponent::loadFormSchemaHandler));

		projectRouter.route("/forms/:formUuid/data").method(POST).handler(fromComponent(FormsPluginComponent::postDataHandler));
		projectRouter.route("/forms/:formUuid/jsondata").method(POST).handler(fromComponent(FormsPluginComponent::postJsonDataHandler));
		projectRouter.route("/forms/:formUuid/data").method(GET).produces(APPLICATION_JSON).handler(fromComponent(FormsPluginComponent::findFormDataHandler));

		projectRouter.route("/forms/:formUuid/data/:formDataUuid").method(GET).produces(APPLICATION_JSON).handler(fromComponent(FormsPluginComponent::loadFormDataHandler));
		projectRouter.route("/forms/:formUuid/data/:formDataUuid").method(GET).produces("text/html").handler(fromComponent(FormsPluginComponent::renderFormDataHandler));
		projectRouter.route("/forms/:formUuid/data/:formDataUuid").method(POST).handler(fromComponent(FormsPluginComponent::updatePostedDataHandler));
		projectRouter.route("/forms/:formUuid/jsondata/:formDataUuid").method(POST).handler(fromComponent(FormsPluginComponent::updatePostedJsonDataHandler));

		projectRouter.route("/forms/:formUuid/data/:formDataUuid").method(DELETE).handler(fromComponent(FormsPluginComponent::deleteFormDataHandler));

		projectRouter.route("/forms/:formUuid/data/:formDataUuid/binary/:binaryFieldName").method(GET).blockingHandler(fromComponent(FormsPluginComponent::loadFormDataBinaryHandler));

		projectRouter.route("/forms/:formUuid/export").method(GET).handler(fromComponent(FormsPluginComponent::exportHandler));
		projectRouter.route("/forms/:formUuid/export").method(POST).handler(fromComponent(FormsPluginComponent::requestExportHandler));

		projectRouter.route("/forms/:formUuid/binaries").method(GET).produces("application/zip").handler(fromComponent(FormsPluginComponent::loadFilesZipHandler));
		projectRouter.route("/forms/:formUuid/binaries").method(POST).handler(fromComponent(FormsPluginComponent::requestFilesZipHandler));

		projectRouter.route("/forms/:formUuid/download/:downloadType/status").method(GET).handler(fromComponent(FormsPluginComponent::checkDownloadHandler));
		projectRouter.route("/forms/:formUuid/download/:downloadUuid").method(GET).handler(fromComponent(FormsPluginComponent::downloadHandler));

		return projectRouter;
	}

	@Override
	public Completable initialize() {
		try {
			configuration = readConfig(FormsConfig.class);
			if (configuration == null) {
				configuration = new FormsConfig();
			}
			configuration.overrideWithEnv();

			LicenseService licenseService = new LicenseService(configuration.getLicenseKey());
			licenseService.isLicenseSufficient(LicensedPlugin.MFRS);

			return component(null).statusService().initialize(getStorageDir().getAbsolutePath());
		} catch (Exception e) {
			return Completable.error(e);
		}

	}

	private FormsPluginComponent component(MeshRestClient userClient) {
		return DaggerFormsPluginComponent.builder()
			.adminClient(adminClient())
			.userClient(userClient)
			.configuration(configuration)
			.vertx(vertx())
			.storageDir(getStorageDir())
			.manifest(getManifest())
			.build();
	}

	private Handler<RoutingContext> fromComponent(Function<FormsPluginComponent, AbstractFormsHandler> handler) {
		return context -> {
			PluginContext meshContext = wrap(context);
			MeshRestClient userClient = meshContext.client();

			// TODO better error handler
			handler.apply(component(userClient)).handle(meshContext).doOnError(t -> {
				log.error("Error while handling request", t);
				MeshRestClientMessageException mrcmException = null;
				if (t instanceof MeshRestClientMessageException) {
					mrcmException = (MeshRestClientMessageException) t;
				} else if (t.getCause() instanceof MeshRestClientMessageException) {
					mrcmException = (MeshRestClientMessageException) t.getCause();
				}

				if (mrcmException != null) {
					context.response().setStatusCode(mrcmException.getStatusCode()).setStatusMessage(mrcmException.getMessage()).end();
				} else if (t instanceof FormsError) {
					FormsError meshException = (FormsError) t;
					context.response()
						.setStatusCode(meshException.getStatusCode())
						.setStatusMessage(meshException.getMessage())
						.end(new JsonObject().put("error", meshException.getLocalizedMessage()).encodePrettily());
				} else {
					context.response().setStatusCode(500).setStatusMessage("Internal Server Error").end();
				}
			}).subscribe();
		};
	}
}
