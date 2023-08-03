package com.gentics.mesh.graphdb;

import com.gentics.mesh.madl.field.FieldType;
import com.orientechnologies.orient.core.metadata.schema.OType;

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
	public static OType toType(FieldType fieldType) {
		switch (fieldType) {
		case LINK:
			return OType.LINK;
		case STRING:
			return OType.STRING;
		case INTEGER:
			return OType.INTEGER;
		case LONG:
			return OType.LONG;
		case BOOLEAN:
			return OType.BOOLEAN;
		case STRING_SET:
			return OType.EMBEDDEDSET;
		case STRING_LIST:
			return OType.EMBEDDEDLIST;
		case FLOAT:
			return OType.FLOAT;
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
	public static OType toSubType(FieldType fieldType) {
		switch (fieldType) {
		case STRING_SET:
			return OType.STRING;
		case STRING_LIST:
			return OType.STRING;
		default:
			return null;
		}
	}
}
