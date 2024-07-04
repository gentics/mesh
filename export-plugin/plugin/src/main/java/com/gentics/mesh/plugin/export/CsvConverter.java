package com.gentics.mesh.plugin.export;

import com.gentics.mesh.plugin.export.model.*;
import com.gentics.mesh.plugin.export.util.Utils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CsvConverter {

	private static final Logger log = LoggerFactory.getLogger(CsvConverter.class);

	private static final String REFERENCE_CONTENT_CONTAINER_NAME = "__refs";
	private static final String ROOT_CONTENT_NAME = "$";

	private CsvConverter() {
	}

	public static List<String> transformToCsv(ConvertingOptions options, JsonObject node) {
		log.debug("Transforming Node: {}", node.getString("uuid", "[no uuid available]"));

		CsvMatrix matrix = convertToMatrix(options, node);

		List<String> lines = convertToPrintableMatrix(matrix).stream()
			.map(row -> {
				CsvStringWriter writer = new CsvStringWriter();
				writer.writeRow(row);
				return writer.toString();
			})
			.collect(Collectors.toList());

		return lines;
	}

	public static CsvMatrix convertToMatrix(ConvertingOptions options, JsonObject node) {
		return options.getMapping().stream()
			.map(definition -> convertToDefinitionMatrix(options, definition, node))
			.collect(CsvMatrix::new, CsvMatrix::addAll, CsvMatrix::addAll);
	}

	public static CsvMatrix convertToDefinitionMatrix(ConvertingOptions options, CsvMappingDefinition definition, JsonObject node) {
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
		if (!values.isPresent() || ((values.get().isEmpty()) && options.getMapping().size() > 1)) {
			return generateEmptyMatrix(options, definition);
		}

		CsvMatrix matrix = new CsvMatrix();

		values.get().stream()
			.map(singleValue -> transformToColumnGroup(options, definition, singleValue, node))
			.map(group -> limitGroup(definition, group))
			.forEach(matrix::add);

		return matrix;
	}

	private static ColumnGroup transformToColumnGroup(
		ConvertingOptions options,
		CsvMappingDefinition definition,
		Object data,
		JsonObject node
	) {
		if (data instanceof JsonArray) {
			JsonArray arr = (JsonArray) data;
			return mapMultipleValues(options, definition, arr, node);
		}

		if (definition.getActualType().isMultiColumn()) {
			JsonObject element = (JsonObject) data;
			putRootIntoElement(definition, element, node);

			if (definition.getSubFilter() != null && !definition.getSubFilter().isAllowed(element)) {
				return generateEmptyMatrix(options, definition).get(0);
			}
			return mapMultiColumns(options, definition, element);
		}

		return new ColumnGroup(new Column(mapSingleValue(options, definition, data)));
	}

	private static JsonObject putRootIntoElement(CsvMappingDefinition definition, JsonObject element, JsonObject node) {
		// Clone the node
		JsonObject root = new JsonObject(node.getMap());

		// Remove the elements in the root to prevent recursion
		List<String> path = new ArrayList<>(definition.getPathAsList());
		String last = path.remove(path.size() - 1);
		Optional<Object> found = Utils.getValueByPath(root, path);
		if (found.isPresent()) {
			if (found.get() instanceof JsonObject) {
				((JsonObject) found.get()).remove(last);
			} else if (found.get() instanceof JsonArray) {
				for (Object o : ((JsonArray) found.get())) {
					if (o instanceof JsonObject) {
						((JsonObject) o).remove(last);
					}
				}
			}
		}

		element.put(ROOT_CONTENT_NAME, root);

		return element;
	}

	private static ColumnGroup limitGroup(CsvMappingDefinition definition, ColumnGroup group) {
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

	private static JsonObject prepareNode(JsonObject node, CsvMappingDefinition definition, final List<String> path) {
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

	private static Boolean hasExpandableSubMapping(List<CsvSubMappingDefinition> subMappings) {
		for (CsvSubMappingDefinition mappingDefinition : subMappings) {
			if (mappingDefinition.getListExpand()) {
				return true;
			}
		}
		return false;
	}

	private static void handleSubMappedList(
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

	private static Optional<JsonArray> getValuesFromNode(
		JsonObject node,
		List<String> path,
		CsvMappingDefinition definition
	) {
		Optional<JsonArray> check = getValuesFromObject(node, path);

		if (!check.isPresent()) {
			return check;
		}

		JsonArray values = check.get();

		// Fix the values size
		if (!CsvMappingDataType.LIST.equals(definition.getType()) && values.size() > 1) {
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

	private static CsvMatrix generateEmptyMatrix(ConvertingOptions options, CsvMappingDefinition definition) {
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
		} else if (definition.getType() == CsvMappingDataType.BOOLEAN) {
			matrix.add(new ColumnGroup(new Column()
				.addValue(Utils.mergeMappings(options.getBooleanMapping(), definition.getBooleanMapping()).getNullValue()
			)));
		} else {
			// Single-Column definitions only return a single "empty" column
			matrix.add(new ColumnGroup(new Column().addValue("")));
		}

		return matrix;
	}

	private static Optional<JsonArray> getValuesFromObject(JsonObject root, List<String> path) {
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

	private static ColumnGroup mapMultipleValues(
		ConvertingOptions options,
		CsvMappingDefinition definition,
		JsonArray values,
		JsonObject node
	) {
		if (values.isEmpty() && options.getMapping().size() > 1) {
			return generateEmptyMatrix(options, definition).get(0);
		}

		// When it's a multi-column, then we iterate over each element and generate a column value for it
		// Each column-value has to be evenly applied to all columns, otherwise we get a wrong amount.
		if (definition.getActualType().isMultiColumn()) {
			return mapMultipleValuesToMultiColumn(options, definition, values, node);
		}

		return mapMultipleValuesToSingleColumn(options, definition, values);
	}

	private static ColumnGroup mapMultipleValuesToMultiColumn(
		ConvertingOptions options,
		CsvMappingDefinition definition,
		JsonArray values,
		JsonObject node
	) {
		Stream<JsonObject> valueStream = values.stream()
			.filter(element -> element instanceof JsonObject)
			.map(obj -> putRootIntoElement(definition, (JsonObject) obj, node))
			.filter(obj -> {
				if (definition.getSubFilter() != null) {
					return definition.getSubFilter().isAllowed(obj);
				}
				return true;
			});

		// If the mapping is not a list and should not expand, then we may only
		// use the first value.
		if (!CsvMappingDataType.LIST.equals(definition.getType()) || !definition.getListExpand()) {
			valueStream = valueStream.limit(1);
		}

		List<ColumnGroup> groups = valueStream
			.map(obj -> mapMultiColumns(options, definition, obj))
			.collect(Collectors.toList());

		if (groups.isEmpty()) {
			groups.addAll(generateEmptyMatrix(options, definition));
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

	private static ColumnGroup mapMultipleValuesToSingleColumn(
		ConvertingOptions options,
		CsvMappingDefinition definition,
		JsonArray values
	) {
		List<String> mapped = values.stream()
			.map(singleValue -> mapSingleValue(options, definition, singleValue))
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

	private static ColumnGroup mapMultiColumns(
		ConvertingOptions options,
		CsvMappingDefinition parentMapping,
		JsonObject subNode
	) {
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

			return mapArrayValuesToMultiColumns(options, definition, parentMapping, values);
		}).forEach(group::add);

		return group;
	}

	private static Column mapArrayValuesToMultiColumns(
		ConvertingOptions options,
		CsvSubMappingDefinition definition,
		CsvMappingDefinition parentMapping,
		JsonArray values
	) {
		List<String> mapped = values.stream()
			.map(singleValue -> mapSingleValue(options, definition, singleValue))
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

	private static String mapSingleValue(
		ConvertingOptions options,
		AbstractCsvMappingDefinition definition,
		Object value
	) {
		if (value == null) {
			return "";
		}

		final CsvMappingDataType type = definition.getActualType();

		try {
			switch (type) {
				case BOOLEAN:
					Boolean bool = null;

					if (value instanceof Boolean) {
						bool = (Boolean) value;
					} else {
						if ("true".equalsIgnoreCase(value.toString()) || "1".equalsIgnoreCase(value.toString())) {
							bool = true;
						} else if ("false".equalsIgnoreCase(value.toString()) || "0".equalsIgnoreCase(value.toString())) {
							bool = false;
						}
					}

					return Utils.mergeMappings(options.getBooleanMapping(), definition.getBooleanMapping()).getStringValue(bool);
				case NUMBER:
					return mapNumberValue(options, value, definition);
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
							for (int groupIndex = 0; groupIndex <= matcher.groupCount(); groupIndex++) {
								String tmp = matcher.group(groupIndex);
								out = out.replaceAll("%" + groupIndex + "%", tmp);
							}
						} catch (IndexOutOfBoundsException ignored) { }
					}

					return out;
				case DATE:
					Instant date;
					if (value instanceof Instant) {
						date = (Instant) value;
					} else {
						date = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(value.toString()));
					}
					return mapDateValue(options, date, definition);
				case CONFIG_VALUE:
					return mapValueFromConfig(options, definition.getConfigKey(), value.toString());
			}
		} catch (Exception e) {
			log.debug("Error while mapping", e);
			// do nothing
		}

		return "";
	}

	private static String mapNumberValue(ConvertingOptions options, Object value, AbstractCsvMappingDefinition definition) {
		if (!(value instanceof Number)) {
			if (definition.getDefaultValue() == null || !(definition.getDefaultValue() instanceof Number)) {
				return "0";
			}
			value = definition.getDefaultValue();
		}

		Number num = (Number) value;
		NumberFormat formatter = definition.getNumberFormatter();

		if (formatter == null) {
			formatter = options.getNumberFormatter();
		}

		return formatter.format(num);
	}

	private static String mapDateValue(ConvertingOptions options, Instant date, AbstractCsvMappingDefinition definition) {
		try {
			DateTimeFormatter formatter = options.getDateFormatter();

			try {
				if (definition.getDateFormat() != null) {
					formatter = DateTimeFormatter.ofPattern(definition.getDateFormat());
				}
				if (definition.getDateLocale() != null) {
					formatter = formatter.withLocale(Locale.forLanguageTag(definition.getDateLocale()));
				}
				if (definition.getDateZone() != null) {
					formatter = formatter.withZone(ZoneId.of(definition.getDateZone()));
				}
			} catch (Exception e) {
				log.warn("Error while creating custom column date formatter", e);
			}

			return formatter.format(date);
		} catch (Exception e) {
			// do nothing
			return "";
		}
	}

	private static String mapValueFromConfig(
		ConvertingOptions options,
		String configKey,
		String id
	) {
		if (StringUtils.trimToEmpty(id).isEmpty()) {
			return "";
		}

		String value = "";

		if (options.getConfigValues().containsKey(configKey)) {
			Map<String, String> values = options.getConfigValues().get(configKey);
			if (values.containsKey(id)) {
				value = values.get(id);
			}
		}

		return value;
	}

	public static List<List<String>> convertToPrintableMatrix(CsvMatrix matrix) {
		List<List<String>> printMatrix = new ArrayList<>();

		for (ColumnGroup group : matrix) {
			appendColumnGroupToPrintMatrix(printMatrix, group);
		}

		printMatrix = printMatrix
			.stream()
			.filter(list -> !list.stream()
				.allMatch(col -> col == null || col.trim().isEmpty())
			)
			.collect(Collectors.toList());

		return printMatrix;
	}

	private static void appendColumnGroupToPrintMatrix(List<List<String>> printMatrix, ColumnGroup group) {
		int addSize = group.get(0).getValues().size();
		int actuallyAdded = 0;
		List<List<String>> copy = new ArrayList<>();

		if (addSize > 1) {
			// Create a copy of the print-matrix for later duplication
			for (List<String> row : printMatrix) {
				copy.add(new ArrayList<>(row));
			}
		}

		for (int i = 0; i < addSize; i++) {
			boolean isLast = i + 1 >= addSize;
			List<String> groupRow = new ArrayList<>();

			for (Column column : group) {
				try {
					groupRow.add(column.getValues().get(i));
				} catch (IndexOutOfBoundsException e) {
					log.warn("Attempted to access invalid index " + i + " in following values: " + column);
					groupRow.add("");
				}
			}

			if (printMatrix.isEmpty()) {
				// If it is the very first row, create a new row in the matrix

				// Filtering out empty rows, unless it is the last and all other previous lines were empty as well.
				// This ensures that it renders empty columns for padding correctly.
				if (!(isLast && actuallyAdded < 1) && groupRow.stream().allMatch(s -> s == null || s.trim().isEmpty())) {
					continue;
				}

				printMatrix.add(new ArrayList<>(groupRow));
				actuallyAdded++;
			} else if (i == 0) {
				// if it's the first element, then append it to each existing row
				for (List<String> row : printMatrix) {
					row.addAll(groupRow);
				}
				actuallyAdded++;
			} else if (copy.isEmpty()) {
				// If copy is empty, we can't append the row to anything yet
				// Therefore simply append it as a new row

				// Filtering out empty rows, unless it is the last and all other previous lines were empty as well.
				// This ensures that it renders empty columns for padding correctly.
				if (!(isLast && actuallyAdded < 1) && groupRow.stream().allMatch(s -> s == null || s.trim().isEmpty())) {
					continue;
				}
				printMatrix.add(new ArrayList<>(groupRow));
				actuallyAdded++;
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
				actuallyAdded++;
			}
		}
	}
}
