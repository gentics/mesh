package com.gentics.mesh.core.data.model;

import com.gentics.mesh.core.data.model.node.MeshNode;
import com.gentics.mesh.core.data.model.node.field.BooleanField;
import com.gentics.mesh.core.data.model.node.field.DateField;
import com.gentics.mesh.core.data.model.node.field.HTMLField;
import com.gentics.mesh.core.data.model.node.field.MicroschemaField;
import com.gentics.mesh.core.data.model.node.field.NodeField;
import com.gentics.mesh.core.data.model.node.field.NumberField;
import com.gentics.mesh.core.data.model.node.field.StringField;

public interface MeshNodeFieldContainer extends FieldContainer {

	StringField getString(String key);
	StringField createString(String key);

	NodeField createNode(String key, MeshNode node);

	DateField createDate(String key);

	NumberField createNumber(String key);

	HTMLField createHTML(String key);

	BooleanField getBoolean(String key);
	BooleanField createBoolean(String key);

	MicroschemaField createMicroschema(String key);


}
