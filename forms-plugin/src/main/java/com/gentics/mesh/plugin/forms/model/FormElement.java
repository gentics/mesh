package com.gentics.mesh.plugin.forms.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.i18n.I18nHelper;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;

/**
 * Abstract form element
 */
public abstract class FormElement {
	protected static final Logger log = LoggerFactory.getLogger(FormElement.class);

	private String name;

	private String label;

	private boolean active = true;

	private boolean mandatory;

	private String validation;

	private List<FormElement> elements = new ArrayList<>();

	@JsonIgnore
	private FormElement container;

	@JsonIgnore
	private FormsConfig configuration;

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
	 * Multivalue flag
	 * @return true for multivalue
	 */
	public boolean isMultivalue() {
		return false;
	}

	/**
	 * Transform the element into a field schema and add it to the container
	 * @param container container (will be modified)
	 */
	public void addFieldSchema(FieldSchemaContainer container) {
		if (isMultivalue()) {
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
	 * @return validation error or null if posted data is valid
	 */
	public ValidationError validate(MultiMap postedData, Set<FileUpload> fileUploads, NodeResponse formDataNode) {
		List<String> values = new ArrayList<>();
		if (isMultivalue()) {
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
				return new ValidationError().setName(getName()).setTitle(I18nHelper.get(Locale.GERMAN, "error.mandatory_field_not_filled.title"))
						.setMessage(I18nHelper.get(Locale.GERMAN, "error.mandatory_field_not_filled", getLabel()));
			}
		}

		Validator validator = Validator.get(getValidation());
		if (validator != null) {
			return validator.validate(getName(), values);
		}

		return null;
	}

	/**
	 * Transform the posted data into a field in the container
	 * @param container container (will be modified)
	 * @param postedData posted data
	 * @param errors true if validation errors were detected
	 */
	public void addField(FieldContainer container, MultiMap postedData, boolean errors) {
		if (isMultivalue()) {
			List<String> values = getMultiFormData(postedData);
			if (values != null) {
				container.getFields().put(getName(), new StringFieldListImpl().setItems(values));
			}
		} else {
			String value = getSingleFormData(postedData);
			if (value != null) {
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
	 * Add the element data stored in the container to the json object
	 * @param object object (will be modified)
	 * @param container field container
	 * @param key key function
	 */
	public void addFormData(JsonObject object, FieldContainer container, Function<FormElement, String> key) {
		if (isMultivalue()) {
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
}
