package com.gentics.mesh.plugin.forms.model.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UischemaFormgridOptions {

	@JsonProperty("type")
	private String type;

	@JsonProperty("condition")
	private Map<String, Object> condition;

	@JsonProperty("numberOfColumns")
	private Object numberOfColumns;

	@JsonProperty("numberOfLines")
	private Object numberOfLines;

	@JsonProperty("inForm")
	private Boolean inForm;

	@JsonProperty("inSummary")
	private Boolean inSummary;

	@JsonProperty("image")
	private Map<String, Object> image;

	@JsonProperty("radio")
	private Boolean radio;

	@JsonProperty("fileExtensionsNotAllowed")
	private List<String> fileExtensionsNotAllowed;

	@JsonProperty("button")
	private Boolean button;

	@JsonProperty("buttonIsPrimary")
	private Boolean buttonIsPrimary;

	@JsonProperty("showFilter")
	private Boolean showFilter;

	@JsonProperty("showOnlyFirstLine")
	private Boolean showOnlyFirstLine;

	@JsonProperty("buttonDisplayStyle")
	private String buttonDisplayStyle;

	@JsonProperty("overlayOptions")
	private Map<String, Object> overlayOptions;

	/** Not in JSON schema, used by BinaryElement for file size validation. */
	@JsonProperty("maxFileSize")
	private Number maxFileSize;

	/** Not in JSON schema, used by BinaryElement for file type validation. */
	@JsonProperty("allowedFiletypes")
	private List<String> allowedFiletypes;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Map<String, Object> getCondition() {
		return condition;
	}

	public void setCondition(Map<String, Object> condition) {
		this.condition = condition;
	}

	public Object getNumberOfColumns() {
		return numberOfColumns;
	}

	public void setNumberOfColumns(Object numberOfColumns) {
		this.numberOfColumns = numberOfColumns;
	}

	public Object getNumberOfLines() {
		return numberOfLines;
	}

	public void setNumberOfLines(Object numberOfLines) {
		this.numberOfLines = numberOfLines;
	}

	public Boolean getInForm() {
		return inForm;
	}

	public void setInForm(Boolean inForm) {
		this.inForm = inForm;
	}

	public Boolean getInSummary() {
		return inSummary;
	}

	public void setInSummary(Boolean inSummary) {
		this.inSummary = inSummary;
	}

	public Map<String, Object> getImage() {
		return image;
	}

	public void setImage(Map<String, Object> image) {
		this.image = image;
	}

	public Boolean getRadio() {
		return radio;
	}

	public void setRadio(Boolean radio) {
		this.radio = radio;
	}

	public List<String> getFileExtensionsNotAllowed() {
		return fileExtensionsNotAllowed;
	}

	public void setFileExtensionsNotAllowed(List<String> fileExtensionsNotAllowed) {
		this.fileExtensionsNotAllowed = fileExtensionsNotAllowed;
	}

	public Boolean getButton() {
		return button;
	}

	public void setButton(Boolean button) {
		this.button = button;
	}

	public Boolean getButtonIsPrimary() {
		return buttonIsPrimary;
	}

	public void setButtonIsPrimary(Boolean buttonIsPrimary) {
		this.buttonIsPrimary = buttonIsPrimary;
	}

	public Boolean getShowFilter() {
		return showFilter;
	}

	public void setShowFilter(Boolean showFilter) {
		this.showFilter = showFilter;
	}

	public Boolean getShowOnlyFirstLine() {
		return showOnlyFirstLine;
	}

	public void setShowOnlyFirstLine(Boolean showOnlyFirstLine) {
		this.showOnlyFirstLine = showOnlyFirstLine;
	}

	public String getButtonDisplayStyle() {
		return buttonDisplayStyle;
	}

	public void setButtonDisplayStyle(String buttonDisplayStyle) {
		this.buttonDisplayStyle = buttonDisplayStyle;
	}

	public Map<String, Object> getOverlayOptions() {
		return overlayOptions;
	}

	public void setOverlayOptions(Map<String, Object> overlayOptions) {
		this.overlayOptions = overlayOptions;
	}

	public Number getMaxFileSize() {
		return maxFileSize;
	}

	public void setMaxFileSize(Number maxFileSize) {
		this.maxFileSize = maxFileSize;
	}

	public List<String> getAllowedFiletypes() {
		return allowedFiletypes;
	}

	public void setAllowedFiletypes(List<String> allowedFiletypes) {
		this.allowedFiletypes = allowedFiletypes;
	}
}
