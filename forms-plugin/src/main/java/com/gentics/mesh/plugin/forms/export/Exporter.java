package com.gentics.mesh.plugin.forms.export;

import com.gentics.mesh.core.rest.graphql.GraphQLError;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.FormsPlugin;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	/** The UUID of the form to be exported. */
	protected final String uuid;

	/** The schema name of the form to be exported. */
	protected final String schemaName;

	/** The list of fields making up the columns in the export. */
	protected List<String> fieldNames = null;

	/**
	 * Map of field names -> labels
	 */
	protected Map<String, String> fieldLabels = new HashMap<>();

	/**
	 * Default constructor.
	 *
	 * @param adminClient mesh rest client
	 * @param context
	 *            The routing context of the export request
	 * @param uuid
	 *            The UUID of the form to be exported
	 * @param schemaName
	 *            The schame name of the form to be exported
	 */
	protected Exporter(MeshRestClient adminClient, PluginContext context, String uuid, String schemaName) {
		this.adminClient = adminClient;
		this.context = context;
		this.uuid = uuid;
		this.schemaName = schemaName;
	}

	/**
	 * The actual export method.
	 *
	 * This method must make sure that either one of the routing contexts <code>fail()</code> or <code>next()</code> methods is called, or the request is ended.
	 *
	 * The GraphQL response containing the data to export. The interesting data can be extracted to a JSON object by {@link #extractData(GraphQLResponse)}.
	 *
	 * @param response
	 *            The response for the GraphQL query
	 * @return single export data
	 */
	abstract protected Single<String> internalExport(GraphQLResponse response);

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
	 * Get the value for the <code>Content-Dispostion</code> HTTP header.
	 *
	 * The method constructs a filename from the form name and the current date and time.
	 *
	 * @param formName
	 *            The name of the form
	 * @param extension
	 *            The file extension to be used (if it does not start with a '.', it will be prepended)
	 *
	 * @return A Content-Disposition value with a filename made up from the form name and the current date
	 */
	protected String getContentDisposition(String formName, String extension) {
		if (!extension.startsWith(".")) {
			extension = "." + extension;
		}

		return "attachment; filename=forms-export_"
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
	public Single<String> export(Form form, boolean publishedOnly) {
		Map<String, String> renderCtx = new HashMap<>();

		renderCtx.put("schemaName", schemaName);
		renderCtx.put("fieldNames", String.join("\n", form.getReportableElementNamesForGraphQL()));

		fieldNames = new ArrayList<>(form.getReportableElementNamesForGraphQL());
		fieldNames.remove("errors");

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
	 */
	public void setFieldLabels(Map<String, String> fieldLabels) {
		this.fieldLabels = fieldLabels;
	}

	/**
	 * Creates a new exporter.
	 *
	 * The export format is specified in the query string, and the default is {@link CsvExporter}.
	 *
	 * @param configuration configuration
	 * @param adminClient mesh rest client
	 * @param context
	 *            The routing context of the export request
	 * @param uuid
	 *            The UUID of the form to be exported
	 * @param schemaName
	 *            The schema name of the form to be exported
	 *
	 * @return An <code>Exporter</code> instance for the respective export format.
	 */
	public static Exporter create(FormsConfig configuration, MeshRestClient adminClient, PluginContext context, String uuid, String schemaName) {
		ExportFormat format = ReflectionUtils.getEnumValue(
			ExportFormat.class,
			context.request().getParam(PARAM_FORMAT),
			configuration.getDefaultExportFormat());

		log.debug("Using export format {}", format.name());

		switch (format) {
		case CSV:
			return new CsvExporter(adminClient, context, uuid, schemaName);

		default:
			log.error("Export format {} is not implemented", format.name());

			return null;
		}
	}
}
