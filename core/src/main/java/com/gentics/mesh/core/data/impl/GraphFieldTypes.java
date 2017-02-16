package com.gentics.mesh.core.data.impl;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformator;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.impl.BinaryGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.BooleanGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.DateGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.HtmlGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.MicronodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.NodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.NumberGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.StringGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.impl.BooleanGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.DateGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.HtmlGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.MicronodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NumberGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.StringGraphFieldListImpl;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;

/**
 * List of all graph field types.
 */
public enum GraphFieldTypes {

	STRING("string", StringGraphFieldImpl.STRING_TRANSFORMATOR, StringGraphFieldImpl.STRING_UPDATER,
			StringGraphFieldImpl.STRING_GETTER),

	STRING_LIST("list.string", StringGraphFieldListImpl.STRING_LIST_TRANSFORMATOR,
			StringGraphFieldListImpl.STRING_LIST_UPDATER, StringGraphFieldListImpl.STRING_LIST_GETTER),

	NUMBER("number", NumberGraphFieldImpl.NUMBER_TRANSFORMATOR, NumberGraphFieldImpl.NUMBER_UPDATER,
			NumberGraphFieldImpl.NUMBER_GETTER),

	NUMBER_LIST("list.number", NumberGraphFieldListImpl.NUMBER_LIST_TRANSFORMATOR,
			NumberGraphFieldListImpl.NUMBER_LIST_UPDATER, NumberGraphFieldListImpl.NUMBER_LIST_GETTER),

	DATE("date", DateGraphFieldImpl.DATE_TRANSFORMATOR, DateGraphFieldImpl.DATE_UPDATER,
			DateGraphFieldImpl.DATE_GETTER),

	DATE_LIST("list.date", DateGraphFieldListImpl.DATE_LIST_TRANSFORMATOR, DateGraphFieldListImpl.DATE_LIST_UPDATER,
			DateGraphFieldListImpl.DATE_LIST_GETTER),

	BOOLEAN("boolean", BooleanGraphFieldImpl.BOOLEAN_TRANSFORMATOR, BooleanGraphFieldImpl.BOOLEAN_UPDATER,
			BooleanGraphFieldImpl.BOOLEAN_GETTER),

	BOOLEAN_LIST("list.boolean", BooleanGraphFieldListImpl.BOOLEAN_LIST_TRANSFORMATOR,
			BooleanGraphFieldListImpl.BOOLEAN_LIST_UPDATER, BooleanGraphFieldListImpl.BOOLEAN_LIST_GETTER),

	HTML("html", HtmlGraphFieldImpl.HTML_TRANSFORMATOR, HtmlGraphFieldImpl.HTML_UPDATER,
			HtmlGraphFieldImpl.HTML_GETTER),

	HTML_LIST("list.html", HtmlGraphFieldListImpl.HTML_LIST_TRANSFORMATOR, HtmlGraphFieldListImpl.HTML_LIST_UPDATER,
			HtmlGraphFieldListImpl.HTML_LIST_GETTER),

	MICRONODE("micronode", MicronodeGraphFieldImpl.MICRONODE_TRANSFORMATOR, MicronodeGraphFieldImpl.MICRONODE_UPDATER,
			MicronodeGraphFieldImpl.MICRONODE_GETTER),

	MICRONODE_LIST("list.micronode", MicronodeGraphFieldListImpl.MICRONODE_LIST_TRANSFORMATOR,
			MicronodeGraphFieldListImpl.MICRONODE_LIST_UPDATER, MicronodeGraphFieldListImpl.MICRONODE_LIST_GETTER),

	NODE("node", NodeGraphFieldImpl.NODE_TRANSFORMATOR, NodeGraphFieldImpl.NODE_UPDATER,
			NodeGraphFieldImpl.NODE_GETTER),

	NODE_LIST("list.node", NodeGraphFieldListImpl.NODE_LIST_TRANSFORMATOR, NodeGraphFieldListImpl.NODE_LIST_UPDATER,
			NodeGraphFieldListImpl.NODE_LIST_GETTER),

	BINARY("binary", BinaryGraphFieldImpl.BINARY_TRANSFORMATOR, BinaryGraphFieldImpl.BINARY_UPDATER,
			BinaryGraphFieldImpl.BINARY_GETTER);

	private String combinedType;
	private FieldTransformator transformator;
	private FieldUpdater updater;
	private FieldGetter getter;

	private GraphFieldTypes(String combinedType, FieldTransformator transformator, FieldUpdater updater,
			FieldGetter getter) {
		this.combinedType = combinedType;
		this.transformator = transformator;
		this.updater = updater;
		this.getter = getter;
	}

	/**
	 * Return the combined field type.
	 * 
	 * @return
	 */
	public String getCombinedType() {
		return combinedType;
	}

	/**
	 * Return the field specific transformator.
	 * 
	 * @return
	 */
	public FieldTransformator<? extends Field> getTransformator() {
		return transformator;
	}

	public static GraphFieldTypes valueByFieldSchema(FieldSchema schema) {
		String combinedType = schema.getType();
		if (schema instanceof ListFieldSchema) {
			combinedType += "." + ((ListFieldSchema) schema).getListType();
		}
		for (GraphFieldTypes type : values()) {
			if (type.getCombinedType().equals(combinedType)) {
				return type;
			}
		}
		return null;
	}

	/**
	 * Invoke the type specific field transformator using the provided information.
	 * 
	 * @param container
	 *            Field container which will be used to load the fields
	 * @param ac
	 *            Action context
	 * @param fieldKey
	 *            Field key
	 * @param fieldSchema
	 *            Field schema used to identify the field type
	 * @param languageTags
	 *            Language tags used to apply language fallback
	 * @param level
	 *            Current level of transformation
	 * @param parentNode
	 * @return
	 */
	public Field getRestFieldFromGraph(GraphFieldContainer container, InternalActionContext ac, String fieldKey,
			FieldSchema fieldSchema, List<String> languageTags, int level, Node parentNode) {
		return getTransformator().transform(container, ac, fieldKey, fieldSchema, languageTags, level, parentNode);
	}

	/**
	 * Invoke the type specific field updater using the provided information.
	 * 
	 * @param container
	 *            Field container which will be used to load the fields
	 * @param ac
	 *            Action context
	 * @param fieldMap
	 *            Fieldmap which contains the source
	 * @param fieldKey
	 *            Field key
	 * @param fieldSchema
	 *            Field schema to be used to identify the type of the field
	 * @param schema
	 */
	public void updateField(GraphFieldContainer container, InternalActionContext ac, FieldMap fieldMap, String fieldKey,
			FieldSchema fieldSchema, FieldSchemaContainer schema) {
		updater.update(container, ac, fieldMap, fieldKey, fieldSchema, schema);
	}

	/**
	 * Invoke the type specific field getter using the provided information.
	 * 
	 * @param container
	 *            Container from which the field should be loaded
	 * @param fieldSchema
	 *            Field schema which will be used to identify the field
	 * @return
	 */
	public GraphField getField(GraphFieldContainer container, FieldSchema fieldSchema) {
		return getter.get(container, fieldSchema);
	}
}
