package com.gentics.mesh.plugin.forms.model.schema;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BinaryData {
	public static final Pattern MESH_ID_REGEX = Pattern.compile("^mesh:\\s*(\\w{8}\\w{4}\\w{4}\\w{4}\\w{12})$");

	@JsonProperty("fileName")
	private String fileName;

	@JsonProperty("size")
	private Long size;

	@JsonProperty("type")
	private String type;

	@JsonProperty("value")
	private String value;

	public String getFileName() {
		return fileName;
	}

	public BinaryData setFileName(String fileName) {
		this.fileName = fileName;
		return this;
	}

	public Long getSize() {
		return size;
	}

	public BinaryData setSize(Long size) {
		this.size = size;
		return this;
	}

	public String getType() {
		return type;
	}

	public BinaryData setType(String type) {
		this.type = type;
		return this;
	}

	public String getValue() {
		return value;
	}

	public BinaryData setValue(String value) {
		this.value = value;
		return this;
	}

	@JsonIgnore
	public Optional<String> getMeshUuid() {
		if (StringUtils.isBlank(value)) {
			return Optional.empty();
		} else {
			Matcher m = MESH_ID_REGEX.matcher(value);
			if (m.matches()) {
				return Optional.of(m.group(1));
			} else {
				return Optional.empty();
			}
		}
	}
}
