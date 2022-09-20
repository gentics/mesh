package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
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

	private FormType type;

	private String email;

	private String mailsubject;

	private String mailtemp;

	private String mailsource;

	private String successurl;

	private List<FormElement> elements = new ArrayList<>();

	private String currentPage;

	private String templateContext;

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
		this.type = other.type;
		this.email = other.email;
		this.mailsubject = other.mailsubject;
		this.mailtemp = other.mailtemp;
		this.mailsource = other.mailsource;
		this.successurl = other.successurl;
		this.elements = other.elements == null ? new ArrayList<>() : new ArrayList<>(other.elements);
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
	 * Get the form elements (direct members of the form)
	 * @return form elements
	 */
	public List<FormElement> getElements() {
		return elements;
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
	 * Get all form elements as flattened stream
	 * @return stream of all form elements
	 */
	public Stream<FormElement> flattened() {
		return elements.stream().flatMap(FormElement::flattened);
	}

	/**
	 * Set the direct form elements
	 * @param elements form elements
	 */
	public void setElements(List<FormElement> elements) {
		this.elements = elements;
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
	 * @return list containing error messages (empty if data is valid)
	 */
	public Map<String, ValidationError> validate(MultiMap postedData, Set<FileUpload> fileUploads, NodeResponse formDataNode) {
		Map<String, ValidationError> errors = new HashMap<>();

		// only filter out the elements, which have not been POSTed, if the form contains multiple pages
		Predicate<FormElement> filter = null;
		if (isMultipage()) {
			filter = elem -> elem.isPosted(postedData, fileUploads);
		} else {
			filter = elem -> true;
		}

		flattened().filter(FormElement::isActive).filter(filter).forEach(elem -> {
			ValidationError elemError = elem.validate(postedData, fileUploads, formDataNode);
			if (elemError != null) {
				errors.put(elem.getName(), elemError);
			}
		});
		return errors;
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
	 * Get the form data as reportable json object
	 * @param container form data node
	 * @return form data json
	 */
	public JsonArray getReportableFormData(FieldContainer container) {
		return getReportableFormData(container, ignored -> true);
	}

	/**
	 * Get the form data as reportable json object
	 * @param container form data node
	 * @param filter additional filter for form elements
	 * @return form data json
	 */
	public JsonArray getReportableFormData(FieldContainer container, Predicate<FormElement> filter) {
		JsonArray array = new JsonArray();

		flattened()
			.filter(elt -> elt.isReportable() && filter.test(elt))
			.forEach(elem -> {
				JsonObject elemObj = new JsonObject(elem.toJson())
					.put("isFile", elem instanceof FileElement);
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

				array.add(addValueLabels(elem, elemObj));
			});

		return array;
	}

	/**
	 * Add the labels for selected values for the given {@link ValueListElement}
	 * to the given result object.
	 *
	 * <p>
	 *     The label will be stored in {@code resultObj} under the key
	 *     {@code valueLabel}.
	 * </p>
	 *
	 * @param elem The form element being processed (this method does nothing for elements other than {@code ValueListElement})
	 * @param resultObj The JSON object to add the value label to
	 * @return The modified {@code resultObj}
	 */
	private JsonObject addValueLabels(FormElement elem, JsonObject resultObj) {
		if (!(elem instanceof ValueListElement)) {
			return resultObj;
		}

		ValueListElement listElem = (ValueListElement) elem;
		Map<String, String> valueLabels = listElem.getOptions().stream()
			.collect(ValueListElement.OPTIONS_MAP_COLLECTOR);

		if (elem.isMultivalue()) {
			JsonArray values = resultObj.getJsonArray(FIELD_VALUE);
			JsonArray labels = values.stream()
				.sequential()
				.filter(v -> v instanceof String)
				.map(Objects::toString)
				.map(v -> valueLabels.getOrDefault(v, v))
				.collect(Collector.of(JsonArray::new, JsonArray::add, JsonArray::addAll));

			return resultObj.put("valueLabel", labels);
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
			.collect(Collectors.toMap(FormElement::getName, e -> e instanceof FileElement, (u, v) -> u, LinkedHashMap::new));
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
		return flattened().anyMatch(elem -> elem instanceof FormpageElement);
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
	 * Create a copy of this form with only active form elements.
	 *
	 * @return A copy of this form with only active form elements
	 */
	public Form removeInactiveElements() {
		Form copy = new Form(this);

		copy.elements = elements.stream()
			.filter(FormElement::isActive)
			.map(FormElement::removeInactiveElements)
			.collect(Collectors.toList());

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
		return flattened()
			.map(elt -> {
				JsonObject json = new JsonObject(elt.toJson());
				Object name = json.remove("name");

				return Pair.of(name == null ? "" : name.toString(), json);
			})
			.collect(Collectors.toMap(Pair::getKey, Pair::getValue));
	}

	@Override
	public String toString() {
		return toJson();
	}
}
