package com.gentics.mesh.core.data.impl;

import java.util.List;
import java.util.function.Supplier;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformer;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
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

	STRING("string", StringGraphFieldImpl.class, StringGraphFieldImpl.STRING_TRANSFORMER, StringGraphFieldImpl.STRING_UPDATER,
		StringGraphFieldImpl.STRING_GETTER),

	STRING_LIST("list.string", StringGraphFieldListImpl.class, StringGraphFieldListImpl.STRING_LIST_TRANSFORMER,
		StringGraphFieldListImpl.STRING_LIST_UPDATER, StringGraphFieldListImpl.STRING_LIST_GETTER),

	NUMBER("number", NumberGraphFieldImpl.class, NumberGraphFieldImpl.NUMBER_TRANSFORMER, NumberGraphFieldImpl.NUMBER_UPDATER,
		NumberGraphFieldImpl.NUMBER_GETTER),

	NUMBER_LIST("list.number", NumberGraphFieldListImpl.class, NumberGraphFieldListImpl.NUMBER_LIST_TRANSFORMER,
		NumberGraphFieldListImpl.NUMBER_LIST_UPDATER, NumberGraphFieldListImpl.NUMBER_LIST_GETTER),

	DATE("date", DateGraphFieldImpl.class, DateGraphFieldImpl.DATE_TRANSFORMER, DateGraphFieldImpl.DATE_UPDATER,
		DateGraphFieldImpl.DATE_GETTER),

	DATE_LIST("list.date", DateGraphFieldListImpl.class, DateGraphFieldListImpl.DATE_LIST_TRANSFORMER, DateGraphFieldListImpl.DATE_LIST_UPDATER,
		DateGraphFieldListImpl.DATE_LIST_GETTER),

	BOOLEAN("boolean", BooleanGraphFieldImpl.class, BooleanGraphFieldImpl.BOOLEAN_TRANSFORMER, BooleanGraphFieldImpl.BOOLEAN_UPDATER,
		BooleanGraphFieldImpl.BOOLEAN_GETTER),

	BOOLEAN_LIST("list.boolean", BooleanGraphFieldListImpl.class, BooleanGraphFieldListImpl.BOOLEAN_LIST_TRANSFORMER,
		BooleanGraphFieldListImpl.BOOLEAN_LIST_UPDATER, BooleanGraphFieldListImpl.BOOLEAN_LIST_GETTER),

	HTML("html", HtmlGraphFieldImpl.class, HtmlGraphFieldImpl.HTML_TRANSFORMER, HtmlGraphFieldImpl.HTML_UPDATER,
		HtmlGraphFieldImpl.HTML_GETTER),

	HTML_LIST("list.html", HtmlGraphFieldListImpl.class, HtmlGraphFieldListImpl.HTML_LIST_TRANSFORMER, HtmlGraphFieldListImpl.HTML_LIST_UPDATER,
		HtmlGraphFieldListImpl.HTML_LIST_GETTER),

	MICRONODE("micronode", MicronodeGraphFieldImpl.class, MicronodeGraphFieldImpl.MICRONODE_TRANSFORMER, MicronodeGraphFieldImpl.MICRONODE_UPDATER,
		MicronodeGraphFieldImpl.MICRONODE_GETTER),

	MICRONODE_LIST("list.micronode", MicronodeGraphFieldListImpl.class, MicronodeGraphFieldListImpl.MICRONODE_LIST_TRANSFORMER,
		MicronodeGraphFieldListImpl.MICRONODE_LIST_UPDATER, MicronodeGraphFieldListImpl.MICRONODE_LIST_GETTER),

	NODE("node", NodeGraphFieldImpl.class, NodeGraphFieldImpl.NODE_TRANSFORMER, NodeGraphFieldImpl.NODE_UPDATER,
		NodeGraphFieldImpl.NODE_GETTER),

	NODE_LIST("list.node", NodeGraphFieldListImpl.class, NodeGraphFieldListImpl.NODE_LIST_TRANSFORMER, NodeGraphFieldListImpl.NODE_LIST_UPDATER,
		NodeGraphFieldListImpl.NODE_LIST_GETTER),

	BINARY("binary", BinaryGraphFieldImpl.class, BinaryGraphFieldImpl.BINARY_TRANSFORMER, BinaryGraphFieldImpl.BINARY_UPDATER,
		BinaryGraphFieldImpl.BINARY_GETTER);

	private String combinedType;
	private FieldTransformer transformer;
	private FieldUpdater updater;
	private FieldGetter getter;
	private Class<? extends HibField> domainClass;

	private GraphFieldTypes(String combinedType, Class<? extends HibField> clazz, FieldTransformer transformer, FieldUpdater updater,
		FieldGetter getter) {
		this.combinedType = combinedType;
		this.domainClass = clazz;
		this.transformer = transformer;
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
	 * Return the field specific transformer.
	 * 
	 * @return
	 */
	public FieldTransformer<? extends Field> getTransformer() {
		return transformer;
	}

	/**
	 * Return the specific internal field type for the given field schema.
	 * 
	 * @param schema
	 * @return
	 */
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
	 * Invoke the type specific field transformer using the provided information.
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
		FieldSchema fieldSchema, List<String> languageTags, int level, Supplier<HibNode> parentNode) {
		return getTransformer().transform(container, ac, fieldKey, fieldSchema, languageTags, level, parentNode);
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
	public void updateField(HibFieldContainer container, InternalActionContext ac, FieldMap fieldMap, String fieldKey,
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
	public HibField getField(GraphFieldContainer container, FieldSchema fieldSchema) {
		return getter.get(container, fieldSchema);
	}

	/**
	 * Returns the graph domain model class for the field type.
	 * 
	 * @return
	 */
	public Class<? extends HibField> getDomainClass() {
		return domainClass;
	}
}
