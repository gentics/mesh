package com.gentics.mesh.plugin.export.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import io.vertx.core.json.JsonObject;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.Range;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The model for the csv request download
 */
public class CsvRequest implements RestModel {

	@NotNull
	@Valid
	@JsonProperty(required = true)
	@JsonPropertyDescription("Variables which are forwarded to the GraphQL query")
	private CsvRequestGraphQLVariables variables;

	@NotEmpty
	@JsonProperty(required = true)
	@JsonPropertyDescription("A GraphQL query.")
	private String query;

	@NotEmpty
	@Valid
	@JsonProperty(required = true)
	@JsonPropertyDescription("The mapping for the csv export.")
	private List<CsvMappingDefinition> mapping;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The path in the request that leads to the elements which should be handled.")
	private String elementsPath = "nodes.elements";

	@JsonProperty(required = true)
	@JsonPropertyDescription("The path in the request which determines if there's more pages.")
	private String hasMorePath = "nodes.hasNextPage";

	@JsonProperty
	@JsonPropertyDescription("The maximal amount of Nodes (Not rows!) the export should contain")
	private Integer maximalNodes = 0;

	@Valid
	@JsonProperty
	@JsonPropertyDescription("The boolean mapping to use as default for this request")
	private BooleanMapping booleanMapping = new BooleanMapping();

	@NotEmpty
	@JsonProperty
	@JsonPropertyDescription("The format of how the date should be formatted for this column")
	private String dateFormat = "YYYY-MM-dd hh:mm:ss";

	@NotEmpty
	@JsonProperty
	@JsonPropertyDescription("The locale to use when formatting the date")
	private String dateLocale = "de";

	@NotEmpty
	@JsonProperty
	@JsonPropertyDescription("The time-zone to use when formatting the date")
	private String dateZone = "Europe/Vienna";

	@NotNull
	@JsonProperty
	@JsonPropertyDescription("The locale of how to format the number")
	private String numberLocale = "de";

	@NotNull
	@JsonProperty
	@JsonPropertyDescription("The style in which the number should be formatted")
	private NumberStyle numberStyle = NumberStyle.NUMBER;

	@JsonProperty
	@JsonPropertyDescription("The currency to use when formatting the number as currency")
	private String numberCurrency;

	@NotNull
	@Range(min = 0)
	@JsonProperty
	@JsonPropertyDescription("The minimum amount of fractions to show in the output")
	private Integer numberFractionMinimum = 0;

	@NotNull
	@Range(min = 0)
	@JsonProperty
	@JsonPropertyDescription("The maximum amount of fractions to show in the output")
	private Integer numberFractionMaximum = 10;

	@Valid
	@JsonProperty
	@JsonPropertyDescription("Filter to use for each element")
	public Filter elementFilter;

	@JsonIgnore
	private String projectName;

	@JsonIgnore
	private Map<String, Map<String, String>> configValues;

	public CsvRequest() {
	}

	public List<CsvMappingDefinition> getMapping() {
		return mapping;
	}

	public void setMapping(List<CsvMappingDefinition> mapping) {
		this.mapping = mapping;
	}

	public CsvRequestGraphQLVariables getVariables() {
		return variables;
	}

	public void setVariables(CsvRequestGraphQLVariables variables) {
		this.variables = variables;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getElementsPath() {
		return elementsPath;
	}

	public CsvRequest setElementsPath(String elementsPath) {
		this.elementsPath = elementsPath;
		return this;
	}

	public String getHasMorePath() {
		return hasMorePath;
	}

	public CsvRequest setHasMorePath(String hasMorePath) {
		this.hasMorePath = hasMorePath;
		return this;
	}

	public Integer getMaximalNodes() {
		return maximalNodes;
	}

	public CsvRequest setMaximalNodes(Integer maximalNodes) {
		this.maximalNodes = maximalNodes;
		return this;
	}

    public BooleanMapping getBooleanMapping() {
	    // Default the values
	    if (this.booleanMapping.getTrueValue() == null) {
	        this.booleanMapping.setTrueValue("1");
        }
	    if (this.booleanMapping.getFalseValue() == null) {
	        this.booleanMapping.setFalseValue("0");
        }
	    if (this.booleanMapping.getNullValue() == null) {
	        this.booleanMapping.setNullValue("0");
        }
        return booleanMapping;
    }

    public CsvRequest setBooleanMapping(BooleanMapping booleanMapping) {
        this.booleanMapping = booleanMapping;
        return this;
    }

    public DateTimeFormatter getDateFormatter() {
		return DateTimeFormatter.ofPattern(dateFormat)
			.withLocale(Locale.forLanguageTag(dateLocale))
			.withZone(ZoneId.of(dateZone));
	}

	public String getDateFormat() {
		return dateFormat;
	}

	public CsvRequest setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
		return this;
	}

	public String getDateLocale() {
		return dateLocale;
	}

	public CsvRequest setDateLocale(String dateLocale) {
		this.dateLocale = dateLocale;
		return this;
	}

	public String getDateZone() {
		return dateZone;
	}

	public CsvRequest setDateZone(String dateZone) {
		this.dateZone = dateZone;
		return this;
	}

	public NumberFormat getNumberFormatter() {
		NumberFormat formatter;
		// Default locale, just how NumberFormat would default it as well
		Locale locale = Locale.getDefault(Locale.Category.FORMAT);

		if (numberLocale != null) {
			locale = Locale.forLanguageTag(numberLocale);
		}

		switch (numberStyle) {
			case NUMBER:
				formatter = NumberFormat.getNumberInstance(locale);
				break;
			case INTEGER:
				formatter = NumberFormat.getIntegerInstance(locale);
				break;
			case PERCENT:
				formatter = NumberFormat.getPercentInstance(locale);
				break;
			case CURRENCY:
				formatter = NumberFormat.getNumberInstance(locale);
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

	public CsvRequest setNumberLocale(String numberLocale) {
		this.numberLocale = numberLocale;
		return this;
	}

	public NumberStyle getNumberStyle() {
		return numberStyle;
	}

	public CsvRequest setNumberStyle(NumberStyle numberStyle) {
		this.numberStyle = numberStyle;
		return this;
	}

	public String getNumberCurrency() {
		return numberCurrency;
	}

	public CsvRequest setNumberCurrency(String numberCurrency) {
		this.numberCurrency = numberCurrency;
		return this;
	}

	public Integer getNumberFractionMinimum() {
		return numberFractionMinimum;
	}

	public CsvRequest setNumberFractionMinimum(Integer numberFractionMinimum) {
		this.numberFractionMinimum = numberFractionMinimum;
		return this;
	}

	public Integer getNumberFractionMaximum() {
		return numberFractionMaximum;
	}

	public CsvRequest setNumberFractionMaximum(Integer numberFractionMaximum) {
		this.numberFractionMaximum = numberFractionMaximum;
		return this;
	}

	public String getProjectName() {
		return projectName;
	}

	public CsvRequest setProjectName(String projectName) {
		this.projectName = projectName;
		return this;
	}

	public Map<String, Map<String, String>> getConfigValues() {
		return configValues;
	}

	public CsvRequest setConfigValues(Map<String, Map<String, String>> configValues) {
		this.configValues = configValues;
		return this;
	}

	public Filter getElementFilter() {
		return elementFilter;
	}

	public CsvRequest setElementFilter(Filter elementFilter) {
		this.elementFilter = elementFilter;
		return this;
	}

	public GraphQLRequest buildGraphQLRequest() {
		GraphQLRequest request = new GraphQLRequest();
		request.setQuery(this.getQuery());
		request.setVariables(JsonObject.mapFrom(this.getVariables()));
		return request;
	}
}

