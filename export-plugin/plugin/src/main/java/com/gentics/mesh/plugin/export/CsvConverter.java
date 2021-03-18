package com.gentics.mesh.plugin.export;

import com.gentics.mesh.plugin.export.model.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CsvConverter {

	private static final Logger log = LoggerFactory.getLogger(CsvConverter.class);

	private static final String REFERENCE_CONTENT_CONTAINER_NAME = "__refs";
	private static final DateTimeFormatter DEFAULT_DATE_FORMAT = DateTimeFormatter
		.ofPattern("yyyy-MM-dd HH:mm:ss")
		.withLocale(Locale.GERMAN)
		.withZone(ZoneId.of("Europe/Vienna"));
	private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.GERMANY);

	// Additional static setup
	static {
		NUMBER_FORMAT.setMinimumFractionDigits(0);
		NUMBER_FORMAT.setMaximumFractionDigits(10);
	}

	private Map<String, Map<String, String>> configValues = new HashMap<>();
	private List<CsvMappingDefinition> mapping = new ArrayList<>();

	public CsvConverter() {
	}

	public List<String> transformToCsv(JsonObject node) {
		log.debug("Transforming Node: {}", node.getString("uuid", "[no uuid available]"));

		CsvMatrix matrix = this.mapping.stream()
			.map(definition -> createMatrix(definition, node))
			.collect(CsvMatrix::new, CsvMatrix::addAll, CsvMatrix::addAll);

		List<String> lines = createPrintableMatrix(matrix).stream()
			.map(row -> {
				CsvStringWriter writer = new CsvStringWriter();
				writer.writeRow(row);
				return writer.toString();
			})
			.collect(Collectors.toList());

		return lines;
	}

	private CsvMatrix createMatrix(CsvMappingDefinition definition, JsonObject node) {
		List<String> path = definition.getPathAsList();

		// In case it's a reference type, then rewrite the path to the correct one, since
		// the reference is getting loaded into an own encapsulated area.
		if (definition.getType() == CsvMappingDataType.FOREIGN_REFERENCE
			|| definition.getListType() == CsvMappingDataType.FOREIGN_REFERENCE) {
			path = Arrays.asList(REFERENCE_CONTENT_CONTAINER_NAME, definition.getPath());
		}

		JsonObject preparedNode = prepareNode(node, definition, path);
		Optional<JsonArray> values = getValuesFromNode(preparedNode, path, definition);

		// Set the default value if nothing could be found
		if ((!values.isPresent() || values.get().isEmpty()) && definition.getDefaultValue() != null) {
			Object val = definition.getDefaultValue();
			if (val instanceof JsonArray) {
				values = Optional.of((JsonArray) val);
			} else {
				values = Optional.of(new JsonArray().add(val));
			}
		}

		// Check if the values are present and not empty. If they are, we have to return
		// empty columns to not screw up the layout.
		if (!values.isPresent() || ((values.get().isEmpty()) && this.mapping.size() > 1)) {
			log.debug("No values present, generating empty cells instead.");
			return generateEmptyMatrix(definition);
		}

		log.debug("Found values to export: {}", values.get());

		CsvMatrix matrix = new CsvMatrix();

		values.get().stream()
			.map(singleValue -> transformToColumnGroup(definition, singleValue))
			.map(group -> limitGroup(definition, group))
			.forEach(matrix::add);

		return matrix;
	}

	private ColumnGroup transformToColumnGroup(CsvMappingDefinition definition, Object data) {
		if (data instanceof JsonArray) {
			JsonArray arr = (JsonArray) data;
			return mapMultipleValues(definition, arr);
		}

		if (definition.getActualType().isMultiColumn()) {
			JsonObject element = (JsonObject) data;
			if (definition.getSubFilter() != null && !definition.getSubFilter().isAllowed(element)) {
				return generateEmptyMatrix(definition).get(0);
			}
			return mapMultiColumns(definition, element);
		}

		return new ColumnGroup(new Column(mapSingleValue(definition, data)));
	}

	private ColumnGroup limitGroup(CsvMappingDefinition definition, ColumnGroup group) {
		int valueSize = group.get(0).getValues().size();

		if (definition.getListExpand() &&
			definition.getListExpandLimit() != null &&
			valueSize > definition.getListExpandLimit()
		) {
			ColumnGroup newGroup = new ColumnGroup();
			for (Column col : group) {
				Column newCol = new Column();
				for (int i = 0; i < valueSize && i < definition.getListExpandLimit(); i++) {
					newCol.addValue(col.getValues().get(i));
				}
				newGroup.add(newCol);
			}
			return newGroup;
		}

		return group;
	}

	private JsonObject prepareNode(JsonObject node, CsvMappingDefinition definition, final List<String> path) {
		JsonObject preparedNode = node;

		if (definition.getSubMapping() != null && !definition.getSubMapping().isEmpty() && hasExpandableSubMapping(
			definition.getSubMapping())) {
			JsonObject nodeCopy = node.copy();
			JsonArray newValues = new JsonArray();

			for (CsvSubMappingDefinition subMappingDefinition : definition.getSubMapping()) {
				JsonObject currentFields = node.getJsonObject(path.get(0), new JsonObject());
				if (subMappingDefinition.getListExpand() && !(currentFields.isEmpty())) {

					JsonArray currentValues = currentFields.getJsonArray(path.get(1), new JsonArray());
					if (currentValues != null) {
						currentValues.forEach(currentSubMappedFields ->
							handleSubMappedList(
								newValues,
								subMappingDefinition,
								currentSubMappedFields
							)
						);
					}
				}
			}

			JsonObject nodeFields = nodeCopy.getJsonObject(path.get(0));
			JsonArray originalValues = nodeFields.getJsonArray(path.get(1), new JsonArray());

			if (originalValues != null) {
				originalValues.clear().addAll(newValues);
			}
			preparedNode = nodeCopy;
		}

		return preparedNode;
	}

	private Boolean hasExpandableSubMapping(List<CsvSubMappingDefinition> subMappings) {
		for (CsvSubMappingDefinition mappingDefinition : subMappings) {
			if (mappingDefinition.getListExpand()) {
				return true;
			}
		}
		return false;
	}

	private void handleSubMappedList(
		JsonArray newValues,
		CsvSubMappingDefinition subMappingDefinition,
		Object currentSubMappedFields
	) {
		if (!(currentSubMappedFields instanceof JsonObject)) {
			return;
		}

		JsonArray valuesToExpand = ((JsonObject) currentSubMappedFields).getJsonArray(
			subMappingDefinition.getPath(), new JsonArray());
		if (valuesToExpand == null) {
			valuesToExpand = new JsonArray();
		}

		if (valuesToExpand.isEmpty()) {
			JsonObject expandedFieldWithOneListItem = ((JsonObject) currentSubMappedFields).copy();
			newValues.add(expandedFieldWithOneListItem);
		}

		for (Object subMappedListValue : valuesToExpand.getList()) {
			if (subMappedListValue instanceof String) {

				JsonObject expandedFieldWithOneListItem = ((JsonObject) currentSubMappedFields).copy();
				List<String> oneItemList = new ArrayList<>();
				oneItemList.add((String) subMappedListValue);
				expandedFieldWithOneListItem.put(subMappingDefinition.getPath(), new JsonArray(oneItemList));
				newValues.add(expandedFieldWithOneListItem);
			}
		}
	}

	private Optional<JsonArray> getValuesFromNode(
		JsonObject node,
		List<String> path,
		CsvMappingDefinition definition
	) {
		Optional<JsonArray> check = getValuesFromObject(node, path);

		if (!check.isPresent()) {
			return check;
		}

		JsonArray values = check.get();

		// Fix the values size if it's not allowed to be expanded and it's a multi-column type
		if ((!definition.getListExpand() || definition.getType() != CsvMappingDataType.LIST) &&
			definition.getActualType().isMultiColumn() &&
			values.size() > 1) {
			values = new JsonArray().add(values.getValue(0));
		}

		// In case that the type is a list, we have to wrap it again.
		if (definition.getType() == CsvMappingDataType.LIST) {
			values = new JsonArray().add(values);
		}

		if (definition.getFilter() != null) {
			JsonArray filtered = new JsonArray();
			values.forEach(value -> {
				if (definition.getFilter().isAllowed(node)) {
					filtered.add(value);
				}
			});
			values = filtered;
		}

		return Optional.of(values);
	}

	private CsvMatrix generateEmptyMatrix(CsvMappingDefinition definition) {
		CsvMatrix matrix = new CsvMatrix();

		if (definition.getActualType().isMultiColumn()) {
			// For Multi-Columns we have to create as many empty columns as the sub-mapping defines
			List<CsvSubMappingDefinition> subMapping = definition.getSubMapping();

			if (subMapping != null && !subMapping.isEmpty()) {
				ColumnGroup tmp = new ColumnGroup();
				for (int i = 0; i < subMapping.size(); i++) {
					tmp.add(new Column().addValue(""));
				}

				matrix.add(tmp);
			}
		} else {
			// Single-Column definitions only return a single "empty" column
			matrix.add(new ColumnGroup(new Column().addValue("")));
		}

		return matrix;
	}

	private Optional<JsonArray> getValuesFromObject(JsonObject root, List<String> path) {
		for (int i = 0; i < path.size() - 1; i++) {
			final int currentIndex = i;
			Object obj = root.getValue(path.get(i));

			if (obj == null) {
				return Optional.empty();
			}

			if (obj instanceof JsonArray) {
				JsonArray values = (JsonArray) obj;
				JsonArray returnArray = new JsonArray();

				values.forEach(singleValue -> {
					if (singleValue instanceof JsonObject) {
						Optional<JsonArray> tmp = getValuesFromObject(
							(JsonObject) singleValue,
							path.subList(currentIndex + 1, path.size())
						);
						tmp.ifPresent(returnArray::addAll);
					}
				});

				return returnArray.isEmpty() ? Optional.empty() : Optional.of(returnArray);
			}

			if (!(obj instanceof JsonObject)) {
				return Optional.empty();
			}

			root = (JsonObject) obj;
		}

		Object obj = root.getValue(path.get(path.size() - 1));
		if (obj == null) {
			return Optional.empty();
		}

		return Optional.of(obj instanceof JsonArray ? (JsonArray) obj : new JsonArray().add(obj));
	}

	private ColumnGroup mapMultipleValues(CsvMappingDefinition definition, JsonArray values) {
		if (values.isEmpty() && this.mapping.size() > 1) {
			return generateEmptyMatrix(definition).get(0);
		}

		// When it's a multi-column, then we iterate over each element and generate a column value for it
		// Each column-value has to be evenly applied to all columns, otherwise we get a wrong amount.
		if (definition.getActualType().isMultiColumn()) {
			return mapMultipleValuesToMultiColumn(definition, values);
		}

		return mapMultipleValuesToSingleColumn(definition, values);
	}

	private ColumnGroup mapMultipleValuesToMultiColumn(CsvMappingDefinition definition, JsonArray values) {
		List<ColumnGroup> groups = values.stream()
			.map(obj -> (JsonObject) obj)
			.filter(obj -> {
				if (definition.getSubFilter() != null) {
					return definition.getSubFilter().isAllowed(obj);
				}
				return true;
			})
			.map(obj -> mapMultiColumns(definition, obj))
			.collect(Collectors.toList());

		if (groups.isEmpty()) {
			groups.addAll(generateEmptyMatrix(definition));
		}

		ColumnGroup output = new ColumnGroup();

		for (ColumnGroup toMerge : groups) {
			if (output.isEmpty()) {
				toMerge.forEach(columnList -> output.add(new Column(columnList.getValues())));
			} else {
				for (int i = 0; i < toMerge.size(); i++) {
					output.get(i).addAll(toMerge.get(i).getValues());
				}
			}
		}

		return output;
	}

	private ColumnGroup mapMultipleValuesToSingleColumn(CsvMappingDefinition definition, JsonArray values) {
		List<String> mapped = values.stream()
			.map(singleValue -> mapSingleValue(definition, singleValue))
			.filter(str -> str != null && !str.trim().isEmpty())
			.collect(Collectors.toList());

		// Requires at least one element, therefore add an empty cell
		if (mapped.isEmpty()) {
			mapped.add("");
		}

		Column col;

		if (definition.getType() == CsvMappingDataType.LIST && definition.getListExpand()) {
			col = new Column(mapped);
		} else {
			// Create a new ArrayList and add the item
			// Doesn't use Arrays#asList, as those generated lists are not modifiable
			col = new Column(String.join(",", mapped));
		}

		return new ColumnGroup(col);
	}

	private ColumnGroup mapMultiColumns(CsvMappingDefinition parentMapping, JsonObject subNode) {
		ColumnGroup group = new ColumnGroup();

		parentMapping.getSubMapping().stream().map(definition -> {
			List<String> path = definition.getPathAsList();
			Optional<JsonArray> maybeValues = getValuesFromObject(subNode, path);

			if (!maybeValues.isPresent()) {
				return new Column().addValue("");
			}

			JsonArray values = maybeValues.get();

			if (definition.getFilter() != null) {
				JsonArray filtered = new JsonArray();
				values.forEach(value -> {
					if (definition.getFilter().isAllowed(subNode)) {
						filtered.add(value);
					}
				});
				values = filtered;
			}

			return mapArrayValuesToMultiColumns(definition, parentMapping, values);
		}).forEach(group::add);

		return group;
	}

	private Column mapArrayValuesToMultiColumns(
		CsvSubMappingDefinition definition,
		CsvMappingDefinition parentMapping,
		JsonArray values
	) {
		List<String> mapped = values.stream()
			.map(singleValue -> mapSingleValue(definition, singleValue))
			.filter(str -> !str.trim().isEmpty())
			.collect(Collectors.toList());

		// Requires at least one element, therefore add an empty cell
		if (mapped.isEmpty()) {
			mapped.add("");
		}

		// If the definition is not a LIST, then multiple values are prohibited.
		// Therefore joining them with commas.
		// Doesn't use Arrays#asList, as those generated lists are not modifiable
		if ((parentMapping.getType() == CsvMappingDataType.LIST && parentMapping.getListExpand())
			|| definition.getType() != CsvMappingDataType.LIST) {
			return new Column(String.join(",", mapped));
		}

		return new Column(mapped);
	}

	private String mapSingleValue(
		AbstractCsvMappingDefinition definition,
		Object value
	) {
		if (value == null) {
			return "";
		}

		final CsvMappingDataType type =
			definition.getType() == CsvMappingDataType.LIST ? definition.getListType() : definition.getType();

		try {
			switch (type) {
				case BOOLEAN:
					if (value instanceof Boolean) {
						return (Boolean) value ? "1" : "0";
					} else {
						return Boolean.parseBoolean(value.toString()) ? "1" : "0";
					}
				case NUMBER:
					if (value instanceof Number) {
						return NUMBER_FORMAT.format(value);
					}
					return "0";
				case STRING:
					return value.toString();
				case TEMPLATE:
					return definition.getTemplateDefinition()
						.replaceAll(AbstractCsvMappingDefinition.TEMPLATE_REPLACE_TOKEN, value.toString());
				case REGEXP:
					Pattern regexp = Pattern.compile(definition.getRegexpDefinition(), definition.getRegexpFlags());
					Matcher matcher = regexp.matcher(value.toString());
					String out = definition.getRegexpTemplate();

					while (matcher.find()) {
						try {
							for (int groupIndex = 0; groupIndex < matcher.groupCount(); groupIndex++) {
								String tmp = matcher.group(groupIndex);
								out = out.replace("%" + groupIndex + "%", tmp);
							}
						} catch (IndexOutOfBoundsException a) {
						}
					}

					return out;
				case DATE:
					Instant date;
					if (value instanceof Instant) {
						date = (Instant) value;
					} else {
						date = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(value.toString()));
					}
					return mapDateValue(date, definition.getDateFormat());
				case CONFIG_VALUE:
					return mapValueFromConfig(definition.getConfigKey(), value.toString());
			}
		} catch (Exception e) {
			log.debug("Error while mapping", e);
			// do nothing
		}

		return "";
	}

	private String mapDateValue(Instant date, String formatString) {
		try {
			DateTimeFormatter formatter;
			try {
				formatter = DateTimeFormatter
					.ofPattern(formatString)
					.withLocale(Locale.GERMAN)
					.withZone(ZoneId.of("Europe/Vienna"));
			} catch (Exception e) {
				formatter = DEFAULT_DATE_FORMAT;
			}
			return formatter.format(date);
		} catch (Exception e) {
			// do nothing
			return "";
		}
	}

	private String mapValueFromConfig(
		String configKey,
		String id
	) {
		if (StringUtils.trimToEmpty(id).isEmpty()) {
			return "";
		}

		String value = "";

		if (this.configValues.containsKey(configKey)) {
			Map<String, String> options = this.configValues.get(configKey);
			if (options.containsKey(id)) {
				value = options.get(id);
			}
		}

		return value;
	}

	private List<List<String>> createPrintableMatrix(CsvMatrix matrix) {
		List<List<String>> printMatrix = new ArrayList<>();

		for (ColumnGroup group : matrix) {
			appendColumnGroupToPrintMatrix(printMatrix, group);
		}

		return printMatrix;
	}

	private void appendColumnGroupToPrintMatrix(List<List<String>> printMatrix, ColumnGroup group) {
		int addSize = group.get(0).getValues().size();
		List<List<String>> copy = new ArrayList<>();

		if (addSize > 1) {
			// Create a copy of the print-matrix for later duplication
			for (List<String> row : printMatrix) {
				copy.add(new ArrayList<>(row));
			}
		}

		for (int i = 0; i < addSize; i++) {
			List<String> groupRow = new ArrayList<>();

			for (Column column : group) {
				try {
					groupRow.add(column.getValues().get(i));
				} catch (IndexOutOfBoundsException e) {
					log.warn("Attempted to access invalid index " + i + " in following values: " + column);
					groupRow.add("");
				}
			}

			// If it is the first in the matrix, create a new Row
			if (printMatrix.isEmpty()) {
				printMatrix.add(new ArrayList<>(groupRow));
			} else if (i == 0) {
				// if it's the first element, then append it to each row
				for (List<String> row : printMatrix) {
					row.addAll(groupRow);
				}
			} else {
				// For each additional value in the group, a new row has to be created
				// The `copy` is a copy before the new columns were added
				// We now iterate over all copied rows and add the columns to the end,
				// and then append these to the matrix
				// These additional lists are required to not add the rows/columns to the wrong list, as they would
				// be references instead of copies.
				List<List<String>> tmp = new ArrayList<>();
				for (List<String> row : copy) {
					List<String> tmpRow = new ArrayList<>();
					tmpRow.addAll(row);
					tmpRow.addAll(groupRow);
					tmp.add(tmpRow);
				}
				printMatrix.addAll(tmp);
			}
		}
	}

	public Map<String, Map<String, String>> getConfigValues() {
		return configValues;
	}

	public CsvConverter setConfigValues(Map<String, Map<String, String>> configValues) {
		this.configValues = configValues;
		return this;
	}

	public List<CsvMappingDefinition> getMapping() {
		return mapping;
	}

	public CsvConverter setMapping(List<CsvMappingDefinition> mapping) {
		this.mapping = mapping;
		return this;
	}
}
