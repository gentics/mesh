package com.gentics.mesh.plugin.forms.export;

import com.gentics.mesh.core.rest.graphql.GraphQLError;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.config.ExportOptions;
import com.gentics.mesh.plugin.forms.util.ReflectionUtils;
import com.gentics.mesh.rest.client.MeshRestClient;

import com.google.common.base.Strings;
import com.google.common.net.HttpHeaders;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVFormat.Predefined;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 * Export form generator data in CSV format.
 *
 * The actual form of the generated CSV file can be specified in the {@link Exporter#PARAM_OPTIONS} query parameter. Supported values are the ones supported by
 * {@link CSVFormat} (specified in {@link Predefined}).
 *
 * The headers in the first line of the generated CSV contain a concatenation of the corresponding field name and field label separated by the value specified
 * in the {@link CsvExporter#PARAM_DELIMITER} query parameter. If no value is specified, {@link CsvExporter#DEFAULT_HEADER_DELIMITER} is used.
 */
public class CsvExporter extends Exporter {

	private static final String PARAM_DELIMITER = "headerDelimiter";

	/**
	 * Default delimiter to separate field name from field label.
	 * Needed, since exported CSV has to be parsable by field name as well as human readable.
	 */
	private final static String DEFAULT_HEADER_DELIMITER = "|||";

	private static final Logger log = LoggerFactory.getLogger(CsvExporter.class);

	/** The CSV format options without the header fields. */
	private final CSVFormat baseFormat;

	/**
	 * Default constructor.
	 *
	 * The query parameter {@link Exporter#PARAM_OPTIONS} is examined for CSV format options. When no (valid) option is present, the default CSV format is used.
	 *
	 * @param adminClient mesh rest client
	 * @param context The routing context of the export request
	 * @param uuid The UUID of the form to be exported
	 * @param schemaName The schema name of the form to be exported
	 * @param options The export options
	 * @param baseUrl The base URL for file downloads
	 */
	protected CsvExporter(MeshRestClient adminClient, PluginContext context, String uuid, String schemaName, ExportOptions options, String baseUrl) {
		super(adminClient, context, uuid, schemaName, options, baseUrl);

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

		StringBuilderWriter writer = new StringBuilderWriter();
		// UTF-8 BOM to detect the correct encoding
		writer.write("\ufeff");

		CSVFormat format = baseFormat
			.withHeader(getHeaders(dataFields))
			.withQuote('"')
			.withIgnoreEmptyLines(false)
			.withAllowMissingColumnNames();
		List<List<String>> records = dataFields.stream()
			.filter(e -> e instanceof JsonObject)
			.map(e -> getExportedValues(((JsonObject) e)))
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
	 * @param dataFields The exported data
	 * @return The labels of the exported fields as a String array
	 */
	private String[] getHeaders(JsonArray dataFields) {
		if (dataFields == null || dataFields.isEmpty()) {
			return new String[0];
		}

		String headerDelimiter = context.request().getParam(PARAM_DELIMITER);
		if (headerDelimiter == null) {
			headerDelimiter = CsvExporter.DEFAULT_HEADER_DELIMITER;
		}

		List<String> headers = new ArrayList<>();

		if (options.isIncludeUuid()) {
			headers.add("uuid");
		}

		if (options.isIncludeCreationTimestamp()) {
			headers.add("created");
		}

		for (String fieldName : fieldNames) {
			String fieldLabel = fieldLabels.getOrDefault(fieldName, fieldName);
			String annotatedFieldLabel = this.normalizeValue(fieldName.equals(fieldLabel)
					? fieldName
					: options.isIncludeNameFirst() ? fieldName + headerDelimiter + fieldLabel : fieldLabel + headerDelimiter + fieldName);

			headers.add(annotatedFieldLabel);

			if (binaryFields.contains(fieldName)) {
				headers.add(annotatedFieldLabel + " (filename)");
			}
		}

		return headers.toArray(new String[0]);
	}

	/**
	 * Extract the field values from the given record into a list of Strings.
	 *
	 * <p>
	 *     When the {@code options} specify {@link ExportOptions#isIncludeUuid() includeUuid}
	 *     or {@link ExportOptions#isIncludeCreationTimestamp() includeCreationTimestamp}
	 *     the data nodes UUID and if available language code or creation
	 *     timestamp will be added respectively.
	 * </p>
	 *
	 * @param data The loaded form data node to extract fields from
	 *
	 * @return The field values of the specified record in a list of Strings.
	 */
	private List<String> getExportedValues(JsonObject data) {
		JsonObject record = data == null ? null : data.getJsonObject("fields");

		if (record == null) {
			return null;
		}

		List<String> ret = new ArrayList<>();

		if (options.isIncludeUuid()) {
			ret.add(data.getString("uuid"));
		}

		if (options.isIncludeCreationTimestamp()) {
			String timestampValue = data.getString("created");
			String timestampFormat = options.timestampFormat();
			if (!StringUtils.isBlank(timestampValue) && !StringUtils.isBlank(timestampFormat) && !StringUtils.equals(timestampFormat, ExportOptions.DEFAULT_TIMESTAMP_FORMAT)) {
				try {
					// strip the Z in the end
					LocalDateTime date = LocalDateTime.parse(timestampValue.substring(0, timestampValue.length() - 1), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern(timestampFormat);
					timestampValue = formatter.format(date); 
				} catch (Throwable e) {
					log.warn("Could not parse date", e);
				}
			}

			ret.add(timestampValue);
		}

		for (String fieldName : fieldNames) {
			Object value = record.getValue(fieldName, "");

			// special handling for binary fields
			if (value instanceof JsonObject) {
				value = ((JsonObject) value).getString("fileName", fieldName);

				ret.add(String.format(
					"%s/%s/binary/%s",
					baseUrl,
					data.getString("uuid"),
					fieldName));
			} else if (value == null && binaryFields.contains(fieldName)) {
				ret.add("");
			}

			ret.add(value == null ? "" : this.normalizeValue(String.valueOf(value)));
		}

		return ret;
	}

	private String normalizeValue(String value) {
		return Strings.nullToEmpty(value)
			.replaceAll("\r?\n", " ");
	}
}
