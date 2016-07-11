package com.gentics.mesh.core.data.impl;

import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.container.impl.TagGraphFieldContainerImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.list.impl.BooleanGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.DateGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.HtmlGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.MicronodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NumberGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.StringGraphFieldListImpl;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.relationship.GraphRelationships;
import com.gentics.mesh.core.data.root.impl.GroupRootImpl;
import com.gentics.mesh.core.data.root.impl.LanguageRootImpl;
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;
import com.gentics.mesh.core.data.root.impl.MicroschemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.NodeRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectMicroschemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectSchemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.ReleaseRootImpl;
import com.gentics.mesh.core.data.root.impl.RoleRootImpl;
import com.gentics.mesh.core.data.root.impl.SchemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;
import com.gentics.mesh.core.data.root.impl.TagRootImpl;
import com.gentics.mesh.core.data.root.impl.UserRootImpl;
import com.gentics.mesh.core.data.schema.impl.AddFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.FieldTypeChangeImpl;
import com.gentics.mesh.core.data.schema.impl.RemoveFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateMicroschemaChangeImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateSchemaChangeImpl;
import com.gentics.mesh.core.data.search.impl.SearchQueueBatchImpl;
import com.gentics.mesh.core.data.search.impl.SearchQueueEntryImpl;
import com.gentics.mesh.core.data.search.impl.SearchQueueImpl;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Utility class that will handle index creation and database migration.
 */
public class DatabaseHelper {

	private static final Logger log = LoggerFactory.getLogger(DatabaseHelper.class);

	protected Database database;

	public DatabaseHelper(Database database) {
		this.database = database;
	}

	/**
	 * Initialize the database indices and types.
	 */
	public void init() {

		log.info("Creating database indices. This may take a few seconds...");

		// Base type for most vertices
		MeshVertexImpl.checkIndices(database);

		// Edges
		GraphRelationships.checkIndices(database);
		GraphPermission.checkIndices(database);

		// Aggregation nodes
		MeshRootImpl.checkIndices(database);
		GroupRootImpl.checkIndices(database);
		UserRootImpl.checkIndices(database);
		RoleRootImpl.checkIndices(database);
		TagRootImpl.checkIndices(database);
		NodeRootImpl.checkIndices(database);
		TagFamilyRootImpl.checkIndices(database);
		LanguageRootImpl.checkIndices(database);
		ProjectRootImpl.checkIndices(database);
		SchemaContainerRootImpl.checkIndices(database);
		MicroschemaContainerRootImpl.checkIndices(database);
		ProjectSchemaContainerRootImpl.checkIndices(database);
		ProjectMicroschemaContainerRootImpl.checkIndices(database);
		ReleaseRootImpl.init(database);

		// Nodes
		SearchQueueImpl.checkIndices(database);
		SearchQueueBatchImpl.checkIndices(database);
		SearchQueueEntryImpl.checkIndices(database);
		ProjectImpl.checkIndices(database);
		ReleaseImpl.init(database);

		// Fields
		NodeGraphFieldContainerImpl.checkIndices(database);
		StringGraphFieldListImpl.checkIndices(database);
		BooleanGraphFieldListImpl.checkIndices(database);
		DateGraphFieldListImpl.checkIndices(database);
		NumberGraphFieldListImpl.checkIndices(database);
		HtmlGraphFieldListImpl.checkIndices(database);
		NodeGraphFieldListImpl.checkIndices(database);
		TagGraphFieldContainerImpl.checkIndices(database);
		MicronodeGraphFieldListImpl.checkIndices(database);

		LanguageImpl.checkIndices(database);
		GroupImpl.checkIndices(database);
		RoleImpl.checkIndices(database);
		UserImpl.checkIndices(database);
		NodeImpl.checkIndices(database);
		MicronodeImpl.checkIndices(database);
		TagImpl.checkIndices(database);
		TagFamilyImpl.checkIndices(database);
		SchemaContainerImpl.checkIndices(database);
		MicroschemaContainerImpl.checkIndices(database);
		SchemaContainerVersionImpl.checkIndices(database);
		MicroschemaContainerVersionImpl.checkIndices(database);

		// Field changes
		FieldTypeChangeImpl.checkIndices(database);
		UpdateSchemaChangeImpl.checkIndices(database);
		RemoveFieldChangeImpl.checkIndices(database);
		UpdateFieldChangeImpl.checkIndices(database);
		AddFieldChangeImpl.checkIndices(database);
		UpdateMicroschemaChangeImpl.checkIndices(database);

	}

}
