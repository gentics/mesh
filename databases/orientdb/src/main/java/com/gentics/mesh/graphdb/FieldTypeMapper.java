package com.gentics.mesh.graphdb;

import com.orientechnologies.orient.core.metadata.schema.OType;
import com.syncleus.ferma.index.field.FieldType;

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
		case BOOLEAN:
			return OType.BOOLEAN;
		case STRING_SET:
			return OType.EMBEDDEDSET;
		case STRING_LIST:
			return OType.EMBEDDEDLIST;
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
