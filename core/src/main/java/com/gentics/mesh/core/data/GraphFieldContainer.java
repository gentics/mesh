package com.gentics.mesh.core.data;

import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.node.field.nesting.SelectGraphField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.handler.InternalActionContext;

import rx.Observable;

/**
 * A graph field container (eg. a container for fields of a node) is used to hold i18n specific graph fields.
 */
public interface GraphFieldContainer extends BasicFieldContainer {

	/**
	 * 
	 * Return the string graph field for the given key.
	 * 
	 * @param key
	 * @return
	 */
	StringGraphField getString(String key);

	/**
	 * Create a new string graph field.
	 * 
	 * @param key
	 * @return
	 */
	StringGraphField createString(String key);

	/**
	 * Return the binary graph field for the given key.
	 * 
	 * @param key
	 * @return
	 */
	BinaryGraphField getBinary(String key);

	/**
	 * Create the binary graph field with the given key.
	 * 
	 * @param key
	 * @return
	 */
	BinaryGraphField createBinary(String key);

	/**
	 * Return the node graph field for the given key.
	 * 
	 * @param key
	 * @return
	 */
	NodeGraphField getNode(String key);

	/**
	 * Create a new node graph field.
	 * 
	 * @param key
	 *            Key of the field
	 * @param node
	 *            Node to be referenced.
	 * @return
	 */
	NodeGraphField createNode(String key, Node node);

	/**
	 * Return the date graph field.
	 * 
	 * @param key
	 * @return
	 */
	DateGraphField getDate(String key);

	/**
	 * Create a new date graph field.
	 * 
	 * @param key
	 * @return
	 */
	DateGraphField createDate(String key);

	/**
	 * Return the number graph field.
	 * 
	 * @param key
	 * @return
	 */
	NumberGraphField getNumber(String key);

	/**
	 * Create the number graph field.
	 * 
	 * @param key
	 * @return
	 */
	NumberGraphField createNumber(String key);

	/**
	 * Return the html graph field.
	 * 
	 * @param key
	 * @return
	 */
	HtmlGraphField getHtml(String key);

	/**
	 * Create a new html graph field.
	 * 
	 * @param key
	 * @return
	 */
	HtmlGraphField createHTML(String key);

	/**
	 * Return the boolean graph field.
	 * 
	 * @param key
	 * @return
	 */
	BooleanGraphField getBoolean(String key);

	/**
	 * Create a new boolean graph field.
	 * 
	 * @param key
	 * @return
	 */
	BooleanGraphField createBoolean(String key);

	/**
	 * Return the micronode graph field.
	 * 
	 * @param key
	 * @return
	 */
	MicronodeGraphField getMicronode(String key);

	/**
	 * Create a new micronode graph field.
	 * 
	 * @param key
	 * @param microschema
	 * @return
	 */
	MicronodeGraphField createMicronode(String key, MicroschemaContainer microschema);

	// Lists

	/**
	 * Return the graph date list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	DateGraphFieldList getDateList(String fieldKey);

	/**
	 * Create a new graph date list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	DateGraphFieldList createDateList(String fieldKey);

	/**
	 * Return graph html list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	HtmlGraphFieldList getHTMLList(String fieldKey);

	/**
	 * Create a new graph html list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	HtmlGraphFieldList createHTMLList(String fieldKey);

	/**
	 * Return graph number list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	NumberGraphFieldList getNumberList(String fieldKey);

	/**
	 * Create a new graph number list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	NumberGraphFieldList createNumberList(String fieldKey);

	/**
	 * Return graph node list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	NodeGraphFieldList getNodeList(String fieldKey);

	/**
	 * Create a new graph node list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	NodeGraphFieldList createNodeList(String fieldKey);

	/**
	 * Return graph string list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	StringGraphFieldList getStringList(String fieldKey);

	/**
	 * Create a new graph string list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	StringGraphFieldList createStringList(String fieldKey);

	/**
	 * Return graph boolean list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	BooleanGraphFieldList getBooleanList(String fieldKey);

	/**
	 * Create a new graph boolean list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	BooleanGraphFieldList createBooleanList(String fieldKey);

	/**
	 * Return graph node list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	MicronodeGraphFieldList getMicronodeList(String fieldKey);

	/**
	 * Create a new graph micronode list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	MicronodeGraphFieldList createMicronodeFieldList(String fieldKey);

	/**
	 * Locate the field with the given fieldkey in this container and return the rest model for this field.
	 * 
	 * @param ac
	 * @param fieldKey
	 * @param fieldSchema
	 * @param languageTags
	 *            language tags
	 */
	Observable<? extends Field> getRestFieldFromGraph(InternalActionContext ac, String fieldKey, FieldSchema fieldSchema, List<String> languageTags);

	/**
	 * Use the given map of rest fields and the schema information to set the data from the map to this container. TODO: This should return an observable
	 * 
	 * @param ac
	 * @param fields
	 * @throws MeshSchemaException
	 */
	void updateFieldsFromRest(InternalActionContext ac, Map<String, Field> restFields, Schema schema);



}
