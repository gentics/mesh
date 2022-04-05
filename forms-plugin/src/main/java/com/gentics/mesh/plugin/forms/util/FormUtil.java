package com.gentics.mesh.plugin.forms.util;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.service.SchemaService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Miscellaneous helpers for handling forms and form data.
 */
public class FormUtil {

	/**
	 * Check whether the node is a form data node (parent node uses the form schema)
	 *
	 * @param node
	 *            node to check
	 * @return true for form data nodes
	 */
	public static boolean isFormDataNode(NodeResponse node) {
		return node != null && SchemaService.SCHEMA_NAME.equals(node.getParentNode().getSchema().getName());
	}

	/**
	 * Remove inactive elements from the form
	 * @param form form, which will be modified
	 */
	public static JsonObject removeInactiveElements(JsonObject form) {
		JsonArray elements = form.getJsonArray("elements");
		if (elements != null) {
			form.put("elements", removeInactiveElements(elements));
		}

		return form;
	}

	/**
	 * Remove inactive elements from the elements array
	 * @param elements elements
	 * @return cleaned elements array
	 */
	private static JsonArray removeInactiveElements(JsonArray elements) {
		JsonArray cleanedElements = new JsonArray();
		for (int i = 0; i < elements.size(); i++) {
			JsonObject element = elements.getJsonObject(i);
			if (ParameterUtil.asBoolean(element.getValue("active"), true)) {
				cleanedElements.add(element);
				JsonArray subElements = element.getJsonArray("elements");
				if (subElements != null) {
					element.put("elements", removeInactiveElements(subElements));
				}
			}
		}
		return cleanedElements;
	}
}
