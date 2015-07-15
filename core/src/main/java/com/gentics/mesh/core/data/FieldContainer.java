package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.basic.BooleanField;
import com.gentics.mesh.core.data.node.field.basic.DateField;
import com.gentics.mesh.core.data.node.field.basic.HTMLField;
import com.gentics.mesh.core.data.node.field.basic.NumberField;
import com.gentics.mesh.core.data.node.field.basic.StringField;
import com.gentics.mesh.core.data.node.field.list.BooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.DateFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.MicroschemaFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberFieldList;
import com.gentics.mesh.core.data.node.field.list.StringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.ListableField;
import com.gentics.mesh.core.data.node.field.nesting.NodeField;
import com.gentics.mesh.core.data.node.field.nesting.SelectField;

public interface FieldContainer extends BasicFieldContainer {

	StringField getString(String key);

	StringField createString(String key);

	NodeField createNode(String key, Node node);

	DateField getDate(String key);

	DateField createDate(String key);

	NumberField createNumber(String key);

	NumberField getNumber(String key);

	HTMLField createHTML(String key);

	HTMLField getHTML(String key);

	BooleanField getBoolean(String key);

	BooleanField createBoolean(String key);

	DateFieldList createDateList(String fieldKey);

	HtmlFieldList createHTMLList(String fieldKey);

	NumberFieldList createNumberList(String fieldKey);

	NodeFieldList createNodeList(String fieldKey);

	StringFieldList createStringList(String fieldKey);

	BooleanFieldList createBooleanList(String fieldKey);

	MicroschemaFieldList createMicroschemaFieldList(String fieldKey);

	<T extends ListableField> SelectField<T> createSelect(String key);

	<T extends ListableField> SelectField<T> getSelect(String key);

}
