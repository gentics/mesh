package com.gentics.mesh.plugin.forms;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON_UTF8;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_OCTET_STREAM;
import static com.gentics.mesh.http.HttpConstants.MULTIPART_FORM_DATA;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import java.text.DateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.pf4j.PluginWrapper;
import org.raml.model.parameter.QueryParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.rest.SortOrder;
import com.gentics.mesh.example.NodeExamples;
import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.plugin.AbstractPlugin;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.RestPlugin;
import com.gentics.mesh.plugin.common.MeshPluginException;
import com.gentics.mesh.plugin.common.license.LicenseService;
import com.gentics.mesh.plugin.common.license.LicensedPlugin;
import com.gentics.mesh.plugin.common.util.MiscUtil;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.dagger.DaggerFormsPluginComponent;
import com.gentics.mesh.plugin.forms.dagger.FormsPluginComponent;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.model.FormtypeConfigsModel;
import com.gentics.mesh.plugin.forms.model.LanguagesModel;
import com.gentics.mesh.plugin.forms.model.TranslationsModel;
import com.gentics.mesh.plugin.forms.model.response.FormOnlineResponse;
import com.gentics.mesh.plugin.forms.model.response.StatusResponse;
import com.gentics.mesh.plugin.forms.model.schema.FormData;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.plugin.forms.service.PurgeService;
import com.gentics.mesh.plugin.forms.service.StatusService;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.util.UUIDUtil;
import com.gentics.vertx.openapi.route.InternalEndpointBuilder;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
		LanguagesModel exampleLanguages = new LanguagesModel().setLanguages(List.of("en", "de"));

		// routes for the activation service
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Check forms status")
			.withPath("/active").withMethod(GET)
			.produces(HttpConstants.APPLICATION_JSON_UTF8)
			.withExampleResponse(HttpResponseStatus.OK, new StatusResponse(), "Current plugin status")
			.withHandler(fromComponent(FormsPluginComponent::checkStatusHandler))
			.build();
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Activate forms for the current project")
			.withPath("/active").withMethod(PUT)
			.produces(APPLICATION_JSON_UTF8)
			.withExampleResponse(HttpResponseStatus.OK, new StatusResponse(), "Current plugin status")
			.withHandler(fromComponent(FormsPluginComponent::activateHandler))
			.build();
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Deactivate forms for the current project")
			.withPath("/active").withMethod(DELETE)
			.produces(HttpConstants.APPLICATION_JSON_UTF8)
			.withExampleResponse(HttpResponseStatus.OK, new StatusResponse(), "Current plugin status")
			.withHandler(fromComponent(FormsPluginComponent::deactivateHandler))
			.build();

		// route for setting formtype configurations
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Set the form type configurations")
			.withPath("/formtypeconfigs").withMethod(POST)
			.consumes(APPLICATION_JSON_UTF8)
			.withExampleRequest(new FormtypeConfigsModel())
			.withHandler(fromComponent(FormsPluginComponent::setFormtypeConfigsHandler))
			.withExampleResponse(HttpResponseStatus.OK, new FormtypeConfigsModel(), "Current formtype configs")
			.produces(APPLICATION_JSON_UTF8)
			.build();

		// routes for translations
		InternalEndpointBuilder.wrap(projectRouter)
			.withPath("/translations/:formUuid/:lang").withMethod(GET)
			.withUriParameter("lang", "Language", "en")
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.withHandler(fromComponent(FormsPluginComponent::getTranslationsHandler))
			.produces(APPLICATION_JSON_UTF8)
			.build();
		InternalEndpointBuilder.wrap(projectRouter)
			.withPath("/translations").withMethod(POST)
			.consumes(APPLICATION_JSON_UTF8)
			.withExampleRequest(new TranslationsModel())
			.withHandler(fromComponent(FormsPluginComponent::setTranslationsHandler))
			.produces(APPLICATION_JSON_UTF8)
			.withExampleResponse(HttpResponseStatus.OK, new TranslationsModel(), APPLICATION_JSON_UTF8)
			.build();

		// routes for getting/setting supported languages
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Get the supported languages for the current project")
			.withPath("/languages").withMethod(GET)
			.withHandler(fromComponent(FormsPluginComponent::getLanguagesHandler))
			.produces(HttpConstants.APPLICATION_JSON_UTF8)
			.withExampleResponse(HttpResponseStatus.OK, exampleLanguages, "Supported languages")
			.build();
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Set supported languages")
			.withPath("/languages").withMethod(PUT)
			.consumes(APPLICATION_JSON_UTF8)
			.produces(HttpConstants.APPLICATION_JSON_UTF8)
			.withExampleRequest(exampleLanguages)
			.withExampleResponse(HttpResponseStatus.OK, exampleLanguages, "Supported languages")
			.withHandler(fromComponent(FormsPluginComponent::setLanguagesHandler))
			.build();

		// TODO cors
		QueryParameter sortOrder = new QueryParameter();
		sortOrder.setDescription("Sort order");
		sortOrder.setExample(SortOrder.ASCENDING.getValue());
		sortOrder.setEnumeration(Arrays.stream(SortOrder.values()).map(SortOrder::getValue).collect(Collectors.toList()));
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Get all forms")
			.withPath("/forms").withMethod(GET)
			.produces(APPLICATION_JSON_UTF8)
			.withQueryParameter("page", "Page number, optional", "1")
			.withQueryParameter("perPage", "Page size, optional", "10")
			.withQueryParameter("sortBy", "Soring field, optional", "name")
			.withQueryParameter("sortOrder", sortOrder)
			.withExampleResponse(HttpResponseStatus.OK, new JsonObject().put("forms", new JsonArray().add(new JsonObject().put("uuid", UUIDUtil.randomUUID()).put("name", "form_12345678").put("cms_id", "87654321"))), "Forms")
			.withHandler(fromComponent(FormsPluginComponent::listFormsHandler))
			.build();

		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Load a form with a given UUID")
			.withPath("/forms/:formUuid").withMethod(GET)
			.produces(APPLICATION_JSON)
			.withExampleResponse(HttpResponseStatus.OK, new JsonObject(), "Forms")
			.withQueryParameter("lang", "Language, optional", "en")
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.withHandler(fromComponent(FormsPluginComponent::loadFormHandler))
			.build();
		InternalEndpointBuilder.wrap(projectRouter).withPath("/forms/:formUuid")
			.withDescription("Upsert a form")
			.withMethod(POST).produces(APPLICATION_JSON)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			//.consumes(APPLICATION_JSON) //Vert.x is super picky about false request content headers, so better don't add ones mid-road
			.withExampleRequest(new JsonObject().put("language", "en").put("cms_id", "87654321").put("name", "form_12345678"))
			.withHandler(fromComponent(FormsPluginComponent::upsertFormHandler))
			.produces(APPLICATION_JSON)
			.withExampleResponse(HttpResponseStatus.OK, new NodeExamples().getNodeResponse2(), "Form node")
			.build();
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Delete a form")
			.withPath("/forms/:formUuid").withMethod(DELETE)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.withHandler(fromComponent(FormsPluginComponent::deleteFormHandler))
			.withExampleResponse(HttpResponseStatus.NO_CONTENT, "Form deleted")
			.build();

		// routes for handling the online status of forms
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Check the form's online status")
			.withPath("/forms/:formUuid/online").withMethod(GET)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.produces(APPLICATION_JSON_UTF8)
			.withExampleResponse(HttpResponseStatus.OK, new FormOnlineResponse(), "Form online status")
			.withHandler(fromComponent(FormsPluginComponent::getFormOnlineStatusHandler))
			.build();
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Set form online for the specific language")
			.withPath("/forms/:formUuid/online").withMethod(PUT)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.produces(APPLICATION_JSON_UTF8)
			.withExampleResponse(HttpResponseStatus.OK, new FormOnlineResponse(), "Form online status")
			.withHandler(fromComponent(FormsPluginComponent::setFormOnlineHandler))
			.build();
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Take a form offline")
			.withPath("/forms/:formUuid/online").withMethod(DELETE)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.produces(APPLICATION_JSON_UTF8)
			.withExampleResponse(HttpResponseStatus.OK, new FormOnlineResponse(), "Form online status")
			.withHandler(fromComponent(FormsPluginComponent::setFormOfflineHandler))
			.build();

		// load form for formgen
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Load a form for the form generator")
			.withPath("/form/:formUuid/load").withMethod(POST)
			.withQueryParameter("formDataUuid", "Form data ID", UUIDUtil.randomUUID())
			.withQueryParameter("lang", "Language", "en")
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.withExampleResponse(HttpResponseStatus.OK, new Form(), "The form")
			.produces(APPLICATION_JSON_UTF8)
			.withHandler(fromComponent(FormsPluginComponent::loadFormGenHandler))
			.build();
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Load form poll results")
			.withPath("/forms/:formUuid/preview").withMethod(GET)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.withQueryParameter("lang", "Language", "en")
			.produces(APPLICATION_JSON_UTF8)
			.withExampleResponse(HttpResponseStatus.OK, 
					new JsonObject().put("results", new JsonArray().add(new JsonObject().put("name", "John Doe").put("score", "42"))).put("templateContext", new Form().getTemplateContext()),
					"Rendered JSON form")
			.withHandler(fromComponent(FormsPluginComponent::loadPollResultsHandler))
			.build();

		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Load form elements")
			.withPath("/forms/:formUuid/elements").withMethod(GET)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.withQueryParameter("lang", "Language code", "en")
			.produces(APPLICATION_JSON)
			.withExampleResponse(HttpResponseStatus.OK, new JsonObject().put("elements", new JsonArray().add(new JsonObject().put("first_name", "string")).add(new JsonObject().put("last_name", "string"))), "Form Elements")
			.withHandler(fromComponent(FormsPluginComponent::loadFormElementsHandler))
			.build();

		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Load form schema")
			.withPath("/forms/:formUuid/schema").withMethod(GET)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.produces(APPLICATION_JSON)
			.withHandler(fromComponent(FormsPluginComponent::loadFormSchemaHandler))
			.withExampleResponse(HttpResponseStatus.OK, new JsonObject(), "Form schema")
			.build();

		projectRouter.route("/form/:formUuid/validate").method(POST).produces(APPLICATION_JSON).handler(fromComponent(FormsPluginComponent::validationHandler));
		projectRouter.route("/form/:formUuid/save").method(POST).handler(fromComponent(FormsPluginComponent::saveDataHandler));
		projectRouter.route("/form/:formUuid/submit").method(POST).handler(fromComponent(FormsPluginComponent::submitDataHandler));
		projectRouter.route("/forms/:formUuid/data").method(GET).produces(APPLICATION_JSON).handler(fromComponent(FormsPluginComponent::findFormDataHandler));
		projectRouter.route("/form/:formUuid/cancel").method(POST).produces(APPLICATION_JSON).handler(fromComponent(FormsPluginComponent::cancelFormDataHandler));
		projectRouter.route("/form/:formUuid/binary/:meshBinaryUuid").method(GET).handler(fromComponent(FormsPluginComponent::loadBinaryHandler));

		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Post (save/modify + publish) a form data")
			.withPath("/form/:formUuid/submit").withMethod(POST)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.withQueryParameter("formDataUuid", "Form data ID", UUIDUtil.randomUUID())
			.withQueryParameter("lang", "Language code", "en")
			.consumes(MULTIPART_FORM_DATA)
			.withExampleResponse(HttpResponseStatus.OK, new FormData(), "Posted data")
			.withHandler(fromComponent(FormsPluginComponent::submitDataHandler))
			.build();
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Save/modify a form data, without publishing")
			.withPath("/form/:formUuid/save").withMethod(POST)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.withQueryParameter("formDataUuid", "Form data ID", UUIDUtil.randomUUID())
			.withQueryParameter("lang", "Language code", "en")
			.consumes(MULTIPART_FORM_DATA)
			.withExampleResponse(HttpResponseStatus.OK, new FormData(), "Posted data")
			.withHandler(fromComponent(FormsPluginComponent::saveDataHandler))
			.build();
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Validate the form data, without saving or publishing")
			.withPath("/form/:formUuid/validate").withMethod(POST)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.withQueryParameter("formDataUuid", "Form data ID", UUIDUtil.randomUUID())
			.withQueryParameter("lang", "Language code", "en")
			.consumes(MULTIPART_FORM_DATA)
			.withExampleResponse(HttpResponseStatus.OK, new FormData(), "Posted data")
			.withHandler(fromComponent(FormsPluginComponent::validationHandler))
			.build();
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Get the posted form data")
			.withPath("/forms/:formUuid/data").withMethod(GET)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.withQueryParameter("formDataUuid", "Form data ID", UUIDUtil.randomUUID())
			.withQueryParameter("q", "Search query, optional", "whatever")
			.withQueryParameter("page", "Page number, optional", "1")
			.withQueryParameter("perPage", "Page size, optional", "10")
			.withQueryParameter("sortBy", "Soring field, optional", "name")
			.withQueryParameter("sortOrder", sortOrder)
			.withQueryParameter(FormDataService.PARAM_PUBLISHED_ONLY, "Should only published data be collected", "true")
			.produces(APPLICATION_JSON)
			.withExampleResponse(HttpResponseStatus.OK, new JsonObject().put("entries", new JsonArray()
					.add(new JsonObject().put("first_name", "Joe").put("last_name", "Doe"))
					.add(new JsonObject().put("first_name", "Jane").put("last_name", "Lane"))), APPLICATION_OCTET_STREAM)
			.withHandler(fromComponent(FormsPluginComponent::findFormDataHandler))
			.build();

		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Cancel the form data")
			.withPath("/form/:formUuid/cancel").withMethod(POST)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.withQueryParameter("formDataUuid", "Form data ID", UUIDUtil.randomUUID())
			.withQueryParameter("lang", "Language code", "en")
			.consumes(MULTIPART_FORM_DATA)
			.withExampleResponse(HttpResponseStatus.OK, "Data has been cancelled")
			.withHandler(fromComponent(FormsPluginComponent::cancelFormDataHandler))
			.build();

		InternalEndpointBuilder.wrap(projectRouter)
			.withPath("/form/:formUuid/binary/:meshBinaryUuid").withMethod(GET)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.withUriParameter("meshBinaryUuid", "BinaryUUID UUID", UUIDUtil.randomUUID())
			.withQueryParameter("formDataUuid", "Form data ID", UUIDUtil.randomUUID())
			.withQueryParameter("lang", "Language code", "en")
			.withQueryParameter("w", "Desired width", "1280")
			.withQueryParameter("h", "Desired height", "800")
			.withHandler(fromComponent(FormsPluginComponent::loadBinaryHandler))
			.produces(APPLICATION_OCTET_STREAM)
			.withExampleResponse(HttpResponseStatus.OK, "Binary data")
			.build();
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Delete the posted form data")
			.withPath("/forms/:formUuid/data/:formDataUuid").withMethod(DELETE)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.withUriParameter("formDataUuid", "Form data UUID", UUIDUtil.randomUUID())
			.withHandler(fromComponent(FormsPluginComponent::deleteFormDataHandler))
			.withExampleResponse(HttpResponseStatus.NO_CONTENT, "Data deleted")
			.build();


		projectRouter.route("/forms/:formUuid/data/:formDataUuid/binary/:binaryFieldName").method(GET).blockingHandler(fromComponent(FormsPluginComponent::loadFormDataBinaryHandler));
		projectRouter.route("/forms/:formUuid/data/:formDataUuid/binary/:binaryFieldName/:index").method(GET).blockingHandler(fromComponent(FormsPluginComponent::loadFormDataMultiBinaryItemHandler));

		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Load form data binary field content")
			.withPath("/forms/:formUuid/data/:formDataUuid/binary/:binaryFieldName").withMethod(GET)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.withUriParameter("formDataUuid", "Form data UUID", UUIDUtil.randomUUID())
			.withUriParameter("binaryFieldName", "Form data binary field key", "uploaded_binary")
			.withBlockingHandler(fromComponent(FormsPluginComponent::loadFormDataBinaryHandler), false)
			.produces(APPLICATION_OCTET_STREAM)
			.withExampleResponse(HttpResponseStatus.OK, "Binary data")
			.build();
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Load form data multibinary binary field content under defined index")
			.withPath("/forms/:formUuid/data/:formDataUuid/binary/:binaryFieldName/:index").withMethod(GET)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.withUriParameter("formDataUuid", "Form data UUID", UUIDUtil.randomUUID())
			.withUriParameter("index", "Multibinary form data index", UUIDUtil.randomUUID())
			.withUriParameter("binaryFieldName", "Form data multibinary field key", "uploaded_binary")
			.withBlockingHandler(fromComponent(FormsPluginComponent::loadFormDataMultiBinaryItemHandler), false)
			.produces(APPLICATION_OCTET_STREAM)
			.withExampleResponse(HttpResponseStatus.OK, "Binary data")
			.build();

		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Export all form data in the CSV format")
			.withPath("/forms/:formUuid/export").withMethod(GET)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.withQueryParameter("lang", "Language code", "en")
			.withHandler(fromComponent(FormsPluginComponent::exportHandler))
			.produces(APPLICATION_OCTET_STREAM)
			.withExampleResponse(HttpResponseStatus.OK, "CSV data")
			.build();
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Request creation of an export node for the given form")
			.withPath("/forms/:formUuid/export").withMethod(POST)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.withQueryParameter("lang", "Language code", "en")
			.withHandler(fromComponent(FormsPluginComponent::requestExportHandler))
			.produces(APPLICATION_JSON)
			.withExampleResponse(HttpResponseStatus.OK, new JsonObject().put("downloadUuid", UUIDUtil.randomUUID()), "UUID of an exported node")
			.build();

		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Download all form data binaries in a single ZIP")
			.withPath("/forms/:formUuid/binaries").withMethod(GET)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.withQueryParameter("lang", "Language code", "en")
			.produces("application/zip")
			.withExampleResponse(HttpResponseStatus.OK, "ZIP data")
			.withHandler(fromComponent(FormsPluginComponent::loadFilesZipHandler))
			.build();
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Request all uploaded binaries in a form of ZIP archive")
			.withPath("/forms/:formUuid/binaries").withMethod(POST)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.withQueryParameter("lang", "Language code", "en")
			.withHandler(fromComponent(FormsPluginComponent::requestFilesZipHandler))
			.produces(APPLICATION_JSON)
			.withExampleResponse(HttpResponseStatus.OK, new NodeExamples().getNodeResponse2(), "Node of a created ZIP")
			.build();

		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Check if the specified download is ready")
			.withPath("/forms/:formUuid/download/:downloadType/status").withMethod(GET)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.withUriParameter("downloadType", "Form UUID", UUIDUtil.randomUUID())
			.withQueryParameter("lang", "Language code", "en")
			.withHandler(fromComponent(FormsPluginComponent::checkDownloadHandler))
			.produces(APPLICATION_JSON_UTF8)
			.withExampleResponse(HttpResponseStatus.OK, new JsonObject().put("downloadReady", true)
					.put("downloadUuid", UUIDUtil.randomUUID())
					.put("downloadTimestamp", DateFormat.getDateTimeInstance().format(Date.from(Instant.now()))), "Download readiness status")
			.build();
		InternalEndpointBuilder.wrap(projectRouter)
			.withDescription("Download prepared data")
			.withPath("/forms/:formUuid/download/:downloadUuid").withMethod(GET)
			.withUriParameter("formUuid", "Form UUID", UUIDUtil.randomUUID())
			.withUriParameter("downloadUuid", "Download UUID", UUIDUtil.randomUUID())
			.withHandler(fromComponent(FormsPluginComponent::downloadHandler))
			.produces(APPLICATION_OCTET_STREAM)
			.withExampleResponse(HttpResponseStatus.OK, "Binary data")
			.build();

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

			configuration.getSecurityOptions().validateSanitationRules();

			LicenseService licenseService = new LicenseService(configuration);
			licenseService.isLicenseSufficient(LicensedPlugin.FORMS);

			FormsPluginComponent component = component(null);
			StatusService statusService = component.statusService();
			FormDataService formDataService = component.formDataService();
			PurgeService purgeService = component.purgeService();

			return statusService.initialize(getStorageDir().getAbsolutePath()).andThen(formDataService.initialize()).andThen(purgeService.initialize());
		} catch (Exception e) {
			return Completable.error(e);
		}

	}

	protected FormsPluginComponent component(MeshRestClient userClient) {
		return DaggerFormsPluginComponent.builder()
			.adminClient(adminClient())
			.userClient(userClient)
			.configuration(configuration)
			.vertx(vertx())
			.storageDir(getStorageDir())
			.manifest(getManifest())
			.env(environment())
			.build();
	}

	protected Handler<RoutingContext> fromComponent(Function<FormsPluginComponent, AbstractFormsHandler> handler) {
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
				} else if (t instanceof MeshPluginException meshException) {
					context.response()
						.setStatusCode(meshException.getStatusCode())
						.setStatusMessage(MiscUtil.getFirstLine(meshException.getMessage()))
						.end(new JsonObject().put("error", meshException.getLocalizedMessage()).encodePrettily());
				} else {
					context.response().setStatusCode(500).setStatusMessage("Internal Server Error").end();
				}
			}).onErrorComplete().subscribe();
		};
	}
}
