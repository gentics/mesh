package com.gentics.mesh.etc.config.env;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.vertx.core.json.JsonObject;

public class TestMethodSetOption implements Option {

	Map<String, Object> values = new HashMap<>();


	@EnvironmentVariable(name = TestOptions.STRING_VALUE_ENV, description = "")
	public void setStringValue(String stringValue) {
		values.put(TestOptions.STRING_VALUE_ENV, stringValue);
	}

	@EnvironmentVariable(name = TestOptions.DOUBLE_VALUE_ENV, description = "")
	public void setDoubleValue(Double doubleValue) {
		values.put(TestOptions.DOUBLE_VALUE_ENV, doubleValue);
	}

	@EnvironmentVariable(name = TestOptions.DOUBLE_VALUE_PRIMITIVE_ENV, description = "")
	public void setDoubleValuePrimitive(double doubleValuePrimitive) {
		values.put(TestOptions.DOUBLE_VALUE_PRIMITIVE_ENV, doubleValuePrimitive);
	}

	@EnvironmentVariable(name = TestOptions.FLOAT_VALUE_PRIMITIVE_ENV, description = "")
	public void setFloatValuePrimitive(float floatValuePrimitive) {
		values.put(TestOptions.FLOAT_VALUE_PRIMITIVE_ENV, floatValuePrimitive);
	}

	@EnvironmentVariable(name = TestOptions.FLOAT_VALUE_ENV, description = "")
	public void setFloatValue(Float floatValue) {
		values.put(TestOptions.FLOAT_VALUE_ENV, floatValue);
	}

	@EnvironmentVariable(name = TestOptions.BOOLEAN_VALUE_PRIMITIVE_ENV, description = "")
	public void setBooleanValuePrimitive(boolean booleanValuePrimitive) {
		values.put(TestOptions.BOOLEAN_VALUE_PRIMITIVE_ENV, booleanValuePrimitive);
	}

	@EnvironmentVariable(name = TestOptions.BOOLEAN_VALUE_ENV, description = "")
	public void setBooleanValue(Boolean booleanValue) {
		values.put(TestOptions.BOOLEAN_VALUE_ENV, booleanValue);
	}

	@EnvironmentVariable(name = TestOptions.INTEGER_VALUE_ENV, description = "")
	public void setIntegerValue(Integer integerValue) {
		values.put(TestOptions.INTEGER_VALUE_ENV, integerValue);
	}

	@EnvironmentVariable(name = TestOptions.INTEGER_VALUE_PRIMITIVE_ENV, description = "")
	public void setIntegerValuePrimitive(int integerValuePrimitive) {
		values.put(TestOptions.INTEGER_VALUE_PRIMITIVE_ENV, integerValuePrimitive);
	}

	@EnvironmentVariable(name = TestOptions.LONG_VALUE_PRIMITIVE_ENV, description = "")
	public void setLongValuePrimitive(long longValuePrimitive) {
		values.put(TestOptions.LONG_VALUE_PRIMITIVE_ENV, longValuePrimitive);
	}

	@EnvironmentVariable(name = TestOptions.LONG_VALUE_ENV, description = "")
	public void setLongValue(Long longValue) {
		values.put(TestOptions.LONG_VALUE_ENV, longValue);
	}

	@EnvironmentVariable(name = TestOptions.JSON_OBJECT_ENV, description = "")
	public void setJsonObject(JsonObject jsonObj) {
		values.put(TestOptions.JSON_OBJECT_ENV, jsonObj);
	}

	@EnvironmentVariable(name = TestOptions.STRING_LIST_ENV, description = "")
	public void setStringList(List<String> stringList) {
		values.put(TestOptions.STRING_LIST_ENV, stringList);
	}

	@EnvironmentVariable(name = TestOptions.STRING_SET_ENV, description = "")
	public void setStringSet(Set<String> stringSet) {
		values.put(TestOptions.STRING_SET_ENV, stringSet);
	}
}
