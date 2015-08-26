package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.basic.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.basic.DateGraphField;
import com.gentics.mesh.core.data.node.field.basic.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.basic.NumberGraphField;
import com.gentics.mesh.core.data.node.field.basic.StringGraphField;
import com.gentics.mesh.core.data.node.field.list.GraphBooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphDateFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphHtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphMicroschemaFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphNodeFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphNumberFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphStringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.data.node.field.nesting.GraphNodeField;
import com.gentics.mesh.core.data.node.field.nesting.GraphSelectField;

public interface GraphFieldContainer extends BasicFieldContainer {

	StringGraphField getString(String key);

	StringGraphField createString(String key);

	GraphNodeField getNode(String key);

	GraphNodeField createNode(String key, Node node);

	DateGraphField getDate(String key);

	DateGraphField createDate(String key);

	NumberGraphField createNumber(String key);

	NumberGraphField getNumber(String key);

	HtmlGraphField createHTML(String key);

	HtmlGraphField getHtml(String key);

	BooleanGraphField getBoolean(String key);

	BooleanGraphField createBoolean(String key);

	GraphDateFieldList createDateList(String fieldKey);

	GraphDateFieldList getDateList(String fieldKey);

	GraphHtmlFieldList createHTMLList(String fieldKey);

	GraphHtmlFieldList getHTMLList(String fieldKey);

	GraphNumberFieldList createNumberList(String fieldKey);

	GraphNumberFieldList getNumberList(String fieldKey);

	GraphNodeFieldList createNodeList(String fieldKey);

	GraphNodeFieldList getNodeList(String fieldKey);

	GraphStringFieldList createStringList(String fieldKey);

	GraphStringFieldList getStringList(String fieldKey);

	GraphBooleanFieldList createBooleanList(String fieldKey);

	GraphBooleanFieldList getBooleanList(String fieldKey);

	GraphMicroschemaFieldList createMicroschemaFieldList(String fieldKey);

	GraphMicroschemaFieldList getMicroschemaList(String fieldKey);

	<T extends ListableGraphField> GraphSelectField<T> createSelect(String key);

	<T extends ListableGraphField> GraphSelectField<T> getSelect(String key);

}
