package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CHANGE;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.SchemaChangeAction;
import com.gentics.mesh.core.data.schema.SchemaChangeset;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * @see SchemaChange
 */
public class SchemaChangeImpl extends MeshVertexImpl implements SchemaChange {

	private static String ACTION_NAME_PROPERTY_KEY = "action";

	private static String FIELD_KEY_PROPERTY_KEY = "fieldKey";

	public static void checkIndices(Database database) {
		database.addVertexType(SchemaChangeImpl.class);
	}

	@Override
	public SchemaChangeset getChangeset() {
		return in(HAS_CHANGE).has(SchemaChangesetImpl.class).nextOrDefaultExplicit(SchemaChangesetImpl.class, null);
	}

	@Override
	public SchemaChange setAction(SchemaChangeAction action) {
		setProperty(ACTION_NAME_PROPERTY_KEY, action.name());
		return this;
	}

	@Override
	public SchemaChange setFieldKey(String fieldKey) {
		setProperty(FIELD_KEY_PROPERTY_KEY, fieldKey);
		return this;
	}

}
