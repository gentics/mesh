package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.basic.HTMLField;

public interface HtmlFieldList extends ListField<HTMLField> {

	HTMLField createHTML(String key);

}
