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
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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

		Collection<String> allowedTypes = getAllowedFiletypes();
		if (allowedTypes == null) {
			allowedTypes = getDefaultAllowedConfiguration();
		}
		if (allowedTypes != null && !allowedTypes.isEmpty()) {
			boolean allowedContentType;

			try {
				allowedContentType = fileHasAllowedContentType(fileUpload, allowedTypes);
			} catch (IOException e) {
				log.error("Could not check file type of uploaded file {} ({}): {}", e, fileUpload.fileName(), fileUpload.contentType(), e.getMessage());

				return new ValidationError()
					.setName(getName())
					.setTitle(I18nHelper.get(Locale.GERMAN, "error.validation.file.type.title"))
					.setMessage(I18nHelper.get(Locale.GERMAN, "error.validation.file.type.check", fileUpload.fileName()));
			}

			if (!allowedContentType && !fileHasAllowedName(fileUpload, allowedTypes)) {
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
	protected boolean fileHasAllowedContentType(FileUpload fileUpload, Collection<String> allowedTypes) throws IOException {
		Map<String, Set<String>> typesMap = getAllowedTypesConfiguration();
		Set<String> allowedContentTypes = new HashSet<>();
		for (String type : allowedTypes) {
			if (typesMap.get(type) != null) {
				allowedContentTypes.addAll(typesMap.get(type));
			}
		}

		String mimeType = getConfiguration().getFileUpload().isUseMagicByteCheck()
			? determineMimeType(fileUpload.uploadedFileName())
			: fileUpload.contentType();

		if (mimeType == null) {
			return false;
		}

		return allowedContentTypes.stream()
			.filter(Objects::nonNull)
			.filter(pattern -> mimeType.matches(pattern)).findFirst().isPresent();
	}

	/**
	 * Determine MIME type from file contents.
	 *
	 * @param filename The path to the file to inspect.
	 * @return The detected MIME type.
	 */
	private String determineMimeType(String filename) throws IOException {
		TikaConfig tikaConfig = TikaConfig.getDefaultConfig();
		Detector detector = tikaConfig.getDetector();
		FileInputStream inputStream = new FileInputStream(filename);
		MediaType mimeType = detector.detect(TikaInputStream.get(inputStream), new Metadata());

		return mimeType == null ? null : mimeType.getType();
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
	protected boolean fileHasAllowedName(FileUpload fileUpload, Collection<String> allowedTypes) {
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

	/**
	 * Get configured default allowed categories or empty set
	 * @return set of default allowed
	 */
	protected Set<String> getDefaultAllowedConfiguration() {
		FormsConfig formsOptions = getConfiguration();
		if (formsOptions == null) {
			return Collections.emptySet();
		}
		FileUploadOptions fileUploadOptions = formsOptions.getFileUpload();
		if (fileUploadOptions == null) {
			return Collections.emptySet();
		}
		Set<String> defaultAllowed = fileUploadOptions.getDefaultAllowed();
		if (defaultAllowed == null) {
			defaultAllowed = Collections.emptySet();
		}
		return defaultAllowed;
	}
}
