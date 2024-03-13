package com.gentics.mesh.plugin.forms.util;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.forms.service.SchemaService;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Miscellaneous helpers for handling forms and form data.
 */
public class FormUtil {

	/**
	 * Check whether the node is a form data node (schema name starts with "form_")
	 *
	 * @param node
	 *            node to check
	 * @return true for form data nodes
	 */
	public static boolean isFormDataNode(NodeResponse node) {
		return node != null && StringUtils.startsWith(node.getSchema().getName(), String.format("%s_", SchemaService.SCHEMA_NAME));
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
	 * Get data from the graphql response by json path. Excpetions during fetch are suppressed
	 * @param <T> type of the returned data
	 * @param response graphql response
	 * @param jsonPath json path
	 * @return fetched data
	 */
	public static <T> T getFromResponse(GraphQLResponse response, String jsonPath) {
		DocumentContext ctx = JsonPath.parse(response.toJson(),
				Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS));
		return ctx.read(jsonPath);
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
