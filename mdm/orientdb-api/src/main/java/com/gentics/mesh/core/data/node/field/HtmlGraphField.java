package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.HtmlField;

/**
 * The HtmlField Domain Model interface.
 * 
 * A HTML graph field is a basic node field which can be used to store a single HTML string value.
 */
public interface HtmlGraphField extends ListableGraphField, BasicGraphField<HtmlField>, HibHtmlField {

}
