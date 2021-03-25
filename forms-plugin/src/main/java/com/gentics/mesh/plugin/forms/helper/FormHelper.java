package com.gentics.mesh.plugin.forms.helper;

import java.io.IOException;
import java.util.Set;

import com.gentics.mesh.plugin.forms.util.ParameterUtil;
import com.github.jknack.handlebars.Options;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Helper for rendering a form (optionally prefilled with input and error messages)
 */
public class FormHelper {
	public final static String SUBTYPE_PARAMETER_NAME = "subtype";

	/**
	 * Create an instance
	 * 
	 * @param handler
	 *            handler
	 */
	public FormHelper() {
	}

	public CharSequence equals(Object o1, Object o2, Options options) throws IOException {
		if (o1.equals(o2)) {
			return options.fn();
		}
		return options.inverse();
	}

	/**
	 * Render the name of the template for the element with type given in the context
	 * 
	 * @param elementType element type
	 * @param options options
	 * @return template name
	 */
	public CharSequence formsElementTemplate(Object elementType, Options options) {
		Object elementSubType = options.hash.get(SUBTYPE_PARAMETER_NAME);
		if (elementSubType != null) {
			return String.format("elements/%s_%s", elementType, elementSubType);	
		} else {
			return String.format("elements/%s", elementType);
		}
	}

	/**
	 * Determine the type of an input element
	 * 
	 * @param context
	 *            context (which should be the input element)
	 * @param options
	 *            options
	 * @return type
	 */
	public CharSequence formsInputType(Object context, Options options) {
		if (context instanceof String) {
			switch ((String) context) {
			case "number":
			case "socialsecurity":
				return "number";
			case "telephone":
				return "tel";
			case "date":
			case "time":
			case "email":
				return (String) context;
			default:
				return "text";
			}
		} else if (context instanceof JsonObject) {
			JsonObject element = (JsonObject) context;
			int minVal = ParameterUtil.asInt(element.getValue("minVal"), -1);
			int maxVal = ParameterUtil.asInt(element.getValue("maxVal"), -1);

			if (minVal > 0 || maxVal > 0) {
				return "number";
			} else {
				return formsInputType(element.getString("validation", ""), options);
			}
		} else {
			return "text";
		}
	}

	/**
	 * Merge the form data into the form
	 * 
	 * @param form
	 *            form
	 * @param formData
	 *            form data
	 */
	protected void mergeData(JsonObject form, JsonObject formData) {
		for (String elementName : formData.fieldNames()) {
			JsonObject element = findElement(form, elementName);
			if (element != null) {
				element.put("data", formData.getJsonObject(elementName));
			}
		}
	}

	/**
	 * Find the json object representing the element with given name
	 * 
	 * @param form
	 *            form to search
	 * @param name
	 *            element name
	 * @return json object or null if not found
	 */
	protected JsonObject findElement(JsonObject form, String name) {
		JsonArray elements = form.getJsonArray("elements");
		if (elements != null) {
			for (int i = 0; i < elements.size(); i++) {
				JsonObject element = elements.getJsonObject(i);
				if (name.equals(element.getString("name"))) {
					return element;
				} else {
					JsonObject subElement = findElement(element, name);
					if (subElement != null) {
						return subElement;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Mark completed and active formpages
	 * 
	 * @param form
	 *            form
	 * @param completed
	 *            names of completed formpages
	 */
	protected void markFormpages(JsonObject form, Set<String> completed) {
		JsonArray elements = form.getJsonArray("elements");
		for (int i = 0; i < elements.size(); i++) {
			JsonObject element = elements.getJsonObject(i);
			if (!"formpage".equals(element.getString("type"))) {
				continue;
			}
			String name = element.getString("name");
			if (completed.contains(name)) {
				element.put("class", "is-completed");
			} else {
				element.put("class", "is-active");
				element.put("active", true);
				break;
			}
		}
	}
}
