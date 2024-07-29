package com.gentics.mesh.hibernate.data.node.field;

import static com.gentics.mesh.core.data.node.field.RestGetters.BINARY_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.BOOLEAN_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.BOOLEAN_LIST_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.DATE_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.DATE_LIST_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.HTML_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.HTML_LIST_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.MICRONODE_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.MICRONODE_LIST_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.NODE_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.NODE_LIST_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.NUMBER_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.NUMBER_LIST_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.S3_BINARY_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.STRING_GETTER;
import static com.gentics.mesh.core.data.node.field.RestGetters.STRING_LIST_GETTER;
import static com.gentics.mesh.core.data.node.field.RestTransformers.BINARY_TRANSFORMER;
import static com.gentics.mesh.core.data.node.field.RestTransformers.BOOLEAN_LIST_TRANSFORMER;
import static com.gentics.mesh.core.data.node.field.RestTransformers.BOOLEAN_TRANSFORMER;
import static com.gentics.mesh.core.data.node.field.RestTransformers.DATE_LIST_TRANSFORMER;
import static com.gentics.mesh.core.data.node.field.RestTransformers.DATE_TRANSFORMER;
import static com.gentics.mesh.core.data.node.field.RestTransformers.HTML_LIST_TRANSFORMER;
import static com.gentics.mesh.core.data.node.field.RestTransformers.HTML_TRANSFORMER;
import static com.gentics.mesh.core.data.node.field.RestTransformers.MICRONODE_LIST_TRANSFORMER;
import static com.gentics.mesh.core.data.node.field.RestTransformers.MICRONODE_TRANSFORMER;
import static com.gentics.mesh.core.data.node.field.RestTransformers.NODE_LIST_TRANSFORMER;
import static com.gentics.mesh.core.data.node.field.RestTransformers.NODE_TRANSFORMER;
import static com.gentics.mesh.core.data.node.field.RestTransformers.NUMBER_LIST_TRANSFORMER;
import static com.gentics.mesh.core.data.node.field.RestTransformers.NUMBER_TRANSFORMER;
import static com.gentics.mesh.core.data.node.field.RestTransformers.S3_BINARY_TRANSFORMER;
import static com.gentics.mesh.core.data.node.field.RestTransformers.STRING_LIST_TRANSFORMER;
import static com.gentics.mesh.core.data.node.field.RestTransformers.STRING_TRANSFORMER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.BINARY_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.BOOLEAN_LIST_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.BOOLEAN_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.DATE_LIST_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.DATE_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.HTML_LIST_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.HTML_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.MICRONODE_LIST_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.MICRONODE_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.NODE_LIST_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.NODE_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.NUMBER_LIST_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.NUMBER_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.S3_BINARY_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.STRING_LIST_UPDATER;
import static com.gentics.mesh.core.data.node.field.RestUpdaters.STRING_UPDATER;

import java.util.List;
import java.util.function.Supplier;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformer;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;

/**
 * Hibernate field types and actor functions.
 * 
 * @author plyhun
 *
 */
public enum HibFieldTypes {

	STRING("string", STRING_TRANSFORMER, STRING_UPDATER, STRING_GETTER),
	STRING_LIST("list.string", STRING_LIST_TRANSFORMER, STRING_LIST_UPDATER, STRING_LIST_GETTER),
	NUMBER("number", NUMBER_TRANSFORMER, NUMBER_UPDATER, NUMBER_GETTER),
	NUMBER_LIST("list.number", NUMBER_LIST_TRANSFORMER, NUMBER_LIST_UPDATER, NUMBER_LIST_GETTER),
	DATE("date", DATE_TRANSFORMER, DATE_UPDATER, DATE_GETTER),
	DATE_LIST("list.date", DATE_LIST_TRANSFORMER, DATE_LIST_UPDATER, DATE_LIST_GETTER),
	BOOLEAN("boolean", BOOLEAN_TRANSFORMER, BOOLEAN_UPDATER, BOOLEAN_GETTER),
	BOOLEAN_LIST("list.boolean", BOOLEAN_LIST_TRANSFORMER, BOOLEAN_LIST_UPDATER, BOOLEAN_LIST_GETTER),
	HTML("html", HTML_TRANSFORMER, HTML_UPDATER, HTML_GETTER),
	HTML_LIST("list.html", HTML_LIST_TRANSFORMER, HTML_LIST_UPDATER, HTML_LIST_GETTER),
	MICRONODE("micronode", MICRONODE_TRANSFORMER, MICRONODE_UPDATER, MICRONODE_GETTER),
	MICRONODE_LIST("list.micronode", MICRONODE_LIST_TRANSFORMER, MICRONODE_LIST_UPDATER, MICRONODE_LIST_GETTER),
	NODE("node", NODE_TRANSFORMER, NODE_UPDATER, NODE_GETTER),
	NODE_LIST("list.node", NODE_LIST_TRANSFORMER, NODE_LIST_UPDATER, NODE_LIST_GETTER),
	BINARY("binary", BINARY_TRANSFORMER, BINARY_UPDATER, BINARY_GETTER),
	S3BINARY("s3binary", S3_BINARY_TRANSFORMER, S3_BINARY_UPDATER, S3_BINARY_GETTER);

	private final String combinedType;
	private final FieldTransformer<?> transformer;
	private final FieldUpdater updater;
	private final FieldGetter getter;

	HibFieldTypes(String combinedType, FieldTransformer<?> transformer, FieldUpdater updater, FieldGetter getter) {
		this.combinedType = combinedType;
		this.transformer = transformer;
		this.updater = updater;
		this.getter = getter;
	}

	public Field getRestField(HibFieldContainer container, InternalActionContext ac, String fieldKey,
							  FieldSchema fieldSchema, List<String> languageTags, int level, Supplier<HibNode> parentNode) {
		return transformer.transform(container, ac, fieldKey, fieldSchema, languageTags, level, parentNode);
	}

	public void updateField(HibFieldContainer container, InternalActionContext ac, FieldMap fieldMap, String fieldKey, FieldSchema fieldSchema, FieldSchemaContainer schema) {
		updater.update(container, ac, fieldMap, fieldKey, fieldSchema, schema);
	}

	public HibField getField(HibFieldContainer container, FieldSchema schema) {
		return getter.get(container, schema);
	}

	public static HibFieldTypes fromFieldSchema(FieldSchema schema) {
		String combinedType = (schema instanceof ListFieldSchema) 
				? (schema.getType() + "." + ((ListFieldSchema)schema).getListType()) 
				: schema.getType();
		for (HibFieldTypes type : values()) {
			if (type.combinedType.equals(combinedType)) {
				return type;
			}
		}

		return null;
	}
}
