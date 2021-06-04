package com.gentics.mesh.core.rest.node;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.S3BinaryField;
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
import com.gentics.mesh.core.rest.schema.Schema;

/**
 * A field map stores all fields of a node or micronode.
 */
public interface FieldMap extends RestModel {

	/**
	 * Add or update the field with the given key in the map.
	 * 
	 * @param fieldKey
	 * @param field
	 * @return
	 */
	Field put(String fieldKey, Field field);

	/**
	 * Add or update the field in the given entry.
	 * @param entry
	 * @return
	 */
	default Field put(Map.Entry<String, Field> entry) {
		return put(entry.getKey(), entry.getValue());
	}

	/**
	 * Puts a string field in the map.
	 * @param fieldKey
	 * @param string
	 * @return
	 */
	default Field putString(String fieldKey, String string) {
		return put(fieldKey, new StringFieldImpl().setString(string));
	}

	/**
	 * Add or update all fields in the given map.
	 *
	 * @param fieldMap
	 * @return
	 */
	FieldMap putAll(Map<String, Field> fieldMap);

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
	 * Return the s3 binary field with the given key.
	 *
	 * @param fieldKey
	 * @return
	 */
	S3BinaryField getS3BinaryField(String fieldKey);

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
	 * Returns true if the given field is an expanded node field.
	 *
	 * @param fieldKey
	 * @return
	 */
	boolean isExpandedNodeField(String fieldKey);

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
	// TODO why do we need to specifiy the field key? the field schema contains the key (name)
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

	/**
	 * Remove the a field with the given fieldKey from the fieldmap.
	 * 
	 * @param fieldKey
	 * @return true if an element with the given key could be removed otherwise false
	 */
	boolean remove(String fieldKey);

	/**
	 * Return the configured rest field values.
	 * 
	 * @param schema
	 *            Schema to be used to determine which field values should be selected
	 * @return
	 */
	@JsonIgnore
	Set<String> getUrlFieldValues(Schema schema);

	/**
	 * Delete all fields in this map.
	 */
	void clear();

	/**
	 * Convenience method for creating a field map with a single entry.
	 * @param key
	 * @param field
	 * @return
	 */
	static FieldMap of(String key, Field field) {
		FieldMap map = new FieldMapImpl();
		map.put(key, field);
		return map;
	}

	/**
	 * Convenience method for creating a field map with two entries.
	 * @param key
	 * @param field
	 * @return
	 */
	static FieldMap of(String key, Field field, String key2, Field field2) {
		FieldMap map = new FieldMapImpl();
		map.put(key, field);
		map.put(key2, field2);
		return map;
	}

	/**
	 * Convenience method for creating a field map with three entries.
	 * @param key
	 * @param field
	 * @return
	 */
	static FieldMap of(String key, Field field, String key2, Field field2, String key3, Field field3) {
		FieldMap map = new FieldMapImpl();
		map.put(key, field);
		map.put(key2, field2);
		map.put(key3, field3);
		return map;
	}

}