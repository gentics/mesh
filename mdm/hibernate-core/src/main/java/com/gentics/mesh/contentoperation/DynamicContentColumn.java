package com.gentics.mesh.contentoperation;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.hibernate.data.node.field.impl.HibNumberFieldImpl;

/**
 * A dynamic content column, that is, a column that is not common to all the content table, but is rather
 * specific for the content schema.
 */
public class DynamicContentColumn implements ContentColumn, Serializable {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = -5001788640387029530L;

	private final String label;
	private final FieldTypes fieldType;

	public DynamicContentColumn(FieldSchema field) {
		this.label = buildColumnName(field);
		this.fieldType = FieldTypes.valueByName(field.getType());
	}

	/**
	 * Make a proper column name for the schema field.
	 * 
	 * @param field
	 * @return
	 */
	public static String buildColumnName(FieldSchema field) {
		String combinedType = (field instanceof ListFieldSchema)
				? (field.getType() + "." + ((ListFieldSchema)field).getListType())
				: field.getType();
		return field.getName() + "-" + combinedType;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public Class<?> getJavaClass() {
		switch (fieldType) {
			case STRING:
			case HTML:
				return String.class;
			case NUMBER:
				// TODO make configurable for number or big decimal
				// or split into number / money type.
				return Double.class;
			case DATE:
				return Instant.class;
			case BOOLEAN:
				return Boolean.class;
			case NODE:
			case LIST:
			case BINARY:
			case S3BINARY:
			case MICRONODE:
				return UUID.class;
		}
		return null;
	}

	/**
	 * The field type of this column
	 * @return
	 */
	public FieldTypes getFieldType() {
		return fieldType;
	}

	@Override
	public Object transformToPersistedValue(Object value) {
		if (fieldType == FieldTypes.NUMBER && value instanceof Number) {
			return HibNumberFieldImpl.convertToDouble((Number)value);
		} else if (fieldType == FieldTypes.DATE) {
			if (value instanceof String) {
				return Instant.parse(value.toString());
			} else if (value instanceof Long) {
				return Instant.ofEpochMilli((long) value);
			} else if (value instanceof Timestamp) {
				return Timestamp.class.cast(value).toInstant();
			} else {
				return ContentColumn.super.transformToPersistedValue(value);
			}
		} else {
			return ContentColumn.super.transformToPersistedValue(value);
		}
	}

	@Override
	public int hashCode() {
		return label.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DynamicContentColumn) {
			return StringUtils.equals(label, ((DynamicContentColumn) obj).getLabel());
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return String.format("%s field '%s'", fieldType, label);
	}
}
