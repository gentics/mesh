package com.gentics.mesh.etc.config.env;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.vertx.core.json.JsonObject;

public class TestOptions implements Option {
	public static final String STRING_VALUE_ENV = "STRING_VALUE_ENV";
	public static final String DOUBLE_VALUE_ENV = "DOUBLE_VALUE_ENV";
	public static final String DOUBLE_VALUE_PRIMITIVE_ENV = "DOUBLE_VALUE_PRIMITIVE_ENV";
	public static final String FLOAT_VALUE_ENV = "FLOAT_VALUE_ENV";
	public static final String FLOAT_VALUE_PRIMITIVE_ENV = "FLOAT_VALUE_PRIMITIVE_ENV";
	public static final String INTEGER_VALUE_ENV = "INTEGER_VALUE_ENV";
	public static final String INTEGER_VALUE_PRIMITIVE_ENV = "INTEGER_VALUE_PRIMITIVE_ENV";
	public static final String LONG_VALUE_ENV = "LONG_VALUE_ENV";
	public static final String LONG_VALUE_PRIMITIVE_ENV = "LONG_VALUE_PRIMITIVE_ENV";
	public static final String BOOLEAN_VALUE_ENV = "BOOLEAN_VALUE_ENV";
	public static final String BOOLEAN_VALUE_PRIMITIVE_ENV = "BOOLEAN_VALUE_PRIMITIVE_ENV";
	public static final String JSON_OBJECT_ENV = "JSON_OBJECT_ENV";
	public static final String STRING_SET_ENV = "STRING_SET_ENV";
	public static final String STRING_LIST_ENV = "STRING_LIST_ENV";

	@EnvironmentVariable(name = STRING_VALUE_ENV, description = "")
	String stringValue;
	@EnvironmentVariable(name = DOUBLE_VALUE_ENV, description = "")
	Double doubleValue;
	@EnvironmentVariable(name = DOUBLE_VALUE_PRIMITIVE_ENV, description = "")
	double doubleValuePrimitive;
	@EnvironmentVariable(name = FLOAT_VALUE_PRIMITIVE_ENV, description = "")
	float floatValuePrimitive;
	@EnvironmentVariable(name = FLOAT_VALUE_ENV, description = "")
	Float floatValue;
	@EnvironmentVariable(name = BOOLEAN_VALUE_PRIMITIVE_ENV, description = "")
	boolean booleanValuePrimitive;
	@EnvironmentVariable(name = BOOLEAN_VALUE_ENV, description = "")
	Boolean booleanValue;
	@EnvironmentVariable(name = INTEGER_VALUE_ENV, description = "")
	Integer integerValue;
	@EnvironmentVariable(name = INTEGER_VALUE_PRIMITIVE_ENV, description = "")
	int integerValuePrimitive;
	@EnvironmentVariable(name = LONG_VALUE_PRIMITIVE_ENV, description = "")
	long longValuePrimitive;
	@EnvironmentVariable(name = LONG_VALUE_ENV, description = "")
	Long longValue;
	@EnvironmentVariable(name = JSON_OBJECT_ENV, description = "")
	JsonObject jsonObject;
	@EnvironmentVariable(name = STRING_SET_ENV, description = "")
	Set<String> stringSet;
	@EnvironmentVariable(name = STRING_LIST_ENV, description = "")
	List<String> stringList;

	Map<String, Object> getValues() throws Exception {
		Map<String, Object> values = new HashMap<>();
		for (Field field : getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(EnvironmentVariable.class)) {
				EnvironmentVariable envInfo = field.getAnnotation(EnvironmentVariable.class);
				field.setAccessible(true);
				values.put(envInfo.name(), field.get(this));
			}
		}
		return values;
	}

}
