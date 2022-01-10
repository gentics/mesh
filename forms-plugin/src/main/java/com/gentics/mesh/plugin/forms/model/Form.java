package com.gentics.mesh.plugin.forms.model;

import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Form
 */
public class Form {
	/**
	 * Name of the schema field containing the errors
	 */
	public final static String ERRORS_FIELD_NAME = "errors";

	public final static String ERROR_FIELD_NAME = "error";

	private String uuid;

	private String name;

	private FormType type;

	private String email;

	private String mailsubject;

	private String mailtemp;

	private String successurl;

	private List<FormElement> elements = new ArrayList<>();

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
	 * Get email template
	 * @return mail template
	 */
	public String getMailtemp() {
		return mailtemp;
	}

	/**
	 * Set email template
	 * @param mailtemp mail template
	 */
	public void setMailtemp(String mailtemp) {
		this.mailtemp = mailtemp;
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
		return !flattened().filter(elem -> !elem.isCompleted(formDataNode)).findAny().isPresent();
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
				elem.addFormData(elemJson, container, e -> "value");
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
	 * @return
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
		JsonArray array = new JsonArray();
		flattened().filter(FormElement::isReportable).forEach(elem -> {
			JsonObject elemObj = new JsonObject();
			elemObj.put("name", elem.getName());
			elemObj.put("label", elem.getLabel());
			elemObj.put("isFile", elem instanceof FileElement);
			elem.addFormData(elemObj, container, e -> "value");
			array.add(elemObj);
		});
		return array;
	}

	/**
	 * Get the names of the form elements.
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
		return flattened().filter(elem -> elem instanceof FormpageElement).findFirst().isPresent();
	}
}
