package com.gentics.mesh.etc.config.env;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gentics.mesh.annotation.Setter;

import io.vertx.core.json.JsonObject;

public class TestMethodSetOption implements Option {

	Map<String, Object> values = new HashMap<>();

	@Setter
	@EnvironmentVariable(name = TestOptions.STRING_VALUE_ENV, description = "")
	public void setStringValue(String stringValue) {
		values.put(TestOptions.STRING_VALUE_ENV, stringValue);
	}

	@Setter
	@EnvironmentVariable(name = TestOptions.DOUBLE_VALUE_ENV, description = "")
	public void setDoubleValue(Double doubleValue) {
		values.put(TestOptions.DOUBLE_VALUE_ENV, doubleValue);
	}

	@Setter
	@EnvironmentVariable(name = TestOptions.DOUBLE_VALUE_PRIMITIVE_ENV, description = "")
	public void setDoubleValuePrimitive(double doubleValuePrimitive) {
		values.put(TestOptions.DOUBLE_VALUE_PRIMITIVE_ENV, doubleValuePrimitive);
	}

	@Setter
	@EnvironmentVariable(name = TestOptions.FLOAT_VALUE_PRIMITIVE_ENV, description = "")
	public void setFloatValuePrimitive(float floatValuePrimitive) {
		values.put(TestOptions.FLOAT_VALUE_PRIMITIVE_ENV, floatValuePrimitive);
	}

	@Setter
	@EnvironmentVariable(name = TestOptions.FLOAT_VALUE_ENV, description = "")
	public void setFloatValue(Float floatValue) {
		values.put(TestOptions.FLOAT_VALUE_ENV, floatValue);
	}

	@Setter
	@EnvironmentVariable(name = TestOptions.BOOLEAN_VALUE_PRIMITIVE_ENV, description = "")
	public void setBooleanValuePrimitive(boolean booleanValuePrimitive) {
		values.put(TestOptions.BOOLEAN_VALUE_PRIMITIVE_ENV, booleanValuePrimitive);
	}

	@Setter
	@EnvironmentVariable(name = TestOptions.BOOLEAN_VALUE_ENV, description = "")
	public void setBooleanValue(Boolean booleanValue) {
		values.put(TestOptions.BOOLEAN_VALUE_ENV, booleanValue);
	}

	@Setter
	@EnvironmentVariable(name = TestOptions.INTEGER_VALUE_ENV, description = "")
	public void setIntegerValue(Integer integerValue) {
		values.put(TestOptions.INTEGER_VALUE_ENV, integerValue);
	}

	@Setter
	@EnvironmentVariable(name = TestOptions.INTEGER_VALUE_PRIMITIVE_ENV, description = "")
	public void setIntegerValuePrimitive(int integerValuePrimitive) {
		values.put(TestOptions.INTEGER_VALUE_PRIMITIVE_ENV, integerValuePrimitive);
	}

	@Setter
	@EnvironmentVariable(name = TestOptions.LONG_VALUE_PRIMITIVE_ENV, description = "")
	public void setLongValuePrimitive(long longValuePrimitive) {
		values.put(TestOptions.LONG_VALUE_PRIMITIVE_ENV, longValuePrimitive);
	}

	@Setter
	@EnvironmentVariable(name = TestOptions.LONG_VALUE_ENV, description = "")
	public void setLongValue(Long longValue) {
		values.put(TestOptions.LONG_VALUE_ENV, longValue);
	}

	@Setter
	@EnvironmentVariable(name = TestOptions.JSON_OBJECT_ENV, description = "")
	public void setJsonObject(JsonObject jsonObj) {
		values.put(TestOptions.JSON_OBJECT_ENV, jsonObj);
	}

	@Setter
	@EnvironmentVariable(name = TestOptions.STRING_LIST_ENV, description = "")
	public void setStringList(List<String> stringList) {
		values.put(TestOptions.STRING_LIST_ENV, stringList);
	}

	@Setter
	@EnvironmentVariable(name = TestOptions.STRING_SET_ENV, description = "")
	public void setStringSet(Set<String> stringSet) {
		values.put(TestOptions.STRING_SET_ENV, stringSet);
	}
}
