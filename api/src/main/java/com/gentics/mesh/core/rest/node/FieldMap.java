package com.gentics.mesh.core.rest.node;

import java.util.Collection;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
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
	 * Return the size of the field map.
	 * 
	 * @return
	 */
	int size();

	/**
	 * Return a collection containing all field keys.
	 * 
	 * @return
	 */
	Collection<String> keySet();

	/**
	 * Return the string field with the given key.
	 * 
	 * @param fieldKey
	 * @return
	 */
	StringFieldImpl getStringField(String fieldKey);

	/**
	 * Return the number field with the given key.
	 * 
	 * @param fieldKey
	 * @return
	 */
	NumberFieldImpl getNumberField(String fieldKey);

	/**
	 * Return the number list field with the given key.
	 * 
	 * @param fieldKey
	 * @return
	 */
	NumberFieldListImpl getNumberFieldList(String fieldKey);

	/**
	 * Return the html field with the given key
	 * 
	 * @param fieldKey
	 * @return
	 */
	HtmlFieldImpl getHtmlField(String fieldKey);

	/**
	 * Return the html list field with the given key.
	 * 
	 * @param fieldKey
	 * @return
	 */
	HtmlFieldListImpl getHtmlFieldList(String fieldKey);

	/**
	 * Return the binary field with the given key.
	 * 
	 * @param fieldKey
	 * @return
	 */
	BinaryField getBinaryField(String fieldKey);

	/**
	 * Return the boolean field with the given key.
	 * 
	 * @param fieldKey
	 * @return
	 */
	BooleanFieldImpl getBooleanField(String fieldKey);

	/**
	 * Return the boolean list field with the given key.
	 * 
	 * @param fieldKey
	 * @return
	 */
	BooleanFieldListImpl getBooleanFieldList(String fieldKey);

	/**
	 * Return the date list field with the given key.
	 * 
	 * @param fieldKey
	 * @return
	 */
	DateFieldListImpl getDateFieldList(String fieldKey);

	/**
	 * Return the date field with the given key.
	 * 
	 * @param fieldKey
	 * @return
	 */
	DateFieldImpl getDateField(String fieldKey);

	/**
	 * Return the node field with the given key.
	 * 
	 * @param fieldKey
	 * @return
	 */
	NodeField getNodeField(String fieldKey);

	/**
	 * Return the node field with the given key in expanded form.
	 * 
	 * @param fieldKey
	 * @return
	 */
	NodeResponse getNodeFieldExpanded(String fieldKey);

	/**
	 * Return the micronode field with the given key.
	 * 
	 * @param fieldKey
	 * @return
	 */
	MicronodeResponse getMicronodeField(String fieldKey);

	/**
	 * Return the string list field with the given key.
	 * 
	 * @param fieldKey
	 * @return
	 */
	StringFieldListImpl getStringFieldList(String fieldKey);

	/**
	 * Return the micronode list field with the given key.
	 * 
	 * @param fieldKey
	 * @return
	 */
	MicronodeFieldList getMicronodeFieldList(String fieldKey);

	/**
	 * Return the node list field for the given key.
	 * 
	 * @param fieldKey
	 * @return
	 */
	NodeFieldList getNodeFieldList(String fieldKey);

	/**
	 * Check whether the field map is empty.
	 * 
	 * @return
	 */
	boolean isEmpty();

	/**
	 * Check whether a field which uses the given key is stored in the map.
	 * 
	 * @param fieldKey
	 * @return
	 */
	boolean hasField(String fieldKey);

	/**
	 * Return the field with the given key.
	 * 
	 * @param fieldKey
	 * @param fieldSchema
	 * @return
	 */
	Field getField(String fieldKey, FieldSchema fieldSchema);

	/**
	 * Return the deserialize field.
	 * 
	 * @param fieldKey
	 *            Key of the current field
	 * @param type
	 *            Expected field
	 * @param listType
	 *            Optional expected list type of the field
	 * @param expand
	 *            The field will be expanded (if possible) when set to true
	 */
	<T extends Field> T getField(String fieldKey, FieldTypes type, String listType, boolean expand);

}