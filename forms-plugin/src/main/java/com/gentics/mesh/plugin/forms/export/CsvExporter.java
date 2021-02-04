package com.gentics.mesh.plugin.forms.export;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVFormat.Predefined;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.output.StringBuilderWriter;

import com.gentics.mesh.core.rest.graphql.GraphQLError;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.util.ReflectionUtils;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.google.common.net.HttpHeaders;

import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Export form generator data in CSV format.
 *
 * The actual form of the generated CSV file can be specified in the {@link Exporter#PARAM_OPTIONS} query parameter. Supported values are the ones supported by
 * {@link CSVFormat} (specified in {@link Predefined}).
 */
public class CsvExporter extends Exporter {

	private static final Logger log = LoggerFactory.getLogger(CsvExporter.class);

	/** The CSV format options without the header fields. */
	private CSVFormat baseFormat;

	/** The list of fields making up the columns in the resulting CSV file. */
	private List<String> fieldNames = null;

	/**
	 * Default constructor.
	 *
	 * The query parameter {@link Exporter#PARAM_OPTIONS} is examined for CSV format options. When no (valid) option is present, the default CSV format is used.
	 *
	 * @param adminClient mesh rest client
	 * @param context
	 *            The routing context of the export request
	 * @param uuid
	 *            The UUID of the form to be exported
	 * @param schemaName
	 *            The schema name of the form to be exported
	 */
	protected CsvExporter(MeshRestClient adminClient, PluginContext context, String uuid, String schemaName) {
		super(adminClient, context, uuid, schemaName);

		Predefined format = ReflectionUtils.getEnumValue(
			Predefined.class,
			context.request().getParam(PARAM_OPTIONS),
			Predefined.Default);

		if (log.isDebugEnabled()) {
			log.debug("Using CSV format: {}", format.name());
		}

		baseFormat = format.getFormat();
	}

	@Override
	protected Single<String> internalExport(GraphQLResponse response) {
		// log all errors
		if (response.getErrors() != null) {
			for (GraphQLError error : response.getErrors()) {
				log.warn("Error in graphql response {}", error.toJson());
			}
		}

		JsonObject formData = extractData(response);

		if (formData == null) {
			return Single.error(new Exception("Could not extract data from GraphQL response"));
		}

		JsonArray dataFields = formData.getJsonArray("data");

		initFieldNames(dataFields);

		StringBuilderWriter writer = new StringBuilderWriter();
		CSVFormat format = baseFormat.withHeader(getHeaders(formData));
		List<List<String>> records = dataFields.stream()
			.filter(e -> e instanceof JsonObject)
			.map(e -> getFieldValues(((JsonObject) e).getJsonObject("fields")))
			.filter(Objects::nonNull)
			.collect(Collectors.toList());

		try (CSVPrinter csv = new CSVPrinter(writer, format)) {
			csv.printRecords(records);
			csv.flush();
		} catch (IOException e) {
			log.error("Could not create CSV printer: {}", e, e.getMessage());
			return Single.error(e);
		}

		String ret = writer.toString();

		context.response()
			.putHeader(HttpHeaders.CONTENT_TYPE, "text/csv; charset=utf-8")
			.putHeader(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(formData.getString("formName"), "csv"));

		return Single.just(ret);
	}

	/**
	 * Get the headers of the exported field as a String array.
	 *
	 * @param data
	 *            The exported data
	 *
	 * @return The labels of the exported fields as a String array
	 */
	private String[] getHeaders(JsonObject data) {
		if (fieldNames == null) {
			initFieldNames(data.getJsonArray("data"));
		}

		JsonArray dataFields = data.getJsonArray("data");

		if (dataFields == null || dataFields.isEmpty()) {
			return new String[0];
		}

		List<String> headers = new ArrayList<>();

		for (String fieldName : fieldNames) {
			headers.add(uniqueHeaderName(fieldLabels.getOrDefault(fieldName, fieldName), headers));
		}

		return headers.toArray(new String[headers.size()]);
	}

	/**
	 * sometimes fieldnames in FormsPlugin are the same - then we just add a suffix to make the column unique
	 * 
	 * @param headerColumn
	 *            headerColumn name
	 * @param headers
	 *            list with current columns
	 * @return unique headerColumn name
	 */
	private String uniqueHeaderName(String headerColumn, List<String> headers) {
		int i = 1;
		String originColumn = headerColumn;
		while (headers.contains(headerColumn)) {
			headerColumn = originColumn + "_" + i;
			i++;
		}
		return headerColumn;
	}

	/**
	 * Gathers the names of the exported fields in a sorted list.
	 *
	 * @param data
	 *            The exported data
	 */
	private void initFieldNames(JsonArray data) {
		if (data == null || data.isEmpty()) {
			return;
		}

		fieldNames = new ArrayList<>(data.getJsonObject(0).getJsonObject("fields").fieldNames());
		Collections.sort(fieldNames);
	}

	/**
	 * Extract the field values from the given record into a list of Strings.
	 *
	 * @param record
	 *            The record to extract fields from
	 *
	 * @return The field values of the specified record in a list of Strings.
	 */
	private List<String> getFieldValues(JsonObject record) {
		if (record == null) {
			return null;
		}

		List<String> ret = new ArrayList<>(fieldNames.size());

		for (String fieldName : fieldNames) {
			Object value = record.getValue(fieldName, "");

			// special handling for binary fields
			if (value instanceof JsonObject) {
				value = ((JsonObject) value).getValue("fileName", "");
			}

			ret.add(value == null ? "" : String.valueOf(value));
		}

		return ret;
	}
}
