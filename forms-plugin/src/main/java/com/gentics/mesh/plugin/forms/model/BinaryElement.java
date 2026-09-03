package com.gentics.mesh.plugin.forms.model;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.forms.SchemaInitializer;
import com.gentics.mesh.plugin.forms.config.FileUploadOptions;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.context.FormDataCreateContext;
import com.gentics.mesh.plugin.forms.i18n.I18nHelper;
import com.gentics.mesh.plugin.forms.model.schema.BinaryData;
import com.gentics.mesh.plugin.forms.model.schema.BinaryValidation;
import com.gentics.mesh.plugin.forms.model.schema.FormUiSchema;
import com.gentics.mesh.plugin.forms.model.schema.PostedData;
import com.gentics.mesh.plugin.forms.model.schema.Validation;
import com.gentics.mesh.plugin.forms.service.FormService;
import com.gentics.mesh.plugin.forms.util.RxUtil;

import io.reactivex.Maybe;
import io.reactivex.Single;
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
	private static final Pattern BINARY_KEY_PATTERN = Pattern.compile("^\\$\\.properties\\[\"([^.]+)\"\\]\\.value$");
	private static final Pattern BINARY_KEY_MULTI_PATTERN = Pattern.compile("^\\$\\.properties\\[\"([^.]+)\"\\]\\.values\\[([\\d]+)\\]$");

	/**
	 * Extract the element ID and index from a FormGen binary part key.
	 * @param binaryPartKey key to handle
	 * @return pair of the element ID and index (which is 0 for single value elements) as optional
	 */
	public static Optional<Pair<String, Integer>> extractFieldId(String binaryPartKey) {
		try {
			String decoded = URLDecoder.decode(binaryPartKey, StandardCharsets.UTF_8);
			Matcher m = BINARY_KEY_PATTERN.matcher(decoded);
			if (m.matches()) {
				return Optional.of(Pair.of(m.group(1), 0));
			} else {
				m = BINARY_KEY_MULTI_PATTERN.matcher(decoded);
				if (m.matches()) {
					return Optional.of(Pair.of(m.group(1), Integer.parseInt(m.group(2))));
				}
			}
		} catch (Exception ignored) {
		}
		return Optional.empty();
	}

	public BinaryElement() {
	}

	protected BinaryElement(BinaryElement other, boolean deep) {
		super(other, deep);

	}

	@Override
	public void addFormData(JsonObject object, FieldContainer container, Function<FormElement, String> key) {
		if (isList()) {
			NodeFieldList field = container.getFields().getNodeFieldList(getName());
			if (field != null) {
				object.put(key.apply(this), field.getItems().stream().map(item -> item != null ? ("\"" + item.getPath() + "\"") : null).collect(Collectors.joining(",", "[", "]")));
			}
		} else {
			NodeField field = container.getFields().getNodeField(getName());
			if (field != null) {
				object.put(key.apply(this), field.getPath());
			}
		}
	}

	@Override
	public void addFieldSchema(FieldSchemaContainer container, List<String> fieldNames) {
		if (isList()) {
			container.addField(new ListFieldSchemaImpl().setListType("node").setAllowedSchemas(SchemaInitializer.MULTIBINARY_ITEM_SCHEMA_NAME).setName(getName()).setLabel(getName()));
		} else {
			container.addField(new NodeFieldSchemaImpl().setAllowedSchemas(SchemaInitializer.MULTIBINARY_ITEM_SCHEMA_NAME).setName(getName()).setLabel(getName()));

			// if the old field was contained in the schema, it must not be removed
			String oldFieldName = FormService.OLD_BINARY_FIELD_PATTERN.formatted(getName());
			if (fieldNames.contains(oldFieldName)) {
				container.addField(new BinaryFieldSchemaImpl().setName(oldFieldName).setLabel(getName()));
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Maybe<ValidationError> validate(FormDataCreateContext ctx, Vertx rxVertx, OkHttpClient httpClient, FormUiSchema uiSchema) {
		var fileUploads = ctx.getFileUploads();
		var translations = ctx.getTranslations();

		Set<FileUpload> myUploads = getFileUploads(fileUploads);
		if (myUploads.isEmpty()) {
			if (isMandatory()) {
				if (ctx.getFormDataNode() != null && hasStoredBinary(ctx.getFormDataNode())) {
					return RxUtil.emptyMaybe();
				}
				return Maybe.just(new ValidationError()
					.setName(getName())
					.setTitle(I18nHelper.get(translations, "form_validation_error_missing_mandatory_field_title"))
					.setMessage(I18nHelper.get(translations, "form_validation_error_missing_mandatory_field", getLabel(ctx.getLanguage(), uiSchema))));
			} else {
				return RxUtil.emptyMaybe();
			}
		}

		int maxFileSize = Optional.ofNullable(validation).map(Validation::getBinary)
				.map(BinaryValidation::getMaxFilesize).orElse(0);
		for (FileUpload fileUpload : myUploads) {
			if (maxFileSize > 0 && fileUpload.size() > (maxFileSize * 1024 * 1024)) {
				fileUploads.remove(fileUpload);
				return Maybe.just(new ValidationError()
					.setName(getName())
					.setTitle(I18nHelper.get(translations, "form_validation_error_file_size_title"))
					.setMessage(I18nHelper.get(translations, "form_validation_error_file_size", fileUpload.fileName(), Long.toString(maxFileSize))));
			}
		}

		// we only need to validate, if either allowed filetypes or allowed filenames are set
		boolean validate = Optional.ofNullable(validation).map(Validation::getBinary).map(binaryValidation -> {
			return CollectionUtils.isNotEmpty(binaryValidation.getAllowedFileTypes())
					|| CollectionUtils.isNotEmpty(binaryValidation.getAllowedFileNames());
		}).orElse(false);

		if (validate) {
			boolean useMagicByteCheck = Optional.ofNullable(getConfiguration()).map(FormsConfig::getFileUpload)
					.map(FileUploadOptions::isUseMagicByteCheck).orElse(false);

			return Single.zip(myUploads.stream().map(fileUpload -> fileHasAllowedContentType(fileUpload, useMagicByteCheck, rxVertx).map(allowedContentType -> {
				if (!allowedContentType && !fileHasAllowedName(fileUpload)) {
					fileUploads.remove(fileUpload);
					return Optional.of(fileUpload);
				} else {
					return Optional.empty();
				}
			})).collect(Collectors.toList()), uploads -> {
				uploads = Arrays.stream(uploads).map(Optional.class::cast).filter(Optional::isPresent).map(Optional::get).toArray();
				if (uploads.length > 0) {
					return Maybe.just(new ValidationError()
						.setName(getName())
						.setTitle(I18nHelper.get(translations, "form_validation_error_file_type_title"))
						.setMessage(I18nHelper.get(translations, "form_validation_error_file_type", 
								Arrays.stream(uploads).map(FileUpload.class::cast).map(FileUpload::fileName)
								.collect(Collectors.joining(", ")))));
				} else {
					return RxUtil.emptyMaybe();
				}
			}).flatMapMaybe(maybe -> (Maybe<ValidationError>) maybe).onErrorResumeNext(e -> {
				log.error("Could not check file type of uploaded file " 
						+ myUploads.stream().map(fileUpload -> fileUpload.fileName() + " (" + fileUpload.contentType() + ")").collect(Collectors.joining(", ")), e);
				return Maybe.just(new ValidationError()
					.setName(getName())
					.setTitle(I18nHelper.get(translations, "form_validation_error_file_type_title"))
					.setMessage(I18nHelper.get(translations, "form_validation_error_file_type", 
							myUploads.stream().map(FileUpload.class::cast).map(FileUpload::fileName)
							.collect(Collectors.joining(", ")))));
			});
		} else {
			return RxUtil.emptyMaybe();
		}
	}

	/**
	 * Check whether the given form data node already contains a stored binary for this element.
	 *
	 * <p>
	 * Binaries are stored as node field(s) referencing {@link SchemaInitializer#MULTIBINARY_ITEM_SCHEMA_NAME} nodes.
	 * Form data which has not been migrated yet still holds the value in the old binary field
	 * {@link FormService#OLD_BINARY_FIELD_PATTERN}.
	 * </p>
	 *
	 * @param formDataNode form data node to check
	 * @return true if a binary is stored for this element
	 */
	protected boolean hasStoredBinary(NodeResponse formDataNode) {
		var fields = formDataNode.getFields();

		if (isList()) {
			NodeFieldList field = fields.getNodeFieldList(getName());
			if (field != null && CollectionUtils.isNotEmpty(field.getItems())) {
				return true;
			}
		} else {
			NodeField field = fields.getNodeField(getName());
			if (field != null && StringUtils.isNotBlank(field.getUuid())) {
				return true;
			}
		}

		// fallback to the old binary field
		BinaryField oldField = fields.getBinaryField(FormService.OLD_BINARY_FIELD_PATTERN.formatted(getName()));
		return oldField != null && oldField.hasValues();
	}

	@Override
	public void addField(FieldContainer container, PostedData postedData, boolean errors) {
	}

	@Override
	public String getGraphQLName(List<String> schemaFields) {
		String graphQL = "%s (version: draft) { uuid fields { ... on form_binary_item { binary { fileName } } } }".formatted(getName());

		String oldFieldName = FormService.OLD_BINARY_FIELD_PATTERN.formatted(getName());
		if (CollectionUtils.isNotEmpty(schemaFields) && schemaFields.contains(oldFieldName)) {
			graphQL += "\n%s { fileName }".formatted(oldFieldName);
		}

		return graphQL;
	}

	@JsonIgnore
	@Override
	public String installFilter(String query) {
		if (isList()) {
			return "";
			// SUP-19994
			//return getName() + ": { anyMatch: { fields: { form_binary_item: { binary: { fileName: { like: \"%" + query + "%\" } } } } } }";
		} else {
			return "";
			// return getName() + ": { node: { fields: { form_binary_item: { binary: { fileName: {like: \"%" + query + "%\"} } } } } }";
		}
	}

	@Override
	@JsonIgnore
	public void setConfiguration(FormsConfig configuration) {
		super.setConfiguration(configuration);

		// set the binary validation
		validation = Optional.ofNullable(validation).orElseGet(() -> new Validation());
		validation.setBinary(Optional.ofNullable(validation.getBinary()).orElseGet(() -> new BinaryValidation()));
		BinaryValidation binaryValidation = validation.getBinary();

		// get the allowed types either from the binary validation or the default from the configuration
		Set<String> allowedTypes = Optional.ofNullable(binaryValidation.getAllowedTypes())
				.orElseGet(() -> getDefaultAllowedConfiguration());

		if (CollectionUtils.isNotEmpty(allowedTypes)) {
			// transform the allowed types in the set of allowed filetype regexes
			Set<String> fileTypes = new HashSet<>();
			Optional.ofNullable(getAllowedTypesConfiguration()).ifPresent(fileTypesMap -> {
				allowedTypes.stream().flatMap(type -> fileTypesMap.getOrDefault(type, Collections.emptySet()).stream())
						.forEach(fileTypes::add);
			});

			// add the already set filetypes from the binary validation
			Optional.ofNullable(binaryValidation.getAllowedFileTypes()).ifPresent(fileTypes::addAll);
			// and set the merged set back to binary validation
			binaryValidation.setAllowedFileTypes(fileTypes);

			// transform the allowed types in the set of allowed filename regexes
			Set<String> fileNames = new HashSet<>();
			Optional.ofNullable(getAllowedNamesConfiguration()).ifPresent(fileNamesMap -> {
				allowedTypes.stream().flatMap(type -> fileNamesMap.getOrDefault(type, Collections.emptySet()).stream())
						.forEach(fileNames::add);
			});

			// add the already set filenames from the binary validation
			Optional.ofNullable(binaryValidation.getAllowedFileNames()).ifPresent(fileNames::addAll);
			// and set the merged set back to binary validation
			binaryValidation.setAllowedFileNames(fileNames);
		}
	}

	@Override
	public String transformPostedValue(Object value) {
		if (value == null) {
			return null;
		}
		return JsonUtil.toJson(value);
	}

	@Override
	public Object transformToExternal(String value) {
		return JsonUtil.readValue(value, BinaryData.class);
	}

	@Override
	protected FormElement clone(boolean deep) {
		return new BinaryElement(this, deep);
	}

	/**
	 * Finds the file upload belonging to this element.
	 *
	 * <p>FormGen sends binary parts with a structured key (e.g. {@code properties.{ID}.value}),
	 * so {@link #extractFieldId(String)} is used to extract the element ID from the part name
	 * before comparing it to {@link #getName()}. Plain element IDs (legacy format) are still
	 * supported as a fallback.</p>
	 *
	 * @param fileUploads all file uploads from the current request
	 * @return the matching upload, or null if none found
	 */
	protected Set<FileUpload> getFileUploads(Set<FileUpload> fileUploads) {
		return fileUploads.stream().filter(fu -> {
			return extractFieldId(fu.name()).map(id -> Strings.CI.equals(getName(), id.getLeft())).orElse(false);
		}).collect(Collectors.toSet());
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
	protected Single<Boolean> fileHasAllowedContentType(FileUpload fileUpload, boolean useMagicByteCheck, Vertx rxVertx) {
		Set<String> allowedContentTypes = Optional.ofNullable(validation).map(Validation::getBinary)
				.map(BinaryValidation::getAllowedFileTypes).orElse(Collections.emptySet());

		Single<String> mimeTypeSingle = useMagicByteCheck
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
	protected boolean fileHasAllowedName(FileUpload fileUpload) {
		Set<String> allowedNames = Optional.ofNullable(validation).map(Validation::getBinary)
				.map(BinaryValidation::getAllowedFileNames).orElse(Collections.emptySet());

		return allowedNames.stream().filter(pattern -> fileUpload.fileName().matches(pattern)).findFirst().isPresent();
	}

	/**
	 * Get configured allowed file types or empty map
	 *
	 * @return map of configured allowed file types
	 */
	protected Map<String, Set<String>> getAllowedTypesConfiguration() {
		return Optional.ofNullable(getConfiguration()).map(FormsConfig::getFileUpload)
				.map(FileUploadOptions::getAllowedFileTypes).orElse(Collections.emptyMap());
		}

	/**
	 * Get configured allowed file names or empty map
	 *
	 * @return map of configured allowed file names
	 */
	protected Map<String, Set<String>> getAllowedNamesConfiguration() {
		return Optional.ofNullable(getConfiguration()).map(FormsConfig::getFileUpload)
				.map(FileUploadOptions::getAllowedFileNames).orElse(Collections.emptyMap());
		}

	/**
	 * Get configured default allowed categories or empty set
	 * @return set of default allowed
	 */
	protected Set<String> getDefaultAllowedConfiguration() {
		return Optional.ofNullable(getConfiguration()).map(FormsConfig::getFileUpload)
				.map(FileUploadOptions::getDefaultAllowed).orElse(Collections.emptySet());
		}
		}
