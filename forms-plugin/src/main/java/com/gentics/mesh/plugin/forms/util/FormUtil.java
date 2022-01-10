package com.gentics.mesh.plugin.forms.util;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.forms.service.SchemaService;

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
}
