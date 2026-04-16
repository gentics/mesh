package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.i18n.I18nHelper;
import com.gentics.mesh.plugin.forms.model.schema.FormUiSchema;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.plugin.forms.util.RxUtil;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.reactivex.core.Vertx;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract form element
 */
public abstract class FormElement implements RestModel {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	protected String name;

	protected String label;

	protected String type;

	protected boolean active = true;

	protected boolean mandatory;

	@JsonProperty("isList")
	protected boolean isList;

	protected String validation; //TODO f-gpu-2257

	@JsonIgnore
	protected List<FormElement> elements = new ArrayList<>(); // TODO 8.5

	protected Map<String, Object> properties = new LinkedHashMap<>();

	@JsonIgnore
	protected FormElement container; // TODO delete 8.5

	@JsonIgnore
	protected FormsConfig configuration;

	@JsonIgnore
	protected String key;

	// This will be integrated inside "data" for frontend, and will be removed inside "schema"
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	protected SchemaFormGridOptions formGridOptions;

	public FormElement() {
	}

	/**
	 * Copy constructor.
	 *
	 * @param other The form element ot copy
	 * @param deep Whether to create a deep copy of the child elements
	 */
	protected FormElement(FormElement other, boolean deep) {
		this.name = other.name;
		this.label = other.label;
		this.type = other.type;
		this.active = other.active;
		this.mandatory = other.mandatory;
		this.isList = other.isList;
		this.validation = other.validation;
		this.elements = cloneField(other.elements, deep);
		this.properties = cloneField(other.properties, deep);
		this.container = other.container;
		this.configuration = other.configuration;
		this.formGridOptions = other.formGridOptions;
	}

	/**
	 * Element name
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the element name
	 * @param name name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Element label
	 * @return label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Set the element label
	 * @param label label
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Element type
	 * @return type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Set the element type
	 * @param type type
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * If the label is not blank, return the label, otherwise return the name (which must not be null)
	 * @return label (if not null) or name
	 */
	@JsonIgnore
	public String getLabelOrName() {
		return StringUtils.isNotBlank(label) ? label : name;
	}

	/**
	 * Get active flag
	 * @return active flag
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Set the active flag
	 * @param active active flag
	 */
	public void setActive(boolean active) {
		this.active = active;
	}

	/**
	 * Get mandatory flag
	 * @return mandatory flag
	 */
	public boolean isMandatory() {
		return mandatory;
	}

	/**
	 * Set the mandatory flag
	 * @param mandatory mandatory flag
	 */
	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}

	/**
	 * If the value of this element is multivalue/a list
	 * @return isList flag
	 */
	@JsonProperty("isList")
	public boolean isList() {
		return isList;
	}

	/**
	 * Set the isList flag
	 * @param isList isList flag
	 */
	public void setIsList(boolean isList) {
		this.isList = isList;
	}

	/**
	 * Validation type
	 * @return validation
	 */
	public String getValidation() {
		return validation;
	}

	/**
	 * Set validation type
	 * @param validation validation
	 */
	public void setValidation(String validation) {
		this.validation = validation;
	}

	/**
	 * Jackson setter that handles both the old String form ("datepicker") and the new
	 * object form ({"minLength": 2, "maxLength": 100}) of the validation field.
	 * When an object is received it is stored in the properties map under "validation"
	 * for downstream use; the String validation field is left null.
	 *
	 * @param value String or Map value from JSON
	 * 
	 */
	@JsonSetter("validation")
	public void setValidationFromJson(Object value) {
		if (value instanceof String) {
			this.validation = (String) value;
		} else if (value != null) {
			// new object form — store for downstream access, don't touch String field
			this.properties.put("validation", value);
		}
	}

	/**
	 * Get direct child elements
	 * @return elements list
	 */
	public List<FormElement> getElements() {
		return elements;
	}

	/**
	 * Set the direct child elements
	 * @param elements elements list
	 */
	public void setElements(List<FormElement> elements) {
		this.elements = elements;
		this.elements.stream().forEach(element -> element.setContainer(this));
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
	 * Transform the element into a field schema and add it to the container
	 * @param container container (will be modified)
	 */
	public void addFieldSchema(FieldSchemaContainer container) {
		if (isList()) {
			container.addField(new ListFieldSchemaImpl().setListType(FieldTypes.STRING.toString()).setName(name).setLabel(name));
		} else {
			container.addField(new StringFieldSchemaImpl().setName(name).setLabel(name));
		}
	}

	/**
	 * Check whether any data were posted for this element
	 * @param postedData posted data
	 * @param fileUploads fileuploads
	 * @return true if data were posted
	 */
	public boolean isPosted(MultiMap postedData, Set<FileUpload> fileUploads) {
		// if the element has a container, it is supposed to be posted, when the container was posted
		if (container != null) {
			return container.isPosted(postedData, fileUploads);
		}

		if (getName() == null) {
			return false;
		}
		for (String alternative : Arrays.asList(getName(), String.format("%s[]", getName()))) {
			if (postedData.contains(alternative)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Validate the definition of this form element.
	 *
	 * <p>
	 *     This is to be called before saving or updating a form.
	 * </p>
	 *
	 * <p>
	 *     The default validation for form elements will always succeed, so no error is returned.
	 * </p>
	 *
	 * @return The default validation will always return {@link Optional#empty()}.
	 */
	public Optional<ValidationError> validateForm() {
		return Optional.empty();
	}

	/**
	 * Validate the posted data
	 * @param postedData posted data
	 * @param fileUploads fileuploads
	 * @param formDataNode form data node (may be null)
	 * @param rxVertx Vert.x instance for the utility tasks
	 * @param httpClient a HTTP client for possible external validation calls
	 * @return validation error or null if posted data is valid
	 */
	public Maybe<ValidationError> validate(MultiMap postedData, Set<FileUpload> fileUploads, NodeResponse formDataNode, Vertx rxVertx, OkHttpClient httpClient, FormUiSchema uischema) {


		List<String> values = new ArrayList<>();
		if (isList()) {
			List<String> data = getMultiFormData(postedData);
			if (data != null) {
				values.addAll(data);
			}
		} else {
			String data = getSingleFormData(postedData);
			if (StringUtils.isNotBlank(data)) {
				values.add(data);
			}
		}

		// check mandatory fields, that are not filled
		if (mandatory) {
			if (values.stream().noneMatch(v -> v != null && !"".equals(v))) {
				return Maybe.just(new ValidationError().setName(getName()).setTitle(I18nHelper.get(Locale.GERMAN, "error.mandatory_field_not_filled.title"))
						.setMessage(I18nHelper.get(Locale.GERMAN, "error.mandatory_field_not_filled", getLabel())));
			}
		}

		Validator validator = Validator.get(getValidation());
		if (validator != null) {
			return validator.validate(getName(), values);
		}
// Apart from user defined validation rules, some elements might have restrictions on the values that can
		// be submitted (e.g. value list elements, like select boxes, only accepting values from their defined options).
		var invalidValue = values.stream().filter(this::isInvalidValue).findAny();

		if (invalidValue.isPresent()) {
			return Maybe.just(new ValidationError()
				.setName(getName())
				.setTitle(I18nHelper.get(Locale.GERMAN, "error.invalid_value.title"))
				.setMessage(I18nHelper.get(Locale.GERMAN, "error.invalid_value", getName(), invalidValue.get())));
		}

		return RxUtil.emptyMaybe();
	}

	/**
	 * Transform the posted data into a field in the container
	 * @param container container (will be modified)
	 * @param postedData posted data
	 * @param errors true if validation errors were detected
	 */
	public void addField(FieldContainer container, MultiMap postedData, boolean errors) {

		if (isList()) {
			List<String> values = getMultiFormData(postedData);

			if (values != null) {
				values = new ArrayList<>(values);
				values.removeIf(this::isInvalidValue);
				container.getFields().put(getName(), new StringFieldListImpl().setItems(values));
			}
		} else {
			String value = getSingleFormData(postedData);
			if (value != null && !isInvalidValue(value)) {
				container.getFields().put(getName(), new StringFieldImpl().setString(value));
			}
		}
	}

	/**
	 * Get the element data as single value
	 * @param postedData posted data
	 * @return single value (may be null)
	 */
	public String getSingleFormData(MultiMap postedData) {
		return postedData.get(getName());
	}

	/**
	 * Get the element data as multivalue
	 * @param postedData posted data
	 * @return value list (may be null)
	 */
	public List<String> getMultiFormData(MultiMap postedData) {
		// multivalue data may be posed as e.g. element42 or element42[]
		for (String alternative : Arrays.asList(getName(), String.format("%s[]", getName()))) {
			if (postedData.contains(alternative)) {
				return postedData.getAll(alternative);
			}
		}
		return null;
	}

	/**
	 * Add poll data from the given form data node to the specfied results
	 * object.
	 *
	 * <p>
	 *     The base implementation will add the type and the label of the form
	 *     element, and make sure a {@code values} field exists which is a map
	 *     from form values to the number of votes for that value. It will also
	 *     initialise the {@total} field to 0 if it is not already present.
	 * </p>
	 *
	 * @param results The {@code JsonObject} to gather results into
	 * @param formData The form data node to get values from
	 */
	public void addPollData(JsonObject results, FieldContainer formData) {
		if (!results.containsKey("type")) {
			results.put("type", getType());
		}

		if (!results.containsKey("label")) {
			results.put("label", getLabel());
		}

		if (!results.containsKey("element")) {
			results.put("element", new JsonObject(toJson()));
		}

		if (!results.containsKey(FormDataService.POLL_VALUES_NAME)) {
			results.put(FormDataService.POLL_VALUES_NAME, new JsonObject(new LinkedHashMap<>()));
		}

		results.put("total", results.getInteger("total", 0) + 1);
	}

	/**
	 * Add the element data stored in the container to the json object
	 * @param object object (will be modified)
	 * @param container field container
	 * @param key key function
	 */
	public void addFormData(JsonObject object, FieldContainer container, Function<FormElement, String> key) {
		if (isList()) {
			StringFieldListImpl field = container.getFields().getStringFieldList(getName());
			if (field != null) {
				object.put(key.apply(this), new JsonArray(field.getItems()));
			}
		} else {
			StringFieldImpl field = container.getFields().getStringField(getName());
			if (field != null) {
				object.put(key.apply(this), field.getString());
			}
		}
	}

	/**
	 * Check whether the element has been completed
	 * @param formDataNode form data node
	 * @return true if the element has been completed.
	 */
	public boolean isCompleted(NodeResponse formDataNode) {
		// generally, all elements are completed
		return true;
	}

	/**
	 * Get flattened stream of all elements (including nested elements)
	 * @return flattened element stream
	 */
	public Stream<FormElement> flattened() {
		return Stream.concat(Stream.of(this), elements.stream().flatMap(FormElement::flattened));
	}

	/**
	 * Get the container element (null for root elements)
	 * @return container element
	 */
	@JsonIgnore
	public FormElement getContainer() {
		return container;
	}

	/**
	 * Set the container element
	 * @param container container element
	 */
	@JsonIgnore
	public void setContainer(FormElement container) {
		this.container = container;
	}

	/**
	 * Get field name part to be added to graphql queries
	 * By default, this will just be the name, but some special fields (like binary) require more
	 * @return graphql part
	 */
	@JsonIgnore
	public String getGraphQLName() {
		return getName();
	}

	/**
	 * Check whether the specific class is annotated with {@link Reportable}
	 * @return true for reportable element implementations
	 */
	@JsonIgnore
	public boolean isReportable() {
		return getClass().isAnnotationPresent(Reportable.class);
	}

	/**
	 * Set the configuration
	 * @param configuration configuration
	 */
	@JsonIgnore
	public void setConfiguration(FormsConfig configuration) {
		this.configuration = configuration;
	}

	/**
	 * Get the configuration
	 * @return configuration
	 */
	@JsonIgnore
	public FormsConfig getConfiguration() {
		return configuration;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	@JsonIgnore
	public SchemaFormGridOptions getFormGridOptions() {
		return formGridOptions;
	}

	public void setFormGridOptions(SchemaFormGridOptions formGridOptions) {
		this.formGridOptions = formGridOptions;
	}

	public Optional<Map<String, Object>> getFormGridOptionsFromUiSchema(FormUiSchema uiSchema) {
		if (uiSchema == null || key == null) {
			return Optional.empty();
		}
		return uiSchema.findFormGridOptions(key);
	}

	/**
	 * Create a clone of this element which has all inactive child elements
	 * removed.
	 *
	 * @return A clone of this element which has all inactive child elements
	 * 		removed
	 */
	@JsonIgnore
	public FormElement removeInactiveElements() {
		FormElement clone = clone(true);

		if (!elements.isEmpty()) {
			clone.elements = clone.elements.stream()
				.filter(FormElement::isActive)
				.map(FormElement::removeInactiveElements)
				.collect(Collectors.toList());
		}

		return clone;
	}

	/**
	 * Performs submit action
	 * @param postedData
	 */
	@JsonIgnore
	public Completable performSubmit(Form form, MultiMap postedData, Vertx rxVertx, OkHttpClient httpClient) {
		return Completable.complete();
	}



	/**
	 * Create a shallow clone of this form element.
	 * @return A shallow clone of this form element.
	 */
	protected FormElement clone() {
		return clone(false);
	}

	/**
	 * Create a clone of this form element.
	 *
	 * <p>
	 *     When {@code deep} is true, all fields that are collections will also
	 *     be cloned, otherwise just the reference to these will be copied.
	 * </p>
	 *
	 * <p>
	 *     <strong>IMPORTANT:</strong> a deep copy of the form element should
	 *     be created when the clones collection fields are modified, for example
	 *     by {@link #removeInactiveElements()}.
	 * </p>
	 *
	 * @param deep Whether to create a deep clone
	 * @return The cloned form element
	 */
	protected abstract FormElement clone(boolean deep);

	/**
	 * Clone the given list field.
	 *
	 * <p>
	 *     When {@code other} is {@code null} the result is an empty list.
	 *     Otherwise the result is a reference to {@code other} when
	 *     {@code deep} is {@code false}, or a new {@link ArrayList} containing
	 *     all the elements of {@code other}.
	 * </p>
	 *
	 * @param other The list to clone
	 * @param deep Whether a copy of the list should be created
	 * @param <T> The type of the list elements
	 * @return Either a reference to {@code other}, or a new list with the
	 * 		same elements
	 */
	@Nonnull
	protected <T> List<T> cloneField(List<T> other, boolean deep) {
		if (other == null) {
			return new ArrayList<>();
		}

		return deep ? new ArrayList<>(other) : other;
	}

	/**
	 * Clone the given map field.
	 *
	 * <p>
	 *     When {@code other} is {@code null} the result is an empty map.
	 *     Otherwise the result is a reference to {@code other} when
	 *     {@code deep} is {@code false}, or a new {@link LinkedHashMap}
	 *     containing all the elements of {@code other}.
	 * </p>
	 *
	 * @param other The map to clone
	 * @param deep Whether a copy of the map should be created
	 * @param <K> The type of the map keys
	 * @param <V> The type of the map values
	 * @return Either a reference to {@code other}, or a new map with the
	 * 		same elements
	 */
	@Nonnull
	protected <K, V> Map<K, V> cloneField(Map<K, V> other, boolean deep) {
		if (other == null) {
			return new LinkedHashMap<>();
		}

		return deep ? new LinkedHashMap<>(other) : other;
	}

	/**
	 * Check whether the given value is valid for this element.
	 *
	 * <p>
	 *     Unlike the checks in {@link #validate(MultiMap, Set, NodeResponse, Vertx, OkHttpClient)}, this
	 *     checks implicit restrictions for fields, like for example that only available options for a value
	 *     list element can be submitted.
	 * </p>
	 *
	 * <p>
	 *     By default, all values are valid for form elements. If a form element has certain restrictions
	 *     that are not covered by user defined validation, its implementation must override this method
	 *     to check the restrictions.
	 * </p>
	 * @param value The value to check.
	 * @return Whether {@code value} is valid for this element.
	 */
	protected boolean isInvalidValue(String value) {
		return false;
	}
}
