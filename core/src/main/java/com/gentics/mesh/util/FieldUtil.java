package com.gentics.mesh.util;

import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;

public final class FieldUtil {

	public static StringFieldSchema createStringFieldSchema(String defaultString) {
		StringFieldSchema fieldSchema = new StringFieldSchemaImpl();
		return fieldSchema;
	}

	public static StringField createStringField(String string) {
		StringField field = new StringFieldImpl();
		field.setString(string);
		return field;
	}

	public static Field createHtmlField(String html) {
		HtmlField field = new HtmlFieldImpl();
		field.setHTML(html);
		return field;
	}

	public static Field createNumberField(String number) {
		NumberField field = new NumberFieldImpl();
		field.setNumber(number);
		return field;
	}

	public static Field createBooleanField(Boolean value) {
		BooleanField field = new BooleanFieldImpl();
		field.setValue(value);
		return field;
	}

	public static Field createDateField(String date) {
		DateField field = new DateFieldImpl();
		field.setDate(date);
		return field;
	}

	public static Field createNodeField(String uuid) {
		NodeFieldImpl field = new NodeFieldImpl();
		field.setUuid(uuid);
		return field;
	}

	public static Field createNodeListField() {
		NodeFieldListImpl field = new NodeFieldListImpl();
		field.add(new NodeFieldListItem(UUIDUtil.randomUUID()));
		field.add(new NodeFieldListItem(UUIDUtil.randomUUID()));
		field.add(new NodeFieldListItem(UUIDUtil.randomUUID()));
		field.setOrder("desc");
		field.setOrderBy("name");
		return field;
	}

	public static Field createNumberListField(String... numbers) {
		NumberFieldListImpl field = new NumberFieldListImpl();
		for (String number : numbers) {
			field.add(number);
		}
		field.setOrder("asc");
		return field;
	}

	public static Field createStringListField(String... strings) {
		StringFieldListImpl field = new StringFieldListImpl();
		for (String string : strings) {
			field.add(string);
		}
		field.setOrder("desc");
		field.setOrderBy("name");
		return field;
	}
}
