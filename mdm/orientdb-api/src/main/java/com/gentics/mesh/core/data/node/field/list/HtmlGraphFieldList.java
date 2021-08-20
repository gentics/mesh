package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.HibHtmlField;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;

/**
 * Domain model definition for a html list.
 */
public interface HtmlGraphFieldList extends ListGraphField<HibHtmlField, HtmlFieldListImpl, String>, HibHtmlFieldList {

	String TYPE = "html";

}
