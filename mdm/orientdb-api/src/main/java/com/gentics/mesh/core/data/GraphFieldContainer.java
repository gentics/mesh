package com.gentics.mesh.core.data;

import java.util.List;
import java.util.stream.Stream;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.ListGraphField;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;

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
	 * Create a binary field and use the given binary to be referenced by the field.
	 * 
	 * @param fieldKey
	 * @param binary
	 * @return
	 */
	BinaryGraphField createBinary(String fieldKey, Binary binary);

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
	 * Create a new micronode graph field. This method ensures that only one micronode exists per key.
	 * 
	 * @param key
	 * @param microschemaVersion
	 * @return
	 */
	MicronodeGraphField createMicronode(String key, MicroschemaVersion microschemaVersion);

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
	// TODO remove field from method name
	MicronodeGraphFieldList createMicronodeFieldList(String fieldKey);

	/**
	 * Locate the field with the given fieldkey in this container and return the rest model for this field.
	 * 
	 * @param ac
	 * @param fieldKey
	 * @param fieldSchema
	 * @param languageTags
	 *            language tags
	 * @param level
	 *            Current level of transformation
	 */
	Field getRestFieldFromGraph(InternalActionContext ac, String fieldKey, FieldSchema fieldSchema, List<String> languageTags, int level);

	/**
	 * Use the given map of rest fields to set the data from the map to this container.
	 * 
	 * @param ac
	 * @param restFields
	 */
	void updateFieldsFromRest(InternalActionContext ac, FieldMap restFields);

	/**
	 * Return the graph field for the given field schema.
	 * 
	 * @param fieldSchema
	 * @return
	 */
	GraphField getField(FieldSchema fieldSchema);

	/**
	 * Get all fields, that are present in this container
	 *
	 * @return
	 */
	List<GraphField> getFields();

	/**
	 * Get the list graph field of specified type
	 * 
	 * @param classOfT
	 * @param fieldKey
	 * @return
	 */
	<T extends ListGraphField<?, ?, ?>> T getList(Class<T> classOfT, String fieldKey);

	/**
	 * Validate consistency of this container. This will check whether all mandatory fields have been filled
	 */
	void validate();

	/**
	 * Delete the field edge with the given key from the container.
	 * 
	 * @param key
	 */
	void deleteFieldEdge(String key);

	/**
	 * Get the schema container version used by this container
	 * 
	 * @return schema container version
	 */
	GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> getSchemaContainerVersion();

	/**
	 * Set the schema container version used by this container
	 * 
	 * @param version
	 *            schema container version
	 */
	void setSchemaContainerVersion(GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> version);

	/**
	 * Get all nodes that are in any way referenced by this node. This includes the following cases:
	 * <ul>
	 *     <li>Node fields</li>
	 *     <li>Node list fields</li>
	 *     <li>Micronode fields with node fields or node list fields</li>
	 *     <li>Micronode list fields with node fields or node list fields</li>
	 * </ul>
	 */
    Iterable<? extends Node> getReferencedNodes();

	/**
	 * Gets the NodeGraphFieldContainers connected to this FieldContainer.
	 * For NodeGraphFieldContainers this is simply the same object.
	 * For Micronodes this is will return all contents that use this micronode.
	 * @return
	 */
	Stream<? extends NodeGraphFieldContainer> getContents();
}
