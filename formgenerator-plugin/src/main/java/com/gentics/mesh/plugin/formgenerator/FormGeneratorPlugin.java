package com.gentics.mesh.plugin.formgenerator;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.AbstractPlugin;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.formgenerator.config.FormGeneratorConfig;
import com.gentics.mesh.plugin.formgenerator.dagger.DaggerFormGeneratorPluginComponent;
import com.gentics.mesh.plugin.formgenerator.dagger.FormGeneratorPluginComponent;
import com.gentics.mesh.plugin.formgenerator.handler.AbstractFormGeneratorHandler;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;

import io.reactivex.Completable;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class FormGeneratorPlugin extends AbstractPlugin implements RestPlugin {
	public final static String SECRET_HEADER_NAME = "FG-ACCESSTOKEN";

	private static final Logger log = LoggerFactory.getLogger(FormGeneratorPlugin.class);

	private FormGeneratorConfig configuration;

	private Map<String, String> rootNodeUuids = new HashMap<>();

	private Map<String, String> baseUrls = new HashMap<>();

	public FormGeneratorPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Router createProjectRouter() {
		Router projectRouter = Router.router(vertx());
		Handler<RoutingContext> securityHandler = rc -> {
			String sentKey = rc.request().getHeader(SECRET_HEADER_NAME);
			if (sentKey == null) {
				rc.fail(401);
			} else if (!sentKey.equals(configuration.getApiKey())) {
				rc.fail(401);
			} else {
				rc.next();
			}
		};

		// TODO cors
		projectRouter.route("/forms").handler(securityHandler);
		projectRouter.route("/forms").method(POST).handler(fromComponent(FormGeneratorPluginComponent::createFormHandler));
		projectRouter.route("/forms").method(GET).handler(fromComponent(FormGeneratorPluginComponent::listFormsHandler));

		projectRouter.route("/forms/:formUuid").method(GET).produces(APPLICATION_JSON).handler(securityHandler);
		projectRouter.route("/forms/:formUuid").method(GET).produces(APPLICATION_JSON).handler(fromComponent(FormGeneratorPluginComponent::loadFormHandler));

		projectRouter.route("/forms/:formUuid").method(GET).produces("text/html").handler(fromComponent(FormGeneratorPluginComponent::renderFormHandler));

		projectRouter.route("/forms/:formUuid").method(POST).handler(securityHandler);
		projectRouter.route("/forms/:formUuid").method(POST).handler(fromComponent(FormGeneratorPluginComponent::updateFormHandler));
		projectRouter.route("/forms/:formUuid").method(DELETE).handler(securityHandler);
		projectRouter.route("/forms/:formUuid").method(DELETE).handler(fromComponent(FormGeneratorPluginComponent::deleteFormHandler));

		projectRouter.route("/forms/:formUuid/elements").method(GET).produces(APPLICATION_JSON).handler(fromComponent(FormGeneratorPluginComponent::loadFormElementsHandler));

		projectRouter.route("/forms/:formUuid/schema").handler(securityHandler);
		projectRouter.route("/forms/:formUuid/schema").method(GET).produces(APPLICATION_JSON).handler(fromComponent(FormGeneratorPluginComponent::loadFormSchemaHandler));

		projectRouter.route("/forms/:formUuid/data").method(POST).handler(fromComponent(FormGeneratorPluginComponent::postDataHandler));
		projectRouter.route("/forms/:formUuid/jsondata").method(POST).handler(fromComponent(FormGeneratorPluginComponent::postJsonDataHandler));
		projectRouter.route("/forms/:formUuid/data").method(GET).handler(securityHandler);
		projectRouter.route("/forms/:formUuid/data").method(GET).produces(APPLICATION_JSON).handler(fromComponent(FormGeneratorPluginComponent::findFormDataHandler));

		projectRouter.route("/forms/:formUuid/data/:formDataUuid").method(GET).produces(APPLICATION_JSON).handler(fromComponent(FormGeneratorPluginComponent::loadFormDataHandler));
		projectRouter.route("/forms/:formUuid/data/:formDataUuid").method(GET).produces("text/html").handler(fromComponent(FormGeneratorPluginComponent::renderFormDataHandler));
		projectRouter.route("/forms/:formUuid/data/:formDataUuid").method(POST).handler(fromComponent(FormGeneratorPluginComponent::updatePostedDataHandler));
		projectRouter.route("/forms/:formUuid/jsondata/:formDataUuid").method(POST).handler(fromComponent(FormGeneratorPluginComponent::updatePostedJsonDataHandler));

		projectRouter.route("/forms/:formUuid/data/:formDataUuid").method(DELETE).handler(securityHandler);
		projectRouter.route("/forms/:formUuid/data/:formDataUuid").method(DELETE).handler(fromComponent(FormGeneratorPluginComponent::deleteFormDataHandler));

		// Downloading binaries is not secured by the security handler, so that uploaded files can be downloaded via the portal.
		projectRouter.route("/forms/:formUuid/data/:formDataUuid/binary/:binaryFieldName").method(GET).handler(fromComponent(FormGeneratorPluginComponent::loadFormDataBinaryHandler));

		projectRouter.route("/forms/:formUuid/export").handler(securityHandler);
		projectRouter.route("/forms/:formUuid/export").method(GET).handler(fromComponent(FormGeneratorPluginComponent::exportHandler));

		projectRouter.route("/forms/:formUuid/binaries").handler(securityHandler);
		projectRouter.route("/forms/:formUuid/binaries").method(GET).produces("application/zip").handler(fromComponent(FormGeneratorPluginComponent::loadFilesZipHandler));

		projectRouter.route("/templates").handler(securityHandler);
		projectRouter.route("/templates").method(POST).handler(fromComponent(FormGeneratorPluginComponent::createTemplateHandler));
		projectRouter.route("/templates").method(GET).handler(fromComponent(FormGeneratorPluginComponent::listTemplatesHandler));

		projectRouter.route("/templates/:templateUuid").handler(securityHandler);
		projectRouter.route("/templates/:templateUuid").method(GET).handler(fromComponent(FormGeneratorPluginComponent::loadTemplateHandler));
		projectRouter.route("/templates/:templateUuid").method(DELETE).handler(fromComponent(FormGeneratorPluginComponent::deleteTemplateHandler));
		return projectRouter;
	}

	@Override
	public Completable initialize() {
		try {
			configuration = readConfig(FormGeneratorConfig.class);

			// initialize schemas, etc.
			return component().schemaInitializer().initialize();
		} catch (Exception e) {
			return Completable.error(e);
		}

	}

	private FormGeneratorPluginComponent component() {
		return DaggerFormGeneratorPluginComponent.builder()
			.adminClient(adminClient())
			.rootNodeUuids(rootNodeUuids)
			.baseUrls(baseUrls)
			.configuration(configuration)
			.vertx(vertx())
			.storageDir(getStorageDir())
			.build();
	}

	private Handler<RoutingContext> fromComponent(Function<FormGeneratorPluginComponent, AbstractFormGeneratorHandler> handler) {
		return context -> {
			PluginContext meshContext = wrap(context);

			// TODO better error handler
			handler.apply(component()).handle(meshContext).doOnError(t -> {
				log.error("Error while handling request", t);
				if (t instanceof MeshRestClientMessageException) {
					MeshRestClientMessageException meshException = (MeshRestClientMessageException) t;
					context.response().setStatusCode(meshException.getStatusCode()).setStatusMessage(meshException.getMessage()).end();
				} else if (t instanceof FormGeneratorError) {
					FormGeneratorError meshException = (FormGeneratorError) t;
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
