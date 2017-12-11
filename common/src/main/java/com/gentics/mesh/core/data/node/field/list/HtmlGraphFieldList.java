package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;

public interface HtmlGraphFieldList extends BasicListGraphField<HtmlGraphField, HtmlFieldListImpl, String> {

	String TYPE = "html";

}
