package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.basic.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.basic.DateGraphField;
import com.gentics.mesh.core.data.node.field.basic.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.basic.NumberGraphField;
import com.gentics.mesh.core.data.node.field.basic.StringGraphField;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.MicroschemaGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.node.field.nesting.SelectGraphField;

/**
 * A graph field container (eg. a container for fields of a node) is used to hold i18n specific graph fields.
 */
public interface GraphFieldContainer extends BasicFieldContainer {

	StringGraphField getString(String key);

	StringGraphField createString(String key);

	NodeGraphField getNode(String key);

	NodeGraphField createNode(String key, Node node);

	DateGraphField getDate(String key);

	DateGraphField createDate(String key);

	NumberGraphField createNumber(String key);

	NumberGraphField getNumber(String key);

	HtmlGraphField createHTML(String key);

	HtmlGraphField getHtml(String key);

	BooleanGraphField getBoolean(String key);

	BooleanGraphField createBoolean(String key);

	DateGraphFieldList createDateList(String fieldKey);

	DateGraphFieldList getDateList(String fieldKey);

	HtmlGraphFieldList createHTMLList(String fieldKey);

	HtmlGraphFieldList getHTMLList(String fieldKey);

	NumberGraphFieldList createNumberList(String fieldKey);

	NumberGraphFieldList getNumberList(String fieldKey);

	NodeGraphFieldList createNodeList(String fieldKey);

	NodeGraphFieldList getNodeList(String fieldKey);

	StringGraphFieldList createStringList(String fieldKey);

	StringGraphFieldList getStringList(String fieldKey);

	BooleanGraphFieldList createBooleanList(String fieldKey);

	BooleanGraphFieldList getBooleanList(String fieldKey);

	MicroschemaGraphFieldList createMicroschemaFieldList(String fieldKey);

	MicroschemaGraphFieldList getMicroschemaList(String fieldKey);

	<T extends ListableGraphField> SelectGraphField<T> createSelect(String key);

	<T extends ListableGraphField> SelectGraphField<T> getSelect(String key);

}
