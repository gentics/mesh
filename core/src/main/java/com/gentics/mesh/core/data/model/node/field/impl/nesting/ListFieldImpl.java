package com.gentics.mesh.core.data.model.node.field.impl.nesting;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.ALLOWED_SCHEMA;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_FIELD;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.data.model.FieldContainer;
import com.gentics.mesh.core.data.model.impl.SchemaImpl;
import com.gentics.mesh.core.data.model.node.field.basic.BasicField;
import com.gentics.mesh.core.data.model.node.field.nesting.AbstractComplexField;
import com.gentics.mesh.core.data.model.node.field.nesting.ListField;
import com.gentics.mesh.core.data.model.node.field.nesting.ListableField;
import com.gentics.mesh.core.data.model.node.field.nesting.NestingField;

public class ListFieldImpl<T extends ListableField> extends AbstractComplexField implements ListField<T>, FieldContainer {

	private static Logger log = LoggerFactory.getLogger(ListFieldImpl.class);

	@Override
	public Class<? extends T> getListType() {
		String type = getProperty("listType");
		if (type == null) {
			return null;
		}
		try {
			return (Class<? extends T>) Class.forName(type);
		} catch (ClassNotFoundException e) {
			log.error("Could not load class for list type {" + type + "}.", e);
			return null;
		}
	}

	@Override
	public void setListType(Class<? extends T> t) {
		setProperty("listType", t.getName());
	}

	@Override
	public List<? extends T> getList() {
		Class<?> type = getListType();
		System.out.println(type.getName());
		if (type.isAssignableFrom(NestingField.class)) {
			return (List<? extends T>) out(HAS_FIELD).toListExplicit(type);
		} else if(type.isAssignableFrom(NodeFieldImpl.class)) {
			return (List<? extends T>) outE(HAS_FIELD).toListExplicit(type);
		} else if(BasicField.class.isAssignableFrom(type)) {
			List<? extends T> list = new ArrayList<>();
			for(String key : getPropertyKeys()) {
				System.out.println("Values: "  + key + " " + getProperty(key));
			}
			return list;
		} else {
			throw new RuntimeException("Invalid list type {" + type + "}");
		}
	}

	public List<? extends SchemaImpl> getAllowedSchemas() {
		return out(ALLOWED_SCHEMA).has(SchemaImpl.class).toListExplicit(SchemaImpl.class);
	}

	public void addAllowedSchema(SchemaImpl schema) {
		linkOut(schema, ALLOWED_SCHEMA);
	}

	public void removeAllowedSchema(SchemaImpl schema) {
		unlinkOut(schema, ALLOWED_SCHEMA);
	}

}
