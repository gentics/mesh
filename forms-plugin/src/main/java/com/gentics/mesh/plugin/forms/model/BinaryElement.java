package com.gentics.mesh.plugin.forms.model;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.plugin.forms.config.FileUploadOptions;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.i18n.I18nHelper;
import com.gentics.mesh.plugin.forms.model.schema.FormUiSchema;
import com.gentics.mesh.plugin.forms.util.RxUtil;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.reactivex.core.Vertx;
import okhttp3.OkHttpClient;

/**
 * Element for file uploads
 */
@Reportable
@FormElementType("binary")
public class BinaryElement extends FormElement {
	public BinaryElement() {
	}

	protected BinaryElement(BinaryElement other, boolean deep) {
		super(other, deep);

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
	public Maybe<ValidationError> validate(MultiMap postedData, Set<FileUpload> fileUploads, NodeResponse formDataNode, Vertx rxVertx, OkHttpClient httpClient, FormUiSchema uiSchema) {

		Map<String, Object> gridOptions = getFormGridOptions(uiSchema).orElse(null);

		FileUpload fileUpload = getFileUpload(fileUploads);
		if (fileUpload == null) {
			if (isMandatory()) {
				if (formDataNode != null) {
					BinaryField field = formDataNode.getFields().getBinaryField(getName());
					if (field != null && field.hasValues()) {
						return RxUtil.emptyMaybe();
					}
				}
				return Maybe.just(new ValidationError().setName(getName()).setTitle(I18nHelper.get(Locale.GERMAN, "error.mandatory_field_not_filled.title"))
					.setMessage(I18nHelper.get(Locale.GERMAN, "error.mandatory_field_not_filled", getLabel())));
			} else {
				return RxUtil.emptyMaybe();
			}
		}

		int maxFileSize = getMaxFileSize(gridOptions);
		if (maxFileSize > 0 && fileUpload.size() > (maxFileSize * 1024 * 1024)) {
			fileUploads.remove(fileUpload);
			return Maybe.just(new ValidationError().setName(getName()).setTitle(I18nHelper.get(Locale.GERMAN, "error.validation.file.size.title"))
				.setMessage(I18nHelper.get(Locale.GERMAN, "error.validation.file.size", fileUpload.fileName(), Integer.toString(maxFileSize))));
		}

		Collection<String> allowedTypes = getAllowedFiletypes(gridOptions);
		if (allowedTypes == null) {
			allowedTypes = getDefaultAllowedConfiguration();
		}
		if (allowedTypes != null && !allowedTypes.isEmpty()) {
			Collection<String> localAllowedTypes = allowedTypes;
			return fileHasAllowedContentType(fileUpload, allowedTypes, rxVertx).flatMapMaybe(allowedContentType -> {
				if (!allowedContentType && !fileHasAllowedName(fileUpload, localAllowedTypes)) {
					fileUploads.remove(fileUpload);
					return Maybe.just(new ValidationError()
						.setName(getName())
						.setTitle(I18nHelper.get(Locale.GERMAN, "error.validation.file.type.title"))
						.setMessage(I18nHelper.get(Locale.GERMAN, "error.validation.file.type.check", fileUpload.fileName())));
				} else {
					return RxUtil.emptyMaybe();
				}
			}).onErrorResumeNext(e -> {
				log.error("Could not check file type of uploaded file " + fileUpload.fileName() + " (" + fileUpload.contentType() + ")", e);
				return Maybe.just(new ValidationError()
					.setName(getName())
					.setTitle(I18nHelper.get(Locale.GERMAN, "error.validation.file.type.title"))
					.setMessage(I18nHelper.get(Locale.GERMAN, "error.validation.file.type.check", fileUpload.fileName())));
			});
		}
		return RxUtil.emptyMaybe();
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
		return new BinaryElement(this, deep);
	}

	/**
	 * Get max file size from grid options
	 *
	 * @param gridOptions
	 *            grid options
	 * @return max file size
	 */
	protected int getMaxFileSize(Map<String, Object> gridOptions) {
		int maxFileSize = 0;
		if (maxFileSize <= 0 && gridOptions != null && gridOptions.get("maxFileSize") instanceof Number) {
			maxFileSize = ((Number) gridOptions.get("maxFileSize")).intValue();
		}
		return maxFileSize;
	}

	/**
	 * Get allowed file types from grid options or default
	 *
	 * @param gridOptions
	 *            grid options
	 * @return allowed file types collection
	 */
	protected Collection<String> getAllowedFiletypes(Map<String, Object> gridOptions) {
		Collection<String> allowedTypes = null;
		if ((allowedTypes == null || allowedTypes.isEmpty()) && gridOptions != null && gridOptions.get("allowedFiletypes") instanceof List) {
			@SuppressWarnings("unchecked")
			List<String> uiAllowedTypes = (List<String>) gridOptions.get("allowedFiletypes");
			allowedTypes = uiAllowedTypes;
		}
		return allowedTypes;
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
	protected Single<Boolean> fileHasAllowedContentType(FileUpload fileUpload, Collection<String> allowedTypes, Vertx rxVertx) {
		Map<String, Set<String>> typesMap = getAllowedTypesConfiguration();
		Set<String> allowedContentTypes = new HashSet<>();
		for (String type : allowedTypes) {
			if (typesMap.get(type) != null) {
				allowedContentTypes.addAll(typesMap.get(type));
			}
		}

		Single<String> mimeTypeSingle = getConfiguration().getFileUpload().isUseMagicByteCheck()
			? determineMimeType(fileUpload.uploadedFileName(), fileUpload.fileName(), rxVertx)
			: Single.just(fileUpload.contentType());

		return mimeTypeSingle
				.map(mimeType -> StringUtils.isBlank(mimeType)
						? false
						: allowedContentTypes.stream()
							.filter(Objects::nonNull)
							.filter(pattern -> mimeType.matches(pattern))
							.findFirst().isPresent());
	}

	/**
	 * Determine MIME type from file contents.
	 *
	 * @param uploadedFileName The path to the file to inspect.
	 * @param providedFileName the provided file name
	 * @return The detected MIME type.
	 */
	private Single<String> determineMimeType(String uploadedFileName, String providedFileName, Vertx rxVertx) {
		TikaConfig tikaConfig = TikaConfig.getDefaultConfig();
		Detector detector = tikaConfig.getDetector();

		return Single.fromCallable(() -> {
			try (TikaInputStream tikaInputStream = TikaInputStream.get(new BufferedInputStream(new FileInputStream(uploadedFileName)))) {
				Metadata metadata = new Metadata();
				metadata.add(TikaCoreProperties.RESOURCE_NAME_KEY, providedFileName);
				String mimeType = detector.detect(tikaInputStream, metadata).toString();

				log.debug(String.format("Tika detected mime type %s", mimeType));
				return mimeType;
			}
		});
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
