package com.gentics.mesh.core.data.impl;

import com.gentics.mesh.core.data.binary.impl.BinaryImpl;
import com.gentics.mesh.core.data.binary.impl.BinaryRootImpl;
import com.gentics.mesh.core.data.branch.impl.BranchMicroschemaEdgeImpl;
import com.gentics.mesh.core.data.branch.impl.BranchSchemaEdgeImpl;
import com.gentics.mesh.core.data.changelog.ChangeMarkerVertexImpl;
import com.gentics.mesh.core.data.changelog.ChangelogRootImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.generic.AbstractGenericFieldContainerVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.job.impl.BranchMigrationJobImpl;
import com.gentics.mesh.core.data.job.impl.JobRootImpl;
import com.gentics.mesh.core.data.job.impl.MicronodeMigrationJobImpl;
import com.gentics.mesh.core.data.job.impl.NodeMigrationJobImpl;
import com.gentics.mesh.core.data.node.field.impl.BinaryGraphFieldImpl;
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
import com.gentics.mesh.core.data.root.impl.BranchRootImpl;
import com.gentics.mesh.core.data.root.impl.GroupRootImpl;
import com.gentics.mesh.core.data.root.impl.LanguageRootImpl;
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;
import com.gentics.mesh.core.data.root.impl.MicroschemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.NodeRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectMicroschemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectSchemaContainerRootImpl;
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
import com.gentics.mesh.graphdb.spi.LegacyDatabase;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Utility class that will handle index creation and database migration.
 */
public final class DatabaseHelper {

	private static final Logger log = LoggerFactory.getLogger(DatabaseHelper.class);

	/**
	 * Initialize the database indices and types.
	 * 
	 * @param database
	 */
	public static void init(LegacyDatabase database) {

		log.info("Creating database indices. This may take a few seconds...");

		// Base type for most vertices
		MeshVertexImpl.init(database);

		// Edges
		GraphRelationships.init(database);
		GraphPermission.init(database);
		GraphFieldContainerEdgeImpl.init(database);
		MicronodeGraphFieldImpl.init(database);
		TagEdgeImpl.init(database);
		BranchSchemaEdgeImpl.init(database);
		BranchMicroschemaEdgeImpl.init(database);

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
		BranchRootImpl.init(database);
		JobRootImpl.init(database);
		ChangelogRootImpl.init(database);

		// Binary
		BinaryImpl.init(database);
		BinaryRootImpl.init(database);

		// Nodes
		ProjectImpl.init(database);
		BranchImpl.init(database);

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
		BinaryGraphFieldImpl.init(database);

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

		// Jobs
		NodeMigrationJobImpl.init(database);
		MicronodeMigrationJobImpl.init(database);
		BranchMigrationJobImpl.init(database);

		// Field changes
		FieldTypeChangeImpl.init(database);
		UpdateSchemaChangeImpl.init(database);
		RemoveFieldChangeImpl.init(database);
		UpdateFieldChangeImpl.init(database);
		AddFieldChangeImpl.init(database);
		UpdateMicroschemaChangeImpl.init(database);

		// Changelog
		ChangeMarkerVertexImpl.init(database);

	}

}
