package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.plugin.forms.model.schema.FormData;
import com.gentics.mesh.plugin.forms.model.schema.FormSchema;
import com.gentics.mesh.plugin.forms.model.schema.FormUiSchema;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.config.InjectionPreventionMode;
import com.gentics.mesh.plugin.forms.config.SecurityOptions;
import com.gentics.mesh.plugin.forms.i18n.I18nHelper;
import com.gentics.mesh.util.StreamUtil;
import io.reactivex.Single;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.reactivex.core.Vertx;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Form
 */
public class Form implements RestModel {
	/**
	 * Name of the schema field containing the errors
	 */
	public final static String ERRORS_FIELD_NAME = "errors";

	public final static String ERROR_FIELD_NAME = "error";
	public static final String FIELD_VALUE = "value";

	private String uuid;

	private String name;

	private String language;

	private FormType type;

	private String email;

	private String mailsubject;

	private String mailtemp;

	private String mailsource;

	private String successurl;

	@JsonProperty("schema")
	private FormSchema schema;

	@JsonProperty("uiSchema")
	private FormUiSchema uiSchema;

	@JsonProperty("data")
	private FormData data;

	private String currentPage;

	private String templateContext;

	private String downloadBaseUrl;

	private Map<String, Object> properties = new LinkedHashMap<>();

	public Form() {
	}

	/**
	 * Copy constructor
	 *
	 * <p>
	 *     Note that the copy is shallow, and the list of form elements does
	 *     <em>not</em> contain copies of this forms elements.
	 * </p>
	 *
	 * @param other The form object to copy
	 */
	private Form(Form other) {
		this.uuid = other.uuid;
		this.name = other.name;
		this.language = other.language;
		this.type = other.type;
		this.email = other.email;
		this.mailsubject = other.mailsubject;
		this.mailtemp = other.mailtemp;
		this.mailsource = other.mailsource;
		this.successurl = other.successurl;
		this.schema = other.schema;
		this.uiSchema = other.uiSchema;
		this.data = other.data;
		this.currentPage = other.currentPage;
		this.templateContext = other.templateContext;
		this.properties = other.properties == null ? new LinkedHashMap<>() : new LinkedHashMap<>(other.properties);
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * Form name
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the form name
	 * @param name name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Form language
	 * @return language
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * Set the form language
	 * @param language language
	 */
	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * Get the form type.
	 *
	 * @return The form type
	 */
	@Nonnull
	public FormType getType() {
		if (type == null) {
			type = FormType.GENERIC;
		}

		return type;
	}

	/**
	 * Set the form type.
	 *
	 * @param type The form type
	 */
	public void setType(FormType type) {
		this.type = type;
	}

	/**
	 * Get email addresses for sending verification mails
	 * @return email addresses (comma separated)
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Set email addresses
	 * @param email email addresses
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * Get email subject for verification mails
	 * @return email subject
	 */
	public String getMailsubject() {
		return mailsubject;
	}

	/**
	 * Set email subject
	 * @param mailsubject email subject
	 */
	public void setMailsubject(String mailsubject) {
		this.mailsubject = mailsubject;
	}

	/**
	 * Get admin email template path.
	 * @return mail template path
	 */
	public String getMailtemp() {
		return mailtemp;
	}

	/**
	 * Set admin email template path.
	 * @param mailtemp mail template path
	 */
	public void setMailtemp(String mailtemp) {
		this.mailtemp = mailtemp;
	}


	/**
	 * Get the admin mail template source.
	 * @return The admin mail template source.
	 */
	public String getMailsource() {
		return mailsource;
	}

	/**
	 * Set the admin mail template source.
	 */
	public void setMailsource(String mailsource) {
		this.mailsource = mailsource;
	}

	/**
	 * Get the successurl
	 * @return successurl
	 */
	public String getSuccessurl() {
		return successurl;
	}

	/**
	 * Set the successurl
	 * @param successurl successurl
	 */
	public void setSuccessurl(String successurl) {
		this.successurl = successurl;
	}

	/**
	 * Get the schema definition of this form.
	 * @return form schema
	 */
	public FormSchema getSchema() {
		return schema;
	}

	/**
	 * Set the schema definition of this form.
	 * @param schema form schema
	 */
	public void setSchema(FormSchema schema) {
		this.schema = schema;
	}

	/**
	 * Get the UI schema definition of this form.
	 * @return ui schema
	 */
	public FormUiSchema getUiSchema() {
		return uiSchema;
	}

	/**
	 * Set the UI schema definition of this form.
	 * @param uiSchema ui schema
	 */
	public void setUiSchema(FormUiSchema uiSchema) {
		this.uiSchema = uiSchema;
	}

	/**
	 * Get the data (pre-filled / current control values) of this form.
	 * @return form data
	 */
	public FormData getData() {
		return data;
	}

	/**
	 * Set the data (pre-filled / current control values) of this form.
	 * @param data form data
	 */
	public void setData(FormData data) {
		this.data = data;
	}

	/**
	 * Create the correct value for the {@link com.gentics.mesh.plugin.forms.service.FormDataService#COOKIE_NAME_DATA form data}
	 * cookie.
	 *
	 * <p>
	 *     The value is the UUID of the respective form data node, and if
	 *     available the name of the last submitted form page.
	 * </p>
	 *
	 * @param dataNodeUuid The UUID for the processed form data node
	 * @return The value for the form data cookie
	 */
	public String getDataCookie(String dataNodeUuid) {
		StringBuilder cookieVal = new StringBuilder(dataNodeUuid);

		if (StringUtils.isNotBlank(currentPage)) {
			cookieVal
				.append('-')
				.append(currentPage);
		}

		return cookieVal.toString();
	}

	/**
	 * Get all form elements as flattened stream.
	 * Derived on-demand from {@code schema.properties}.
	 *
	 * @return stream of all form elements
	 */
	public Stream<FormElement> flattened() {
		if (schema == null || schema.getProperties() == null) {
			return Stream.empty();
		}
		return schema.getProperties().values().stream().flatMap(FormElement::flattened);
	}

	/**
	 * Get the template context.
	 * @return The template context
	 */
	public String getTemplateContext() {
		return templateContext;
	}

	/**
	 * Set the template context.
	 * @param templateContext The template context
	 */
	public void setTemplateContext(String templateContext) {
		this.templateContext = templateContext;
	}

	/**
	 * Get the download base URL.
	 * @return The download base URL.
	 */
	@Nonnull
	public String getDownloadBaseUrl() {
		if (downloadBaseUrl == null) {
			downloadBaseUrl = "";
		}

		return downloadBaseUrl;
	}

	/**
	 * Set the download base URL.
	 * @param downloadBaseUrl The download base URL.
	 */
	public void setDownloadBaseUrl(String downloadBaseUrl) {
		this.downloadBaseUrl = downloadBaseUrl;
	}

	/**
	 * Get the map of additional properties.
	 * @return The map of additional properties
	 */
	@JsonAnyGetter
	public Map<String, Object> getAny() {
		if (properties == null) {
			properties = new LinkedHashMap<>();
		}

		return properties;
	}

	/**
	 * Set the specified additional property to the given value.
	 *
	 * <p>
	 *     This will store {@code value} under {@code key} in the
	 *     {@link #properties} map.
	 * </p>
	 *
	 * @param key The name of the additional property
	 * @param value The value of the additional property
	 */
	@JsonAnySetter
	public void setProperty(String key, Object value) {
		if (StringUtils.isNotBlank(key)) {
			getAny().put(key, value);
		}
	}

	/**
	 * Transform all form elements into schema fields in the container
	 * @param container container (will be modified)
	 */
	public void addFieldSchemas(FieldSchemaContainer container) {
		flattened().forEach(elem -> elem.addFieldSchema(container));
	}

	/**
	 * Transform the posted data into fields, that are added to the container
	 * @param container field container
	 * @param postedData posted data
	 * @param errors true, if validation errors were detected
	 */
	public void addFields(FieldContainer container, MultiMap postedData, boolean errors) {
		flattened().filter(FormElement::isActive).forEach(elem -> {
			elem.addField(container, postedData, errors);
		});
	}

	/**
	 * Validate the form data
	 * @param postedData posted data
	 * @param fileUploads fileuploads
	 * @param formDataNode form data node (null if the form data are not yet saved)
	 * @param rxVertx vert.x instance for the utility tasks
	 * @param httpClient a HTTP client for an external validation
	 * @return list containing error messages (empty if data is valid)
	 */
	public Single<Map<String, ValidationError>> validate(MultiMap postedData, Set<FileUpload> fileUploads, NodeResponse formDataNode, Vertx rxVertx, OkHttpClient httpClient) {
		// only filter out the elements, which have not been POSTed, if the form contains multiple pages
		Predicate<FormElement> filter = null;
		if (isMultipage()) {
			filter = elem -> elem.isPosted(postedData, fileUploads);
		} else {
			filter = elem -> true;
		}
		return Single.zip(
				StreamUtil.toIterable(
					flattened()
						.filter(FormElement::isActive)
						.filter(filter)
						.map(elem -> elem.validate(postedData, fileUploads, formDataNode, rxVertx, httpClient, uiSchema)
								.map(error -> Pair.of(elem.getName(), error))
								.toSingle(Pair.of(null, null)))),
					errors -> Arrays.stream(errors)
						.map(Pair.class::cast)
						.filter(pair -> pair.getKey() != null && pair.getValue() != null)
						.collect(Collectors.toMap(
								pair -> (String) pair.getKey(),
								pair -> (ValidationError) pair.getValue()))
				);
	}

	/**
	 * Either sanitize unsafe input or create validation errors for every
	 * posted element with unsafe data.
	 *
	 * @param postedData The posted form data.
	 * @param options The security configuration.
	 * @return A map from element names to {@code ValidationErrors} for elements
	 * 		with unsafe values. Note that only elements which where not already
	 * 		sanitized are contained in this map.
	 */
	public Map<String, ValidationError> preventInjection(MultiMap postedData, SecurityOptions options) {
		InjectionPreventionMode mode = options.getInjectionPreventionMode();
		Map<Pattern, String> rules = options.getCompiledSanitationRules();

		if (mode == InjectionPreventionMode.NONE) {
			return Collections.emptyMap();
		}

		if (mode == InjectionPreventionMode.SANITIZE || mode == InjectionPreventionMode.SANITIZE_MARKUP) {
			sanitizeValues(postedData, mode, rules);

			return Collections.emptyMap();
		}

		Predicate<String> blacklistFilter = getBlacklistFilter(mode == InjectionPreventionMode.REJECT_MARKUP, rules.keySet());

		return postedData.names().stream()
			.filter(name -> postedData.getAll(name).stream().map(s -> s == null ? "" : s).anyMatch(blacklistFilter))
			.map(invalidName -> Pair.of(
				invalidName,
				new ValidationError()
					.setName(invalidName)
					.setTitle(I18nHelper.get(Locale.GERMAN, "error.validation.unsafe_value.title"))
					.setMessage(I18nHelper.get(Locale.GERMAN, "error.validation.unsafe_value", invalidName))))
			.collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}

	/**
	 * Create combined predicate that will return {@code true} for values which
	 * are considered unsafe.
	 *
	 * @param checkMarkup Whether markup should be considered unsafe.
	 * @param blacklist A list of regex patterns that are considered unsafe.
	 * @return A combined predicate returning {@code true} for all values
	 * 		that are considered unsafe.
	 */
	private Predicate<String> getBlacklistFilter(boolean checkMarkup, Collection<Pattern> blacklist) {
		Predicate<String> markupValidator = checkMarkup
			? str -> str != null && !str.trim().equals(Jsoup.parse(str).text().trim())
			: str -> false;

		return blacklist.stream()
			.map(pattern -> (Predicate<String>) s -> s != null && pattern.matcher(s).find())
			.reduce(markupValidator, Predicate::or);
	}

	/**
	 * Sanitize values in {@code postedData} according to the given rules.
	 *
	 * <p>
	 *     When {@code mode} is {@link InjectionPreventionMode#SANITIZE_MARKUP}
	 *     this method will also remove all markup.
	 * </p>
	 *
	 * <p>
	 *     The {@code rules} is a mapping from regex patterns to replacements.
	 *     For each processed value a {@link java.util.regex.Matcher} is created
	 *     from the given pattern and
	 *     {@link java.util.regex.Matcher#replaceAll(String)} called with the
	 *     replacement.
	 * </p>
	 *
	 * <p>
	 *     Note that if the regex contains captured groups, the replacement can
	 *     use the captured values with {@code "$n"} where n is the group
	 *     number. E.g.: "\\.(\\S)" -> ". $1" will replace "evil.site" with
	 *     "evil. site".
	 * </p>
	 *
	 * @param postedData The posted form data.
	 * @param mode The sanitation mode.
	 * @param rules The sanitation rules. A mapping from regex pattern to
	 * 		replacement values.
	 */
	private void sanitizeValues(MultiMap postedData, InjectionPreventionMode mode, Map<Pattern, String> rules) {
		Function<String, String> markupRemover = mode == InjectionPreventionMode.SANITIZE_MARKUP
			? str -> Jsoup.parse(str).text()
			: Function.identity();
		Function<String, String> sanitizer = rules.entrySet().stream()
			// Create function that replaces pattern with replacement value.
			.map(e -> (Function<String, String>) (String v) -> v == null ? "" : e.getKey().matcher(v).replaceAll(e.getValue()))
			// Combine all replacement functions.
			.reduce(markupRemover, Function::andThen);

		for (String name : postedData.names()) {
			List<String> sanitizedValues = postedData.getAll(name).stream()
				.map(s -> s == null ? "" : s)
				.map(sanitizer)
				.collect(Collectors.toList());

			postedData.set(name, sanitizedValues);
		}
	}

	/**
	 * Validate the definition of this form, by checking elements with the specified <code>types</code>.
	 *
	 * <p>
	 *     This is to be called before saving or updating a form.
	 * </p>
	 *
	 * <p>
	 *     The default validation for form elements will always succeed, so no error is returned.
	 * </p>
	 *
	 * @return The respective error or {@link Optional#empty()} when the form is valid
	 */
	public Optional<ValidationError> validateForm() {
		return flattened()
			.filter(FormElement::isActive)
			.map(FormElement::validateForm)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.findAny();
	}

	/**
	 * Check whether the form is completed (all formpages have been submitted)
	 * @param formDataNode form data node
	 * @return true if the form is completed, false if not
	 */
	public boolean isCompleted(NodeResponse formDataNode) {
		return flattened().allMatch(elem -> elem.isCompleted(formDataNode));
	}

	/**
	 * Get the form data and error messages (if any) from the container (which should be a form data node)
	 * @param container form data node
	 * @return form data json
	 */
	public JsonObject getFormDataJson(FieldContainer container) {
		String errorsJson = container.getFields().getStringField(ERRORS_FIELD_NAME).getString();
		JsonObject errors = (errorsJson != null && !"".equals(errorsJson)) ? new JsonObject(errorsJson) : new JsonObject();

		JsonObject object = new JsonObject();
		flattened().forEach(elem -> {
			if (elem.getName() != null) {
				JsonObject elemJson = new JsonObject();
				elem.addFormData(elemJson, container, e -> FIELD_VALUE);
				object.put(elem.getName(), elemJson);
				JsonObject elemError = errors.getJsonObject(elem.getName());
				if (elemError != null) {
					elemJson.put(ERROR_FIELD_NAME, elemError);
				}
			}
		});
		return object;
	}

	/**
	 * Collect the poll results from the given form data into the specified
	 * results object.
	 *
	 * @param results The {@code JsonObject} to gather the results into
	 * @param formData The form data node containing
	 * @return The {@code results} object for fluent API.
	 */
	public JsonObject gatherPollResults(JsonObject results, FieldContainer formData) {
		flattened().forEach(elem -> {
			String name = elem.getName();

			if (StringUtils.isNotBlank(name)) {
				if (!results.containsKey(name)) {
					results.put(name, new JsonObject());
				}

				elem.addPollData(results.getJsonObject(name), formData);
			}
		});

		return results;
	}

	/**
	 * Get the form data as reportable JSON array, with value labels resolved without a specific language.
	 *
	 * @param container form data node
	 * @return form data JSON array
	 */
	public JsonArray getReportableFormData(FieldContainer container) {
		return getReportableFormData(container, ignored -> true, null);
	}

	/**
	 * Get the form data as reportable JSON array, with value labels resolved without a specific language.
	 *
	 * @param container form data node
	 * @param filter additional filter for form elements
	 * @return form data JSON array
	 */
	public JsonArray getReportableFormData(FieldContainer container, Predicate<FormElement> filter) {
		return getReportableFormData(container, filter, null);
	}

	/**
	 * Get the form data as reportable JSON array. For {@link ValueListElement} fields, the human-readable
	 * label for each selected value is resolved using the given language. If {@code language} is
	 * {@code null} or not found in the label map, the first available label is used as a fallback.
	 *
	 * @param container form data node
	 * @param filter additional filter for form elements
	 * @param language BCP-47 language tag used to resolve option labels (e.g. {@code "de"}), may be {@code null}
	 * @return form data JSON array
	 */
	public JsonArray getReportableFormData(FieldContainer container, Predicate<FormElement> filter, String language) {
		JsonArray array = new JsonArray();

		flattened()
			.filter(elt -> elt.isReportable() && filter.test(elt))
			.forEach(elem -> {
				JsonObject elemObj = new JsonObject(elem.toJson())
					.put("isFile", elem instanceof BinaryElement);
				Object valueProperty = elemObj.remove(FIELD_VALUE);

				if (valueProperty != null) {
					// This means the element has a property named value, which
					// conflicts with field name for the actually submitted
					// value for this element. So we store it under a different
					// name. This will overwrite any field with the name
					// "valueProperty", so that name must not be used as a
					// custom property for form elements.
					elemObj.put(FIELD_VALUE + "Property", valueProperty);
				}

				elem.addFormData(elemObj, container, e -> FIELD_VALUE);

				array.add(addValueLabels(elem, elemObj, language));
			});

		return array;
	}

	/**
	 * Add the labels for selected values for the given {@link ValueListElement} to the given result object.
	 * The label is resolved from the static options using the provided language; if the language is
	 * {@code null} or not present in the label map, the first available label is used as a fallback.
	 *
	 * <p>The resolved label is stored in {@code resultObj} under the key {@code valueLabel}.</p>
	 *
	 * @param elem      the form element being processed (no-op for non-{@link ValueListElement} elements)
	 * @param resultObj the JSON object to add the value label to
	 * @param language  BCP-47 language tag used to resolve option labels, may be {@code null}
	 * @return the modified {@code resultObj}
	 */
	private JsonObject addValueLabels(FormElement elem, JsonObject resultObj, String language) {
		if (!(elem instanceof ValueListElement)) {
			return resultObj;
		}

		ValueListElement listElem = (ValueListElement) elem;
		Map<String, String> valueLabels = listElem.getSelectOptions().stream()
			.collect(ValueListElement.optionsMapCollector(language));

		if (elem.isList()) {
			JsonArray values = resultObj.getJsonArray(FIELD_VALUE);

			if (values == null) {
				values = new JsonArray();
			}

			JsonArray labels = values.stream()
				.sequential()
				.filter(v -> v instanceof String)
				.map(Objects::toString)
				.map(v -> valueLabels.getOrDefault(v, v))
				.collect(Collector.of(JsonArray::new, JsonArray::add, JsonArray::addAll));

			if (labels.isEmpty()) {
				resultObj.put("valueLabel", "");
			} else {
				resultObj.put("valueLabel", labels);
			}

			return resultObj;
		}

		String value = resultObj.getString(FIELD_VALUE);

		return resultObj.put("valueLabel", valueLabels.getOrDefault(value, value));
	}

	/**
	 * Get the names of the form elements for use in a GraphQL query.
	 *
	 * <strong>Note</strong> that elements without a name will <em>not</em> be
	 * contained in the resulting set.
	 *
	 * @return A set of names of form elements containing submitted data.
	 */
	public List<String> getReportableElementNamesForGraphQL() {
		return flattened()
			.filter(FormElement::isReportable)
			.map(FormElement::getGraphQLName)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	/**
	 * Get the names of reportable form elements, as well as whether the field
	 * is a binary or not.
	 *
	 * <strong>Note</strong> that elements without a name will <em>not</em> be
	 * contained in the resulting set.
	 *
	 * @return A mapping of reportable field names, to a boolean indicating
	 * 		if the field is a file element or not
	 */
	public Map<String, Boolean> getReportableElementsInfo() {
		return flattened()
			.filter(elt -> elt.isReportable() && StringUtils.isNotBlank(elt.getName()))
			.collect(Collectors.toMap(FormElement::getName, e -> e instanceof BinaryElement, (u, v) -> u, LinkedHashMap::new));
	}

	/**
	 * Get a map of field name -> field label for all reportable fields
	 * @return map of name -> label
	 */
	public Map<String, String> getReportableFields() {
		return flattened()
			.filter(FormElement::isReportable)
			.collect(Collectors.toMap(FormElement::getName, FormElement::getLabelOrName));
	}

	/**
	 * Check whether the form contains pages
	 * @return true if at least one FormpageElement is found
	 */
	public boolean isMultipage() {
		if(uiSchema != null && uiSchema.getPages() != null && uiSchema.getPages().size() > 1){
			return true;
		}
		return false;
	}


	/**
	 * Extract the name of the last submitted page if available.
	 * @param postedData The posted form data
	 */
	public void updateFormPage(MultiMap postedData) {
		flattened()
			.filter(elt -> elt.isActive() && elt instanceof FormpageElement && "1".equals(elt.getSingleFormData(postedData)))
			.map(FormElement::getName)
			.findFirst()
			.ifPresent(page -> currentPage = page);
	}

	/**
	 * Create a copy of this form containing only active form elements.
	 * Filters {@code schema.properties} by {@link FormElement#isActive()}.
	 *
	 * @return A copy of this form with only active elements in its schema
	 */
	public Form removeInactiveElements() {
		Form copy = new Form(this);

		if (copy.schema != null && copy.schema.getProperties() != null) {
			Map<String, FormElement> activeProperties = copy.schema.getProperties().entrySet().stream()
				.filter(entry -> entry.getValue().isActive())
				.collect(Collectors.toMap(
					Map.Entry::getKey,
					Map.Entry::getValue,
					(u, v) -> u,
					LinkedHashMap::new));
			FormSchema filteredSchema = new FormSchema();
			filteredSchema.setKey(copy.schema.getKey());
			filteredSchema.setVersion(copy.schema.getVersion());
			filteredSchema.setProperties(activeProperties);
			copy.schema = filteredSchema;
		}

		return copy;
	}

	/**
	 * Prepare a JSON object containing all necessary information about the form
	 * so that it can be rendered with handlebars.
	 *
	 * @param pathPrefix The path prefix for the action URL
	 * @param configuration The form configuration
	 * @return A JSON object containing render information of the form
	 */
	public JsonObject prepareRenderContext(String pathPrefix, FormsConfig configuration) {
		return new JsonObject(toJson())
			// add id
			.put("id", uuid)
			// add property "multipage"
			.put("multipage", isMultipage())
			// add options
			.put("options", JsonObject.mapFrom(configuration))
			// add path prefix
			.put("pathPrefix", pathPrefix)
			// add elements map
			.put("elementMap", elementMap());
	}

	/**
	 * Create a mapping from form element names to their respective JSON
	 * representation.
	 *
	 * @return A mapping from form element names to their respective JSON
	 * 		representation
	 */
	public Map<String, Object> elementMap() {
		if (schema == null || schema.getProperties() == null) {
			return Collections.emptyMap();
		}

		return schema.getProperties().entrySet().stream()
			.collect(Collectors.toMap(
				Map.Entry::getKey,
				entry -> {
					JsonObject json = new JsonObject(entry.getValue().toJson());
					json.remove("name"); // TODO f-gpu-2257: ist es wirklich notwendig das zu entfernen?
					return json;
				}));
	}

	@Override
	public String toString() {
		return toJson();
	}
}
