package com.gentics.mesh.plugin.forms.export;

import com.gentics.mesh.core.rest.graphql.GraphQLError;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.FormsPlugin;
import com.gentics.mesh.plugin.forms.config.ExportOptions;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.util.ReflectionUtils;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base class for exporting submitted form generator data.
 *
 * This class handles creation of concrete exporters for a specific format (via {@link #create} extraction of necessary data from the
 * <code>GraphQLResponse</code>.
 *
 * The actual export data has to be provided by subclasses in {@link #internalExport}.
 *
 * The export format can be specified via the query parameter {@link #PARAM_FORMAT}. Further options for the concrete exporter can be passed via the query
 * parameter {@link #PARAM_OPTIONS}.
 */
public abstract class Exporter {

	public static final String PARAM_FORMAT = "format";
	public static final String PARAM_OPTIONS = "options";

	private static final Logger log = LoggerFactory.getLogger(Exporter.class);

	/** mesh rest client **/
	protected final MeshRestClient adminClient;

	/** The routing context of the export request. */
	protected final PluginContext context;

	/** The plugins storage directory. */
	protected final File storageDir;

	/** The UUID of the form to be exported. */
	protected final String uuid;

	/** The schema name of the form to be exported. */
	protected final String schemaName;

	/** The export options. */
	protected final ExportOptions options;

	/** Base URL for file downloads. */
	protected final String baseUrl;

	/** The names of binary fields in case they need special treatment. */
	protected final Set<String> binaryFields = new HashSet<>();

	/** The list of fields making up the columns in the export. */
	protected List<String> fieldNames = new ArrayList<>();

	/**
	 * Map of field names -> labels
	 */
	protected Map<String, String> fieldLabels = new HashMap<>();

	/**
	 * Default constructor.
	 *
	 * @param adminClient mesh rest client
	 * @param context The routing context of the export request
	 * @param storageDir The plugins storage directory
	 * @param uuid The UUID of the form to be exported
	 * @param schemaName The schame name of the form to be exported
	 * @param options The export options
	 * @param baseUrl The base URL for file downloads
	 */
	protected Exporter(MeshRestClient adminClient, PluginContext context, File storageDir, String uuid, String schemaName, ExportOptions options, String baseUrl) {
		this.adminClient = adminClient;
		this.context = context;
		this.storageDir = storageDir;
		this.uuid = uuid;
		this.schemaName = schemaName;
		this.options = options;
		this.baseUrl = baseUrl;
	}

	/**
	 * Get the export format.
	 * @return The export format.
	 */
	public abstract ExportFormat format();

	/**
	 * The actual export method.
	 *
	 * This method must make sure that either one of the routing contexts <code>fail()</code> or <code>next()</code> methods is called, or the request is ended.
	 *
	 * The GraphQL response containing the data to export. The interesting data can be extracted to a JSON object by {@link #extractData(GraphQLResponse)}.
	 *
	 * @param response The response for the GraphQL query
	 * @return single export data
	 */
	abstract protected Single<ExportData> internalExport(GraphQLResponse response);

	/**
	 * Extracts the schema fields and the children of the exported form.
	 *
	 * @param response
	 *            The result of the GraphQL query
	 *
	 * @return A JSON object with the field <code>schema</code> which contains the list of names and labels of form fields, and the field <code>data</code>
	 *         which contains the list of submitted data for the form. The display name of the form is in the field <code>formName</code>.
	 */
	protected JsonObject extractData(GraphQLResponse response) {
		JsonObject data = response.getData();
		JsonObject node = data == null ? null : data.getJsonObject("node");

		if (node == null) {
			log.error("Could not load node {}", uuid);

			List<GraphQLError> errors = response.getErrors();

			if (!errors.isEmpty()) {
				log.error("GraphQL errors:");
				for (GraphQLError error : response.getErrors()) {
					log.error("  {}: {}", error.getElementId(), error.getMessage());
				}
			}

			return null;
		}

		JsonObject emptyObject = new JsonObject();
		JsonArray emptyArray = new JsonArray();

		JsonObject schema = data.getJsonObject("schema");

		if (schema == null || schema.isEmpty()) {
			log.error("Schema {} not found", schemaName);

			return null;
		}

		return new JsonObject()
			.put("formName", node.getString("displayName", uuid))
			.put("data", node.getJsonObject("children", emptyObject).getJsonArray("elements", emptyArray))
			.put("schema", schema);
	}

	/**
	 * Get the download filename for the export.
	 *
	 * The method constructs a filename from the form name and the current date and time.
	 *
	 * @param formName
	 *            The name of the form
	 * @param extension
	 *            The file extension to be used (if it does not start with a '.', it will be prepended)
	 *
	 * @return The download filename.
	 */
	protected String getDownloadFilename(String formName, String extension) {
		if (!extension.startsWith(".")) {
			extension = "." + extension;
		}

		return "forms-export_"
			+ (formName == null || formName.isEmpty() ? uuid : formName)
			+ "_"
			+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmm"))
			+ extension;
	}

	/**
	 * Export data submitted for the form.
	 *
	 * This method will build the respective GraphQL query and pass the result to {@link #internalExport} which is responsible for creating a response in the
	 * desired export format.
	 *
	 * @param form The form to export
	 * @param publishedOnly Whether to export only published (i.e. completed) forms or all form data
	 * @return single export
	 */
	public Single<ExportData> export(Form form, boolean publishedOnly) {
		Map<String, String> renderCtx = new HashMap<>();

		renderCtx.put("schemaName", schemaName);
		renderCtx.put("fieldNames", String.join("\n", form.getReportableElementNamesForGraphQL()));

		binaryFields.clear();
		fieldNames.clear();

		form.getReportableElementsInfo().forEach((name, isBinary) -> {
			fieldNames.add(name);

			if (isBinary) {
				binaryFields.add(name);
			}
		});

		String query;

		try {
			String rawQuery = IOUtils.toString(
				FormsPlugin.class.getResource("loadFormData.graphql"),
				"UTF-8");

			query = new Handlebars().compileInline(rawQuery).apply(Context.newContext(renderCtx));
		} catch (IOException e) {
			log.error("Could not create export GraphQL query: ", e.getMessage());
			return Single.error(e);
		}

		GraphQLRequest request = new GraphQLRequest()
			.setQuery(query)
			.setVariables(new JsonObject().put("uuid", uuid).put("perPage", Integer.MAX_VALUE).put("page", 1));
		VersioningParameters params = new VersioningParametersImpl();

		if (publishedOnly) {
			params.published();
		} else {
			params.draft();
		}

		return adminClient
			.graphql(context.project().getString("name"), request, params)
			.toSingle()
			.flatMap(graphQlResponse -> internalExport(graphQlResponse));
	}

	/**
	 * Set the field labels
	 * @param fieldLabels field labels
	 * @return A reference to this exporter for fluent API access
	 */
	public Exporter setFieldLabels(Map<String, String> fieldLabels) {
		this.fieldLabels = fieldLabels;

		return this;
	}

	/**
	 * Creates a new exporter.
	 *
	 * The export format is specified in the query string, and the default is {@link CsvExporter}.
	 *
	 * @param configuration configuration
	 * @param adminClient mesh rest client
	 * @param context The routing context of the export request
	 * @param storageDir The plugins storage directory
	 * @param uuid The UUID of the form to be exported
	 * @param schemaName The schema name of the form to be exported
	 * @param options The export options
	 * @param baseUrl The base URL for file downloads
	 *
	 * @return An <code>Exporter</code> instance for the respective export format.
	 */
	public static Exporter create(FormsConfig configuration, MeshRestClient adminClient, PluginContext context, File storageDir, String uuid, String schemaName, ExportOptions options, String baseUrl) {
		ExportFormat format = ReflectionUtils.getEnumValue(
			ExportFormat.class,
			context.request().getParam(PARAM_FORMAT),
			configuration.getDefaultExportFormat());

		log.debug("Using export format {}", format.name());

		switch (format) {
		case CSV:
			return new CsvExporter(adminClient, context, storageDir, uuid, schemaName, options, baseUrl);

		default:
			log.error("Export format {} is not implemented", format.name());

			return null;
		}
	}
}
