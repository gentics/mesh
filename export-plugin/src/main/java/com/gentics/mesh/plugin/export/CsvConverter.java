package com.gentics.mesh.plugin.export;

import com.gentics.mesh.plugin.export.model.*;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CsvConverter {

    private static Logger log = LoggerFactory.getLogger(CsvConverter.class);

    private static final String RESPONSE_KEY_FIELDS = "fields";
    private static final String CONFIG_KEY_ID = "id";
    private static final String CONFIG_KEY_VALUE = "value";
    private static final String REFERENCE_CONTENT_CONTAINER_NAME = "__refs";
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getNumberInstance(Locale.GERMANY);
    private static final DateTimeFormatter DEFAULT_DATE_FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withLocale(Locale.GERMAN)
            .withZone(ZoneId.of("Europe/Vienna"));

    static {
        NUMBER_FORMAT.setMinimumFractionDigits(0);
        NUMBER_FORMAT.setMaximumFractionDigits(10);
    }

    public CsvConverter() {
    }

    public Single<List<String>> transformToCsv(JsonObject node, CsvRequest request, Single<JsonObject> config) {
        log.debug("Transforming Node: {}", node.getString("uuid", "[no uuid available]"));
        boolean hasOtherColumns = request.getMapping().size() > 1;

        return Observable
                .fromIterable(request.getMapping())
                .concatMap(definition -> this.renderColumns(node, definition, config, hasOtherColumns).toObservable())
                .collectInto(new CsvMatrix(), CsvMatrix::addAll)
                .map(this::createPrintableMatrix)
                .toObservable()
                .map(rows -> {
                    CsvStringWriter writer = new CsvStringWriter();
                    rows.forEach(writer::writeRow);
                    return writer.toString();
                })
                .toList();
    }

    private Single<CsvMatrix> renderColumns(
            JsonObject node, CsvMappingDefinition definition, Single<JsonObject> config, boolean hasOtherColumns
    ) {
        List<String> path = definition.getPathAsList();

        // In case it's a reference type, then rewrite the path to the correct one, since
        // the reference is getting loaded into an own encapsulated area.
        if (definition.getType() == CsvMappingDataType.FOREIGN_REFERENCE
                || definition.getListType() == CsvMappingDataType.FOREIGN_REFERENCE) {
            path = Arrays.asList(REFERENCE_CONTENT_CONTAINER_NAME, definition.getPath());
        }

        final CsvMappingDataType type =
                definition.getType() == CsvMappingDataType.LIST ? definition.getListType() : definition.getType();

        JsonObject preparedNode = prepareNode(node, definition, path);
        Optional<JsonArray> values = this.prepareValues(preparedNode, path, definition, type);

        // Check if the values are present and not empty. If they are, we have to return
        // empty columns to not screw up the layout.
        if (!values.isPresent() || ((values.get().isEmpty()) && hasOtherColumns)) {
            log.debug("No values present, generating empty cells instead.");
            return Single.just(generateEmptyCells(definition, type));
        }

        log.debug("Found values to export: {}", values.get());

        return Observable.fromIterable(values.get()).concatMap(data -> {
            if (data instanceof JsonArray) {
                JsonArray arr = (JsonArray) data;
                return renderValuesToColumns(definition, config, type, arr, hasOtherColumns);
            }

            if (type.isMultiColumn()) {
                JsonObject element = (JsonObject) data;
                if (definition.getSubFilter() != null && !definition.getSubFilter().isAllowed(definition, element)) {
                    return Observable.fromIterable(generateEmptyCells(definition, type));
                }
                return this.mapMultiColumns(element, definition, config).toObservable();
            }

            return this
                    .mapSingleValue(data, definition, config)
                    .map(Arrays::asList)
                    .map(Column::new)
                    .map(ColumnGroup::new)
                    .toObservable();
        }).flatMapSingle(group -> {
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
                return Single.just(newGroup);
            }

            return Single.just(group);
        }).collectInto(new CsvMatrix(), CsvMatrix::add);
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
            JsonArray newValues, CsvSubMappingDefinition subMappingDefinition, Object currentSubMappedFields
    ) {
        if (currentSubMappedFields instanceof JsonObject) {
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
    }

    private Optional<JsonArray> prepareValues(
            JsonObject root, List<String> path, CsvMappingDefinition definition, CsvMappingDataType type
    ) {
        Optional<JsonArray> check = this.getValuesFromObject(root, path);

        if (!check.isPresent()) {
            return check;
        }

        JsonArray values = check.get();

        // Fix the values size if it's not allowed to be expanded and it's a multi-column type
        if ((!definition.getListExpand() || definition.getType() != CsvMappingDataType.LIST) && type.isMultiColumn()
                && values.size() > 1) {
            values = new JsonArray().add(values.getValue(0));
        }

        // In case that the type is a list, we have to wrap it again.
        if (definition.getType() == CsvMappingDataType.LIST) {
            values = new JsonArray().add(values);
        }

        if (definition.getFilter() != null) {
            JsonArray filtered = new JsonArray();
            values.forEach(value -> {
                if (definition.getFilter().isAllowed(definition, root)) {
                    filtered.add(value);
                }
            });
            values = filtered;
        }

        return Optional.of(values);
    }

    private CsvMatrix generateEmptyCells(
            CsvMappingDefinition definition, CsvMappingDataType actualType
    ) {
        CsvMatrix matrix = new CsvMatrix();

        if (actualType.isMultiColumn()) {
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
                        Optional<JsonArray> tmp = this.getValuesFromObject(
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

    private ObservableSource<? extends ColumnGroup> renderValuesToColumns(
            CsvMappingDefinition definition, Single<JsonObject> config, CsvMappingDataType actualType, JsonArray arr,
            boolean hasOtherColumns
    ) {
        if (arr.isEmpty() && hasOtherColumns) {
            return Observable.fromIterable(this.generateEmptyCells(definition, actualType));
        }

        // When it's a multi-column, then we iterate over each element and generate a column value for it
        // Each column-value has to be evenly applied to all columns, otherwise we get a wrong amount.
        if (actualType.isMultiColumn()) {
            return renderValuesToMultiColumns(definition, config, arr);
        }

        return renderValuesToSingleColumn(definition, config, arr);
    }

    private ObservableSource<? extends ColumnGroup> renderValuesToSingleColumn(
            CsvMappingDefinition definition, Single<JsonObject> config, JsonArray arr
    ) {
        return Observable
                .fromIterable(arr)
                .concatMap(elm -> this.mapSingleValue(elm, definition, config).toObservable())
                .toList()
                .map(parts -> {
                    // Filter out null/empty values
                    parts = parts
                            .stream()
                            .filter(str -> str != null && !str.trim().isEmpty())
                            .collect(Collectors.toList());

                    // Requires at least one element, therefore add an empty cell
                    if (parts.isEmpty()) {
                        parts.add("");
                    }

                    if (definition.getType() == CsvMappingDataType.LIST && definition.getListExpand()) {
                        return parts;
                    }

                    // Create a new ArrayList and add the item
                    // Doesn't use Arrays#asList, as those generated lists are not modifiable
                    List<String> singlePartList = new ArrayList<>();
                    singlePartList.add(String.join(",", parts));

                    return singlePartList;
                })
                .map(Column::new)
                .map(ColumnGroup::new)
                .toObservable();
    }

    private ObservableSource<? extends ColumnGroup> renderValuesToMultiColumns(
            CsvMappingDefinition definition, Single<JsonObject> config, JsonArray arr
    ) {
        return Observable
                .fromIterable(arr)
                .map(obj -> (JsonObject) obj)
                .filter(obj -> {
                    if (definition.getSubFilter() != null) {
                        return definition.getSubFilter().isAllowed(definition, obj);
                    }
                    return true;
                })
                .concatMap(obj -> this.mapMultiColumns(obj, definition, config).toObservable())
                .toList()
                .flatMapObservable(list -> {
                    if (list.isEmpty()) {
                        list.addAll(generateEmptyCells(
                                definition,
                                definition.getType() == CsvMappingDataType.LIST
                                        ? definition.getListType()
                                        : definition.getType())
                        );
                    }

                    return Observable.fromIterable(list);
                })
                .collectInto(new ColumnGroup(), (sourceGroup, toMergeGroup) -> {
                    if (sourceGroup.isEmpty()) {
                        toMergeGroup.forEach(columnList -> sourceGroup.add(new Column(columnList.getValues())));
                    } else {
                        for (int i = 0; i < toMergeGroup.size(); i++) {
                            sourceGroup.get(i).addAll(toMergeGroup.get(i).getValues());
                        }
                    }
                })
                .toObservable();
    }

    private Single<ColumnGroup> mapMultiColumns(
            JsonObject subNode, CsvMappingDefinition parentMapping, Single<JsonObject> config
    ) {
        List<CsvSubMappingDefinition> subMapping = parentMapping.getSubMapping();

        return Observable.fromIterable(subMapping).concatMap(definition -> {
            List<String> path = definition.getPathAsList();
            Optional<JsonArray> maybeValues = this.getValuesFromObject(subNode, path);

            if (!maybeValues.isPresent()) {
                return Observable.just(new Column().addValue(""));
            }

            JsonArray values = maybeValues.get();

            if (definition.getFilter() != null) {
                JsonArray filtered = new JsonArray();
                values.forEach(value -> {
                    if (definition.getFilter().isAllowed(definition, subNode)) {
                        filtered.add(value);
                    }
                });
                values = filtered;
            }

            return mapArrayValuesToMultiColumns(parentMapping, config, definition, values);
        }).collectInto(new ColumnGroup(), ColumnGroup::add);
    }

    private ObservableSource<? extends Column> mapArrayValuesToMultiColumns(
            CsvMappingDefinition parentMapping, Single<JsonObject> config, CsvSubMappingDefinition definition,
            JsonArray arr
    ) {
        return Observable
                .fromIterable(arr)
                .concatMap(arrayValue -> this.mapSingleValue(arrayValue, definition, config).toObservable())
                .toList()
                .map(parts -> {
                    // Filter out null/empty values
                    parts = parts
                            .stream()
                            .filter(str -> str != null && !str.trim().isEmpty())
                            .collect(Collectors.toList());

                    // Requires at least one element, therefore add an empty cell
                    if (parts.isEmpty()) {
                        parts.add("");
                    }

                    // Create a new ArrayList and add the item
                    // Doesn't use Arrays#asList, as those generated lists are not modifiable
                    if ((parentMapping.getType() == CsvMappingDataType.LIST && parentMapping.getListExpand())
                            || definition.getType() != CsvMappingDataType.LIST) {
                        List<String> singlePartList = new ArrayList<>();
                        singlePartList.add(String.join(",", parts));
                        return singlePartList;
                    }

                    return parts;
                })
                .map(Column::new)
                .toObservable();
    }

    private Single<String> mapSingleValue(
            Object value, AbstractCsvMappingDefinition definition, Single<JsonObject> config
    ) {
        if (value == null) {
            return Single.just("");
        }

        final CsvMappingDataType type =
                definition.getType() == CsvMappingDataType.LIST ? definition.getListType() : definition.getType();

        try {
            switch (type) {
                case BOOLEAN:
                    if (value instanceof Boolean) {
                        return Single.just((Boolean) value ? "1" : "0");
                    } else {
                        return Single.just(Boolean.parseBoolean(value.toString()) ? "1" : "0");
                    }
                case NUMBER:
                    if (value instanceof Number) {
                        return Single.just(NUMBER_FORMAT.format(value));
                    }
                    return Single.just("0");
                case STRING:
                    return Single.just(value.toString());
                case DATE:
                    Instant date;
                    if (value instanceof Instant) {
                        date = (Instant) value;
                    } else {
                        date = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(value.toString()));
                    }
                    return Single.just(this.mapDateValue(date, definition.getDateFormat()));
                case CONFIG_VALUE:
                    return this.mapValueFromConfig(value.toString(), definition.getConfigKey(),
                            definition.getConfigValueField(), config);
            }
        } catch (Exception e) {
            // do nothing
        }

        return Single.just("");
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

    private Single<String> mapValueFromConfig(
            String id, String configKey, String configValueField, Single<JsonObject> config
    ) {
        if (StringUtils.trimToEmpty(id).isEmpty()) {
            return Single.just("");
        }
        if (StringUtils.trimToEmpty(configValueField).isEmpty()) {
            configValueField = CONFIG_KEY_VALUE;
        }

        // Java is so great it requires a final one even when it doesn't change anymore
        final String finalConfigValueField = configValueField;

        return config.map(conf -> {
            String value = "";
            if (conf.containsKey(configKey)) {
                JsonArray array = conf.getJsonArray(configKey);
                Optional<String> configValue = IntStream
                        .range(0, array.size())
                        .mapToObj(array::getJsonObject)
                        .filter(obj -> obj.containsKey(RESPONSE_KEY_FIELDS))
                        .map(obj -> obj.getJsonObject(RESPONSE_KEY_FIELDS))
                        .filter(obj -> obj != null && obj.containsKey(CONFIG_KEY_ID) && obj
                                .getValue(CONFIG_KEY_ID)
                                .equals(id) && obj.containsKey(finalConfigValueField))
                        .map(obj -> obj.getString(finalConfigValueField))
                        .findFirst();

                if (configValue.isPresent()) {
                    value = configValue.get();
                }
            }
            return value;
        });
    }

    private List<List<String>> createPrintableMatrix(CsvMatrix matrix) {
        List<List<String>> printMatrix = new ArrayList<>();

        for (ColumnGroup group : matrix) {
            appendColumnGroupToPrintMatrix(printMatrix, group);
        }

        return printMatrix;
    }

    private void appendColumnGroupToPrintMatrix(
            List<List<String>> printMatrix, ColumnGroup group
    ) {
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
}
