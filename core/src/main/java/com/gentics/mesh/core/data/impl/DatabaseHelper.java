package com.gentics.mesh.core.data.impl;

import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.generic.AbstractGenericFieldContainerVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.impl.MicronodeGraphFieldImpl;
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
		MeshVertexImpl.init(database);

		// Edges
		GraphRelationships.init(database);
		GraphPermission.init(database);
		GraphFieldContainerEdgeImpl.init(database);
		MicronodeGraphFieldImpl.init(database);
		TagEdgeImpl.init(database);

		// Aggregation nodes
		MeshRootImpl.init(database);
		GroupRootImpl.init(database);
		UserRootImpl.init(database);
		RoleRootImpl.init(database);
		TagRootImpl.init(database);
		NodeRootImpl.init(database);
		TagFamilyRootImpl.init(database);
		LanguageRootImpl.init(database);
		ProjectRootImpl.init(database);
		SchemaContainerRootImpl.init(database);
		MicroschemaContainerRootImpl.init(database);
		ProjectSchemaContainerRootImpl.init(database);
		ProjectMicroschemaContainerRootImpl.init(database);
		ReleaseRootImpl.init(database);

		// Nodes
		SearchQueueImpl.init(database);
		SearchQueueBatchImpl.init(database);
		SearchQueueEntryImpl.init(database);
		ProjectImpl.init(database);
		ReleaseImpl.init(database);

		// Fields
		AbstractGenericFieldContainerVertex.init(database);
		NodeGraphFieldContainerImpl.init(database);
		StringGraphFieldListImpl.init(database);
		BooleanGraphFieldListImpl.init(database);
		DateGraphFieldListImpl.init(database);
		NumberGraphFieldListImpl.init(database);
		HtmlGraphFieldListImpl.init(database);
		NodeGraphFieldListImpl.init(database);
		MicronodeGraphFieldListImpl.init(database);

		LanguageImpl.init(database);
		GroupImpl.init(database);
		RoleImpl.init(database);
		UserImpl.init(database);
		NodeImpl.init(database);
		MicronodeImpl.init(database);
		TagImpl.init(database);
		TagFamilyImpl.init(database);
		SchemaContainerImpl.init(database);
		MicroschemaContainerImpl.init(database);
		SchemaContainerVersionImpl.init(database);
		MicroschemaContainerVersionImpl.init(database);

		// Field changes
		FieldTypeChangeImpl.init(database);
		UpdateSchemaChangeImpl.init(database);
		RemoveFieldChangeImpl.init(database);
		UpdateFieldChangeImpl.init(database);
		AddFieldChangeImpl.init(database);
		UpdateMicroschemaChangeImpl.init(database);

	}

}
