package com.syncleus.ferma.index.field;

import java.util.LinkedHashMap;

public class FieldMap extends LinkedHashMap<String, FieldType> {

	private static final long serialVersionUID = 1L;

	public static FieldMap create(String key, FieldType type) {
		FieldMap fieldMap = new FieldMap();
		fieldMap.put(key, type);
		return fieldMap;
	}

	public static FieldMap create(String key, FieldType type, String key2, FieldType type2) {
		FieldMap fieldMap = new FieldMap();
		fieldMap.put(key, type);
		fieldMap.put(key2, type2);
		return fieldMap;
	}

}
