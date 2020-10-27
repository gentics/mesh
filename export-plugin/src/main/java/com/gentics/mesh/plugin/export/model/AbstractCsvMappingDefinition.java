package com.gentics.mesh.plugin.export.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Range;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

public class AbstractCsvMappingDefinition {
    @NotNull
    @JsonProperty(required = true)
    private String path;

    @NotNull
    @JsonProperty(required = true)
    private CsvMappingDataType type;

    @JsonProperty
    @JsonPropertyDescription("The label of the column in the csv for this mapping.")
    private String label = "";

    @JsonProperty
    @JsonPropertyDescription("The option of the config to get the display-value.")
    private String configKey;

    @JsonProperty(defaultValue = "value")
    @JsonPropertyDescription("The key that is used to get the value/translation of the config-value")
    private String configValueField;

    @JsonProperty
    @JsonPropertyDescription("The types inside of the list.")
    private CsvMappingDataType listType;

    @JsonProperty(defaultValue = "false")
    @JsonPropertyDescription("If each value of the list should create a new CSV line")
    private Boolean listExpand;

    @Range(min = 1)
    @JsonProperty
    @JsonPropertyDescription("Limit of how many elements this column is able to output")
    private Integer listExpandLimit;

    @JsonProperty(defaultValue = "YYYY-MM-dd hh:mm:ss")
    @JsonPropertyDescription("The format of how the date should be formatted for this column")
    private String dateFormat;

    @Valid
    @JsonProperty
    @JsonPropertyDescription("Filter to limit the elements in this column")
    private Filter filter;

    public static List<String> pathStringAsList(String path) {
        return Arrays.asList(StringUtils.trimToEmpty(path).split("\\."));
    }

    @NotNull
    public String getPath() {
        return path;
    }

    public void setPath(@NotNull String path) {
        this.path = path;
    }

    public List<String> getPathAsList() {
        return pathStringAsList(this.path);
    }

    @NotNull
    public CsvMappingDataType getType() {
        return type;
    }

    public void setType(@NotNull CsvMappingDataType type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValueField() {
        return configValueField;
    }

    public void setConfigValueField(String configValueField) {
        this.configValueField = configValueField;
    }

    public CsvMappingDataType getListType() {
        return listType;
    }

    public void setListType(CsvMappingDataType listType) {
        this.listType = listType;
    }

    public Boolean getListExpand() {
        return listExpand == null ? false : listExpand;
    }

    public void setListExpand(Boolean listExpand) {
        this.listExpand = listExpand;
    }

    public Integer getListExpandLimit() {
        return listExpandLimit;
    }

    public void setListExpandLimit(Integer listExpandLimit) {
        this.listExpandLimit = listExpandLimit;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }
}
