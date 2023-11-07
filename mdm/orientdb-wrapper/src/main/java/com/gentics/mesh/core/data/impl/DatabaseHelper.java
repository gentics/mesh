package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.binary.impl.BinaryGraphFieldVariantImpl;
import com.gentics.mesh.core.data.binary.impl.BinaryImpl;
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
import com.gentics.mesh.core.data.job.impl.VersionPurgeJobImpl;
import com.gentics.mesh.core.data.node.field.impl.BinaryGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.MicronodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.S3BinaryGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.impl.BooleanGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.DateGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.HtmlGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.MicronodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NumberGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.StringGraphFieldListImpl;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
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
import com.gentics.mesh.core.data.s3binary.impl.S3BinaryImpl;
import com.gentics.mesh.core.data.schema.impl.AddFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.FieldTypeChangeImpl;
import com.gentics.mesh.core.data.schema.impl.RemoveFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateMicroschemaChangeImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateSchemaChangeImpl;
import com.gentics.mesh.graphdb.spi.GraphDatabase;

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
	public static void init(GraphDatabase database) {
		log.info("Creating database indices. This may take a few seconds...");
		TypeHandler type = database.type();
		IndexHandler index = database.index();

		// Base type for most vertices
		MeshVertexImpl.init(type, index);

		// Edges
		GraphRelationships.init(type, index);
		initPermissions(type, index);
		GraphFieldContainerEdgeImpl.init(type, index);
		MicronodeGraphFieldImpl.init(type, index);
		TagEdgeImpl.init(type, index);
		BranchSchemaEdgeImpl.init(type, index);
		BranchMicroschemaEdgeImpl.init(type, index);

		// Aggregation nodes
		MeshRootImpl.init(type, index);
		GroupRootImpl.init(type, index);
		UserRootImpl.init(type, index);
		RoleRootImpl.init(type, index);
		TagRootImpl.init(type, index);
		NodeRootImpl.init(type, index);
		TagFamilyRootImpl.init(type, index);
		LanguageRootImpl.init(type, index);
		ProjectRootImpl.init(type, index);
		SchemaContainerRootImpl.init(type, index);
		MicroschemaContainerRootImpl.init(type, index);
		ProjectSchemaContainerRootImpl.init(type, index);
		ProjectMicroschemaContainerRootImpl.init(type, index);
		BranchRootImpl.init(type, index);
		JobRootImpl.init(type, index);
		ChangelogRootImpl.init(type, index);

		// Binary
		BinaryImpl.init(type, index);
		S3BinaryImpl.init(type, index);

		// Nodes
		ProjectImpl.init(type, index);
		BranchImpl.init(type, index);

		// Fields
		AbstractGenericFieldContainerVertex.init(type, index);
		NodeGraphFieldContainerImpl.init(type, index);
		StringGraphFieldListImpl.init(type, index);
		BooleanGraphFieldListImpl.init(type, index);
		DateGraphFieldListImpl.init(type, index);
		NumberGraphFieldListImpl.init(type, index);
		HtmlGraphFieldListImpl.init(type, index);
		NodeGraphFieldListImpl.init(type, index);
		MicronodeGraphFieldListImpl.init(type, index);
		BinaryGraphFieldImpl.init(type, index);
		S3BinaryGraphFieldImpl.init(type, index);

		LanguageImpl.init(type, index);
		GroupImpl.init(type, index);
		RoleImpl.init(type, index);
		UserImpl.init(type, index);
		NodeImpl.init(type, index);
		MicronodeImpl.init(type, index);
		TagImpl.init(type, index);
		TagFamilyImpl.init(type, index);
		SchemaContainerImpl.init(type, index);
		MicroschemaContainerImpl.init(type, index);
		SchemaContainerVersionImpl.init(type, index);
		MicroschemaContainerVersionImpl.init(type, index);

		// Jobs
		NodeMigrationJobImpl.init(type, index);
		VersionPurgeJobImpl.init(type, index);
		MicronodeMigrationJobImpl.init(type, index);
		BranchMigrationJobImpl.init(type, index);

		// Field changes
		FieldTypeChangeImpl.init(type, index);
		UpdateSchemaChangeImpl.init(type, index);
		RemoveFieldChangeImpl.init(type, index);
		UpdateFieldChangeImpl.init(type, index);
		AddFieldChangeImpl.init(type, index);
		UpdateMicroschemaChangeImpl.init(type, index);

		// Changelog
		ChangeMarkerVertexImpl.init(type, index);

		// Binary variant
		BinaryGraphFieldVariantImpl.init(type, index);
	}

	private static void initPermissions(TypeHandler type, IndexHandler index) {
		for (String label : InternalPermission.labels()) {
			type.createType(edgeType(label));
			index.createIndex(edgeIndex(label).withInOut());
		}
	}

}
