package com.gentics.mesh.core.data.node.field.nesting;

import com.gentics.mesh.core.data.node.MeshNode;
import com.gentics.mesh.core.data.node.field.Field;
import com.gentics.mesh.core.data.node.field.basic.BooleanField;
import com.gentics.mesh.core.data.node.field.basic.DateField;
import com.gentics.mesh.core.data.node.field.basic.HTMLField;
import com.gentics.mesh.core.data.node.field.basic.NumberField;
import com.gentics.mesh.core.data.node.field.basic.StringField;

public interface NestingField extends Field {

	StringField createString(String string);

	StringField getString(String key);

	NodeField createNode(String key, MeshNode node);

	DateField createDate(String key);

	NumberField createNumber(String key);

	HTMLField createHTML(String key);

	BooleanField getBoolean(String key);

	BooleanField createBoolean(String key);
}
