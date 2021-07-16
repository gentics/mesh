package com.gentics.mesh.plugin.export.model;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConvertingOptions {

	private BooleanMapping booleanMapping;
	private NumberFormat numberFormatter;
	private DateTimeFormatter dateFormatter;
	private Map<String, Map<String, String>> configValues = new HashMap<>();
	private List<CsvMappingDefinition> mapping = new ArrayList<>();

	public ConvertingOptions() {}

	public BooleanMapping getBooleanMapping() {
		return booleanMapping;
	}

	public ConvertingOptions setBooleanMapping(BooleanMapping booleanMapping) {
		this.booleanMapping = booleanMapping;
		return this;
	}

	public NumberFormat getNumberFormatter() {
		return numberFormatter;
	}

	public ConvertingOptions setNumberFormatter(NumberFormat numberFormatter) {
		this.numberFormatter = numberFormatter;
		return this;
	}

	public DateTimeFormatter getDateFormatter() {
		return dateFormatter;
	}

	public ConvertingOptions setDateFormatter(DateTimeFormatter dateFormatter) {
		this.dateFormatter = dateFormatter;
		return this;
	}

	public Map<String, Map<String, String>> getConfigValues() {
		return configValues;
	}

	public ConvertingOptions setConfigValues(Map<String, Map<String, String>> configValues) {
		this.configValues = configValues;
		return this;
	}

	public List<CsvMappingDefinition> getMapping() {
		return mapping;
	}

	public ConvertingOptions setMapping(List<CsvMappingDefinition> mapping) {
		this.mapping = mapping;
		return this;
	}
}
