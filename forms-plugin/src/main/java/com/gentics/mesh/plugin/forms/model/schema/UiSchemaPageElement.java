package com.gentics.mesh.plugin.forms.model.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UiSchemaPageElement {

	@JsonProperty("type")
	private String type;

	@JsonProperty("id")
	private String id;

	@JsonProperty("label")
	private Map<String, String> label;

	@JsonProperty("description")
	private Map<String, String> description;

	@JsonProperty("formGridOptions")
	private UischemaFormgridOptions formGridOptions;

	@JsonProperty("editModeDialogUsage")
	private Number editModeDialogUsage;

	@JsonProperty("csvUploadEnabled")
	private Boolean csvUploadEnabled;

	@JsonProperty("editMode")
	private Integer editMode;

	@JsonProperty("editModeDialogFormID")
	private String editModeDialogFormID;

	@JsonProperty("uiSchemaPage")
	private Integer uiSchemaPage;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Map<String, String> getLabel() {
		return label;
	}

	public void setLabel(Map<String, String> label) {
		this.label = label;
	}

	public Map<String, String> getDescription() {
		return description;
	}

	public void setDescription(Map<String, String> description) {
		this.description = description;
	}

	public UischemaFormgridOptions getFormGridOptions() {
		return formGridOptions;
	}

	public void setFormGridOptions(UischemaFormgridOptions formGridOptions) {
		this.formGridOptions = formGridOptions;
	}

	public Number getEditModeDialogUsage() {
		return editModeDialogUsage;
	}

	public void setEditModeDialogUsage(Number editModeDialogUsage) {
		this.editModeDialogUsage = editModeDialogUsage;
	}

	public Boolean getCsvUploadEnabled() {
		return csvUploadEnabled;
	}

	public void setCsvUploadEnabled(Boolean csvUploadEnabled) {
		this.csvUploadEnabled = csvUploadEnabled;
	}

	public Integer getEditMode() {
		return editMode;
	}

	public void setEditMode(Integer editMode) {
		this.editMode = editMode;
	}

	public String getEditModeDialogFormID() {
		return editModeDialogFormID;
	}

	public void setEditModeDialogFormID(String editModeDialogFormID) {
		this.editModeDialogFormID = editModeDialogFormID;
	}

	public Integer getUiSchemaPage() {
		return uiSchemaPage;
	}

	public void setUiSchemaPage(Integer uiSchemaPage) {
		this.uiSchemaPage = uiSchemaPage;
	}
}
