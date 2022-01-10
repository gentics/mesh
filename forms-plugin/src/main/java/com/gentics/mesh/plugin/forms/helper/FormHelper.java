package com.gentics.mesh.plugin.forms.helper;

import com.gentics.mesh.plugin.forms.util.ParameterUtil;
import com.github.jknack.handlebars.Options;
import io.vertx.core.json.JsonObject;

import java.io.IOException;

/**
 * Helper for rendering a form (optionally prefilled with input and error messages)
 */
public class FormHelper {
	public final static String SUBTYPE_PARAMETER_NAME = "subtype";
	public final static String DEFAULT_SUBTYPE_PARAMETER_NAME = "defaultSubtype";

	/**
	 * Create an instance
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
		return elementTemplate(elementType, "elements", options);
	}

	/**
	 * Render the name of the poll results template for the element with type given in the context
	 *
	 * @param elementType element type
	 * @param options options
	 * @return template name
	 */
	public CharSequence pollResultTemplate(Object elementType, Options options) {
		return elementTemplate(elementType, "poll-results", options);
	}

	/**
	 * Render the name of the template for the element with type given in the context
	 *
	 * @param elementType element type
	 * @param baseDir directory name
	 * @param options options
	 * @return template name
	 */
	public CharSequence elementTemplate(Object elementType, String baseDir, Options options) {
		Object elementSubType = options.hash.get(SUBTYPE_PARAMETER_NAME);
		// to prevent and endless loop if the helper is called with "null" in the subtype parameter
		// require a default subtype parameter to fallback to
		// example:
		//   (formsElementTemplate selectgroup subtype=null defaultSubtype="singleselect")
		//   ... this will get the template selectgroup_singleselect
		if (elementSubType == null) {
			elementSubType = options.hash.get(DEFAULT_SUBTYPE_PARAMETER_NAME);
		}
		if (elementSubType != null) {
			return String.format("%s/%s_%s", baseDir, elementType, elementSubType);
		}
		return String.format("%s/%s", baseDir, elementType);
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

}
