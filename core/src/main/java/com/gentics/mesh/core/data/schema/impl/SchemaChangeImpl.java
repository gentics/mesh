package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CHANGE;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.SchemaChangeOperation;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * @see SchemaChange
 */
public class SchemaChangeImpl extends MeshVertexImpl implements SchemaChange {

	private static String OPERATION_NAME_PROPERTY_KEY = "operation";

	private static String FIELD_KEY_PROPERTY_KEY = "fieldKey";

	public static void checkIndices(Database database) {
		database.addVertexType(SchemaChangeImpl.class);
	}

	@Override
	public SchemaChange getNextChange() {
		return in(HAS_CHANGE).has(SchemaChangeImpl.class).nextOrDefaultExplicit(SchemaChangeImpl.class, null);
	}

	@Override
	public SchemaChange getPreviousChange() {
		return out(HAS_CHANGE).has(SchemaChangeImpl.class).nextOrDefaultExplicit(SchemaChangeImpl.class, null);
	}

	@Override
	public SchemaChange setOperation(SchemaChangeOperation action) {
		setProperty(OPERATION_NAME_PROPERTY_KEY, action.name());
		return this;
	}

	@Override
	public SchemaChangeOperation getOperation() {
		return getProperty(OPERATION_NAME_PROPERTY_KEY);
	}

	@Override
	public SchemaChange setFieldKey(String fieldKey) {
		setProperty(FIELD_KEY_PROPERTY_KEY, fieldKey);
		return this;
	}

	@Override
	public SchemaContainer getNewSchemaContainer() {
		// TODO Traverse all changes until you find the new schema container
		return null;
	}

	@Override
	public SchemaContainer getOldSchemaContainer() {
		// TODO Traverse all changes until you find the old schema container
		return null;
	}

}
