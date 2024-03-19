package com.gentics.mesh.graphdb;

import com.arcadedb.schema.Type;
import com.gentics.mesh.madl.field.FieldType;

/**
 * The field mapper provides the bridge between field types that were defined in MADL and field types of OrientDB.
 */
public final class FieldTypeMapper {

	/**
	 * Convert the vendor agnostic type to an orientdb specific type.
	 * 
	 * @param fieldType
	 * @return
	 */
	public static Type toType(FieldType fieldType) {
		switch (fieldType) {
		case LINK:
			return Type.LINK;
		case STRING:
			return Type.STRING;
		case INTEGER:
			return Type.INTEGER;
		case LONG:
			return Type.LONG;
		case BOOLEAN:
			return Type.BOOLEAN;
		case STRING_SET:
			return Type.MAP;
		case STRING_LIST:
			return Type.LIST;
		default:
			throw new RuntimeException("Unsupported type {" + fieldType + "}");
		}
	}

	/**
	 * Convert the vendor agnostic type to an orientdb specific sub type (eg. string for string lists)
	 * 
	 * @param fieldType
	 * @return
	 */
	public static Type toSubType(FieldType fieldType) {
		switch (fieldType) {
		case STRING_SET:
			return Type.STRING;
		case STRING_LIST:
			return Type.STRING;
		default:
			return null;
		}
	}
}
