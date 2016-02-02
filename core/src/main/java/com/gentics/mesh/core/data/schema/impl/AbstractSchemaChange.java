package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CHANGE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;

import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * @see SchemaChange
 */
public abstract class AbstractSchemaChange extends MeshVertexImpl implements SchemaChange {

	private static String OPERATION_NAME_PROPERTY_KEY = "operation";

	private static String MIGRATION_SCRIPT_PROPERTY_KEY = "migrationScript";

	public static void checkIndices(Database database) {
		database.addVertexType(AbstractSchemaChange.class);
	}

	@Override
	public SchemaChange getNextChange() {
		return (SchemaChange) out(HAS_CHANGE).nextOrDefault(null);
	}

	@Override
	public SchemaChange setNextChange(SchemaChange change) {
		setUniqueLinkOutTo(change.getImpl(), HAS_CHANGE);
		return this;
	}

	@Override
	public SchemaChange getPreviousChange() {
		return (SchemaChange) in(HAS_CHANGE).nextOrDefault(null);
	}

	@Override
	public SchemaChange setPreviousChange(SchemaChange change) {
		setUniqueLinkInTo(change.getImpl(), HAS_CHANGE);
		return this;
	}

	@Override
	public SchemaChange setOperation(SchemaChangeOperation operation) {
		setProperty(OPERATION_NAME_PROPERTY_KEY, operation.name());
		return this;
	}

	@Override
	public SchemaChangeOperation getOperation() {
		return getProperty(OPERATION_NAME_PROPERTY_KEY);
	}

	@Override
	public SchemaContainer getPreviousSchemaContainer() {
		return in(HAS_SCHEMA_CONTAINER).has(SchemaContainerImpl.class).nextOrDefaultExplicit(SchemaContainerImpl.class, null);
	}

	@Override
	public SchemaChange setPreviousSchemaContainer(SchemaContainer container) {
		setSingleLinkInTo(container.getImpl(), HAS_SCHEMA_CONTAINER);
		return this;
	}

	@Override
	public SchemaContainer getNextSchemaContainer() {
		return out(HAS_SCHEMA_CONTAINER).has(SchemaContainerImpl.class).nextOrDefaultExplicit(SchemaContainerImpl.class, null);
	}

	@Override
	public SchemaChange setNextSchemaContainer(SchemaContainer container) {
		setSingleLinkOutTo(container.getImpl(), HAS_SCHEMA_CONTAINER);
		return this;
	}

	@Override
	public String getMigrationScript() {
		return getProperty(MIGRATION_SCRIPT_PROPERTY_KEY, String.class);
	}

	@Override
	public SchemaChange setMigrationScript(String migrationScript) {
		setProperty(MIGRATION_SCRIPT_PROPERTY_KEY, migrationScript);
		return this;
	}
}
