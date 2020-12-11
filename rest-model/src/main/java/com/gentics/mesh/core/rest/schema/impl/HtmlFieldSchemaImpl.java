package com.gentics.mesh.core.rest.schema.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;

/**
 * @see HtmlFieldSchema
 */
public class HtmlFieldSchemaImpl extends AbstractFieldSchema implements HtmlFieldSchema {

	@Override
	public String getType() {
		return FieldTypes.HTML.toString();
	}
}
