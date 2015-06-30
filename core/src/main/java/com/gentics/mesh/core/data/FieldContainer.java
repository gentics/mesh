package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.node.MeshNode;
import com.gentics.mesh.core.data.node.field.basic.BooleanField;
import com.gentics.mesh.core.data.node.field.basic.DateField;
import com.gentics.mesh.core.data.node.field.basic.HTMLField;
import com.gentics.mesh.core.data.node.field.basic.NumberField;
import com.gentics.mesh.core.data.node.field.basic.StringField;
import com.gentics.mesh.core.data.node.field.nesting.ListField;
import com.gentics.mesh.core.data.node.field.nesting.ListableField;
import com.gentics.mesh.core.data.node.field.nesting.NodeField;
import com.gentics.mesh.core.data.node.field.nesting.SelectField;

public interface FieldContainer extends BasicFieldContainer {

	StringField getString(String key);

	StringField createString(String key);

	NodeField createNode(String key, MeshNode node);

	DateField getDate(String key);

	DateField createDate(String key);

	NumberField createNumber(String key);

	NumberField getNumber(String key);

	HTMLField createHTML(String key);

	HTMLField getHTML(String key);

	BooleanField getBoolean(String key);

	BooleanField createBoolean(String key);

	<T extends ListableField> ListField<T> createList(String key);

	<T extends ListableField> ListField<T> getList(String key);

	<T extends ListableField> SelectField<T> createSelect(String key);

	<T extends ListableField> SelectField<T> getSelect(String key);

}
