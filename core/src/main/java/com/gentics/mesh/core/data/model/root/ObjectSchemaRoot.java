package com.gentics.mesh.core.data.model.root;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.ObjectSchema;

public class ObjectSchemaRoot extends AbstractPersistable {

	public List<ObjectSchema> getSchemas() {
		return out(BasicRelationships.HAS_OBJECT_SCHEMA).toList(ObjectSchema.class);
	}

	public void addSchema(ObjectSchema schema) {
		linkOut(schema, BasicRelationships.HAS_OBJECT_SCHEMA);
	}

	// TODO unique index

}
