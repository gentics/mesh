package com.gentics.mesh.core.data.node.field.nesting;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.basic.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.basic.DateGraphField;
import com.gentics.mesh.core.data.node.field.basic.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.basic.NumberGraphField;
import com.gentics.mesh.core.data.node.field.basic.StringGraphField;

public interface GraphNestingField extends GraphField {

	StringGraphField createString(String string);

	StringGraphField getString(String key);

	GraphNodeField createNode(String key, Node node);

	DateGraphField createDate(String key);

	NumberGraphField createNumber(String key);

	HtmlGraphField createHTML(String key);

	BooleanGraphField getBoolean(String key);

	BooleanGraphField createBoolean(String key);
}
