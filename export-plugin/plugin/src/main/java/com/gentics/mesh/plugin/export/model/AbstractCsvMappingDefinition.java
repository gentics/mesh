package com.gentics.mesh.plugin.export.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Range;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

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

    @Valid
    @JsonProperty
    @JsonPropertyDescription("Filter to limit the elements in this column")
    private Filter filter;

    @Valid
    @JsonProperty
    @JsonPropertyDescription("Mappings for how booleans should be mapped")
    private BooleanMapping booleanMapping;

    @JsonProperty
    @JsonPropertyDescription("The option of the config to get the display-value.")
    private String configKey;

    @JsonProperty
    @JsonPropertyDescription("The format of how the date should be formatted for this column")
    private String dateFormat;

    @JsonProperty
    @JsonPropertyDescription("The locale to use when formatting the date")
    private String dateLocale;

    @JsonProperty
    @JsonPropertyDescription("The time-zone to use when formatting the date")
    private String dateZone;

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

    @JsonProperty
    @JsonPropertyDescription("The locale of how to format the number")
    private String numberLocale;

    @JsonProperty
    @JsonPropertyDescription("The style in which the number should be formatted")
    private NumberStyle numberStyle;

    @JsonProperty
    @JsonPropertyDescription("The currency to use when formatting the number as currency")
    private String numberCurrency;

    @JsonProperty
    @JsonPropertyDescription("The minimum amount of fractions to show in the output")
    private Integer numberFractionMinimum;

    @JsonProperty
    @JsonPropertyDescription("The maximum amount of fractions to show in the output")
    private Integer numberFractionMaximum;

    @JsonProperty
    @JsonPropertyDescription("The regexp definition to use when extracting the data.")
    private String regexpDefinition;

    @JsonProperty
    @JsonPropertyDescription("Flags to use with the 'regexpDefinition' when compiling the Pattern.")
    private Integer regexpFlags = 0;

    @JsonProperty
    @JsonPropertyDescription("The regexp template which renders the result with all placeholders replaced.")
    private String regexpTemplate;

    @JsonProperty
    @JsonPropertyDescription("A template string which replaces the token '" + TEMPLATE_REPLACE_TOKEN + "' with the calculated value and outputs it.")
    private String templateDefinition = null;

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

    public AbstractCsvMappingDefinition setType(@NotNull CsvMappingDataType type) {
        this.type = type;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public AbstractCsvMappingDefinition setLabel(String label) {
        this.label = label;
        return this;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public AbstractCsvMappingDefinition setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public Filter getFilter() {
        return filter;
    }

    public AbstractCsvMappingDefinition setFilter(Filter filter) {
        this.filter = filter;
        return this;
    }

    public BooleanMapping getBooleanMapping() {
        return booleanMapping;
    }

    public AbstractCsvMappingDefinition setBooleanMapping(BooleanMapping booleanMapping) {
        this.booleanMapping = booleanMapping;
        return this;
    }

    public String getConfigKey() {
        return configKey;
    }

    public AbstractCsvMappingDefinition setConfigKey(String configKey) {
        this.configKey = configKey;
        return this;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public AbstractCsvMappingDefinition setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
        return this;
    }

    public String getDateLocale() {
        return dateLocale;
    }

    public AbstractCsvMappingDefinition setDateLocale(String dateLocale) {
        this.dateLocale = dateLocale;
        return this;
    }

    public String getDateZone() {
        return dateZone;
    }

    public AbstractCsvMappingDefinition setDateZone(String dateZone) {
        this.dateZone = dateZone;
        return this;
    }

    public CsvMappingDataType getListType() {
        return listType;
    }

    public AbstractCsvMappingDefinition setListType(CsvMappingDataType listType) {
        this.listType = listType;
        return this;
    }

    public boolean getListExpand() {
        return listExpand != null && listExpand;
    }

    public AbstractCsvMappingDefinition setListExpand(Boolean listExpand) {
        this.listExpand = listExpand;
        return this;
    }

    public Integer getListExpandLimit() {
        return listExpandLimit;
    }

    public AbstractCsvMappingDefinition setListExpandLimit(Integer listExpandLimit) {
        this.listExpandLimit = listExpandLimit;
        return this;
    }

    public NumberFormat getNumberFormatter() {
        NumberFormat formatter;

        if (numberStyle == null) {
            return null;
        }

        switch (numberStyle) {
            case NUMBER:
                formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag(numberLocale));
                break;
            case INTEGER:
                formatter = NumberFormat.getIntegerInstance(Locale.forLanguageTag(numberLocale));
                break;
            case PERCENT:
                formatter = NumberFormat.getPercentInstance(Locale.forLanguageTag(numberLocale));
                break;
            case CURRENCY:
                formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag(numberLocale));
                if (numberCurrency != null) {
                    formatter.setCurrency(Currency.getInstance(numberCurrency));
                }
                break;
            default:
                return null;
        }

        if (formatter != null && numberStyle != NumberStyle.CURRENCY) {
            if (numberFractionMinimum != null) {
                formatter.setMinimumFractionDigits(numberFractionMinimum);
            }
            if (numberFractionMaximum != null) {
                formatter.setMaximumFractionDigits(numberFractionMaximum);
            }
        }

        return formatter;
    }

    public String getNumberLocale() {
        return numberLocale;
    }

    public AbstractCsvMappingDefinition setNumberLocale(String numberLocale) {
        this.numberLocale = numberLocale;
        return this;
    }

    public NumberStyle getNumberStyle() {
        return numberStyle;
    }

    public AbstractCsvMappingDefinition setNumberStyle(NumberStyle numberStyle) {
        this.numberStyle = numberStyle;
        return this;
    }

    public String getNumberCurrency() {
        return numberCurrency;
    }

    public AbstractCsvMappingDefinition setNumberCurrency(String numberCurrency) {
        this.numberCurrency = numberCurrency;
        return this;
    }

    public Integer getNumberFractionMinimum() {
        return numberFractionMinimum;
    }

    public AbstractCsvMappingDefinition setNumberFractionMinimum(Integer numberFractionMinimum) {
        this.numberFractionMinimum = numberFractionMinimum;
        return this;
    }

    public Integer getNumberFractionMaximum() {
        return numberFractionMaximum;
    }

    public AbstractCsvMappingDefinition setNumberFractionMaximum(Integer numberFractionMaximum) {
        this.numberFractionMaximum = numberFractionMaximum;
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
        return regexpFlags;
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

    public String getTemplateDefinition() {
        return templateDefinition;
    }

    public AbstractCsvMappingDefinition setTemplateDefinition(String templateDefinition) {
        this.templateDefinition = templateDefinition;
        return this;
    }
}
