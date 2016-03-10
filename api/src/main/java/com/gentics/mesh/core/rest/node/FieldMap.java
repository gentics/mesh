package com.gentics.mesh.core.rest.node;

import java.io.IOException;
import java.util.Collection;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;

/**
 * A field map stores all fields of a node or micronode.
 */
public interface FieldMap {

	/**
	 * Add or update the field with the given key in the map.
	 * 
	 * @param fieldKey
	 * @param field
	 * @return
	 */
	Field put(String fieldKey, Field field);

	/**
	 * Return the field with the given key.
	 * 
	 * @param key
	 *            Field key
	 * @param classOfT
	 * @return
	 */
	<T extends Field> T get(String key, Class<T> classOfT);

	/**
	 * Return the size of the field map.
	 * 
	 * @return
	 */
	int size();

	Collection<String> keySet();

	boolean containsKey(String key);

	StringFieldImpl getStringField(String key);

	NumberFieldImpl getNumberField(String key);

	NumberFieldListImpl getNumberFieldList(String key);

	HtmlFieldImpl getHtmlField(String key);

	HtmlFieldListImpl getHtmlFieldList(String key);

	BinaryField getBinaryField(String key);

	BooleanFieldImpl getBooleanField(String key);

	BooleanFieldListImpl getBooleanListField(String key);

	DateFieldListImpl getDateFieldList(String key);

	DateFieldImpl getDateField(String key);

	NodeField getNodeField(String key);

	NodeResponse getNodeFieldExpanded(String key);

	NodeFieldListImpl getNodeListField(String key);

	MicronodeResponse getMicronodeField(String key);

	StringFieldListImpl getStringFieldList(String key);

	FieldList<MicronodeField> getMicronodeFieldList(String key);

	NodeFieldList getNodeFieldList(String key);

	/**
	 * Check whether the field map is empty.
	 * 
	 * @return
	 */
	boolean isEmpty();

	/**
	 * Check whether a field which uses the given key is stored in the map.
	 * 
	 * @param key
	 * @return
	 */
	boolean hasField(String key);

	/**
	 * Return the field with the given key.
	 * 
	 * @param key
	 * @param fieldSchema
	 * @return
	 */
	Field getField(String key, FieldSchema fieldSchema);

	/**
	 * Return the deserialize field.
	 * 
	 * @param key
	 *            Key of the current field
	 * @param type
	 *            Expected field
	 * @param listType
	 *            Optional expected list type of the field
	 * @param expand
	 *            The field will be expanded (if possible) when set to true
	 */
	<T extends Field> T getField(String key, FieldTypes type, String listType, boolean expand);

}
