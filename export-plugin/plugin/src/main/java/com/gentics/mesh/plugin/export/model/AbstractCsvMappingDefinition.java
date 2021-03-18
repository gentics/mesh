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

    public static final String TEMPLATE_REPLACE_TOKEN = "%value%";

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
    @JsonPropertyDescription("The value to use when it can't be found or is null.")
    private Object defaultValue = null;

    @JsonProperty
    @JsonPropertyDescription("A template string which replaces the token '" + TEMPLATE_REPLACE_TOKEN + "' with the calculated value and outputs it.")
    private String templateDefinition = null;

    @JsonProperty
    @JsonPropertyDescription("The regexp defintion to use when extracting the data.")
    private String regexpDefinition;

    @JsonProperty
    @JsonPropertyDescription("Flags to use with the 'regexpDefinition' when compiling the Pattern.")
    private Integer regexpFlags = 0;

    @JsonProperty
    @JsonPropertyDescription("The regexp template which renders the result with all placeholders replaced.")
    private String regexpTemplate;

    @JsonProperty
    @JsonPropertyDescription("The option of the config to get the display-value.")
    private String configKey;

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

    public CsvMappingDataType getActualType() {
        return type == CsvMappingDataType.LIST ? listType : type;
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

    public Object getDefaultValue() {
        return defaultValue;
    }

    public AbstractCsvMappingDefinition setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public String getTemplateDefinition() {
        return templateDefinition;
    }

    public AbstractCsvMappingDefinition setTemplateDefinition(String templateDefinition) {
        this.templateDefinition = templateDefinition;
        return this;
    }

    public String getRegexpDefinition() {
        return regexpDefinition;
    }

    public AbstractCsvMappingDefinition setRegexpDefinition(String regexpDefinition) {
        this.regexpDefinition = regexpDefinition;
        return this;
    }

    public Integer getRegexpFlags() {
        return regexpFlags == null ? 0 : regexpFlags;
    }

    public AbstractCsvMappingDefinition setRegexpFlags(Integer regexpFlags) {
        this.regexpFlags = regexpFlags;
        return this;
    }

    public String getRegexpTemplate() {
        return regexpTemplate;
    }

    public AbstractCsvMappingDefinition setRegexpTemplate(String regexpTemplate) {
        this.regexpTemplate = regexpTemplate;
        return this;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
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
