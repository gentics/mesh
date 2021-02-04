package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.HibHtmlField;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;

public interface HtmlGraphFieldList extends HibHtmlFieldList, ListGraphField<HibHtmlField, HtmlFieldListImpl, String> {

	String TYPE = "html";

}
