package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.plugin.forms.config.FileUploadOptions;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.i18n.I18nHelper;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Element for file uploads
 */
@Reportable
public class FileElement extends FormElement {

	@JsonProperty("max_filesize")
	private int maxSizeMB;

	@JsonProperty("allowed_filetypes")
	private List<String> allowedFiletypes;

	public FileElement() {
	}

	protected FileElement(FileElement other, boolean deep) {
		super(other, deep);

		this.maxSizeMB = other.maxSizeMB;;
		this.allowedFiletypes = cloneField(other.allowedFiletypes, deep);
	}

	@JsonProperty("max_filesize")
	public int getMaxSizeMB() {
		return maxSizeMB;
	}

	@JsonProperty("max_filesize")
	public void setMaxSizeMB(int maxSizeMB) {
		this.maxSizeMB = maxSizeMB;
	}

	@JsonProperty("allowed_filetypes")
	public List<String> getAllowedFiletypes() {
		return allowedFiletypes;
	}

	@JsonProperty("allowed_filetypes")
	public void setAllowedFiletypes(List<String> allowedFiletypes) {
		this.allowedFiletypes = allowedFiletypes;
	}

	@Override
	public void addFormData(JsonObject object, FieldContainer container, Function<FormElement, String> key) {
		BinaryField field = container.getFields().getBinaryField(getName());
		if (field != null) {
			object.put(key.apply(this), field.getFileName());
		}
	}

	@Override
	public void addFieldSchema(FieldSchemaContainer container) {
		container.addField(new BinaryFieldSchemaImpl().setName(getName()).setLabel(getName()));
	}

	@Override
	public String getSingleFormData(MultiMap postedData) {
		return null;
	}

	@Override
	public List<String> getMultiFormData(MultiMap postedData) {
		return null;
	}

	@Override
	public ValidationError validate(MultiMap postedData, Set<FileUpload> fileUploads, NodeResponse formDataNode) {
		FileUpload fileUpload = getFileUpload(fileUploads);
		if (fileUpload == null) {
			if (isMandatory()) {
				if (formDataNode != null) {
					BinaryField field = formDataNode.getFields().getBinaryField(getName());
					if (field != null && field.hasValues()) {
						return null;
					}
				}
				return new ValidationError().setName(getName()).setTitle(I18nHelper.get(Locale.GERMAN, "error.mandatory_field_not_filled.title"))
					.setMessage(I18nHelper.get(Locale.GERMAN, "error.mandatory_field_not_filled", getLabel()));
			} else {
				return null;
			}
		}

		List<String> allowedTypes = getAllowedFiletypes();
		if (allowedTypes != null && !allowedTypes.isEmpty()) {
			if (!fileHasAllowedContentType(fileUpload, allowedTypes) && !fileHasAllowedName(fileUpload, allowedTypes)) {
				fileUploads.remove(fileUpload);
				return new ValidationError().setName(getName()).setTitle(I18nHelper.get(Locale.GERMAN, "error.validation.file.type.title"))
					.setMessage(I18nHelper.get(Locale.GERMAN, "error.validation.file.type", fileUpload.fileName()));
			}
		}

		if (maxSizeMB > 0 && fileUpload.size() > (maxSizeMB * 1024 * 1024)) {
			fileUploads.remove(fileUpload);
			return new ValidationError().setName(getName()).setTitle(I18nHelper.get(Locale.GERMAN, "error.validation.file.size.title"))
				.setMessage(I18nHelper.get(Locale.GERMAN, "error.validation.file.size", fileUpload.fileName(), Integer.toString(maxSizeMB)));
		}

		return null;
	}

	@Override
	public void addField(FieldContainer container, MultiMap postedData, boolean errors) {
	}

	@Override
	public String getGraphQLName() {
		return String.format("%s { fileName }", getName());
	}

	@Override
	protected FormElement clone(boolean deep) {
		return new FileElement(this, deep);
	}

	/**
	 * Get the fileupload for this field
	 *
	 * @param fileUploads
	 *            fileuploads
	 * @return fileupload or null
	 */
	protected FileUpload getFileUpload(Set<FileUpload> fileUploads) {
		return fileUploads.stream().filter(fu -> getName().equals(fu.name())).findFirst().orElse(null);
	}

	/**
	 * Check whether the file has an allowed content-type
	 *
	 * @param fileUpload
	 *            file upload
	 * @param allowedTypes
	 *            allowed type definitions for the field
	 * @return true if the file has allowed content-type
	 */
	protected boolean fileHasAllowedContentType(FileUpload fileUpload, List<String> allowedTypes) {
		Map<String, Set<String>> typesMap = getAllowedTypesConfiguration();
		Set<String> allowedContentTypes = new HashSet<>();
		for (String type : allowedTypes) {
			if (typesMap.get(type) != null) {
				allowedContentTypes.addAll(typesMap.get(type));
			}
		}
		return allowedContentTypes.stream().filter(pattern -> fileUpload.contentType().matches(pattern)).findFirst().isPresent();
	}

	/**
	 * Check whether the file has an allowed name
	 *
	 * @param fileUpload
	 *            file upload
	 * @param allowedTypes
	 *            allowed type definitions for the field
	 * @return true if the file has allowed name
	 */
	protected boolean fileHasAllowedName(FileUpload fileUpload, List<String> allowedTypes) {
		Map<String, Set<String>> namesMap = getAllowedNamesConfiguration();
		Set<String> allowedNames = new HashSet<>();
		for (String type : allowedTypes) {
			if (namesMap.get(type) != null) {
				allowedNames.addAll(namesMap.get(type));
			}
		}
		return allowedNames.stream().filter(pattern -> fileUpload.fileName().matches(pattern)).findFirst().isPresent();
	}

	/**
	 * Get configured allowed file types or empty map
	 *
	 * @return map of configured allowed file types
	 */
	protected Map<String, Set<String>> getAllowedTypesConfiguration() {
		FormsConfig config = getConfiguration();
		if (config== null) {
			return Collections.emptyMap();
		}
		FileUploadOptions fileUploadOptions = config.getFileUpload();
		if (fileUploadOptions == null) {
			return Collections.emptyMap();
		}
		if (fileUploadOptions.getAllowedFileTypes() == null) {
			return Collections.emptyMap();
		}
		return fileUploadOptions.getAllowedFileTypes();
	}

	/**
	 * Get configured allowed file names or empty map
	 *
	 * @return map of configured allowed file names
	 */
	protected Map<String, Set<String>> getAllowedNamesConfiguration() {
		FormsConfig formsOptions = getConfiguration();
		if (formsOptions == null) {
			return Collections.emptyMap();
		}
		FileUploadOptions fileUploadOptions = formsOptions.getFileUpload();
		if (fileUploadOptions == null) {
			return Collections.emptyMap();
		}
		if (fileUploadOptions.getAllowedFileNames() == null) {
			return Collections.emptyMap();
		}
		return fileUploadOptions.getAllowedFileNames();
	}
}
