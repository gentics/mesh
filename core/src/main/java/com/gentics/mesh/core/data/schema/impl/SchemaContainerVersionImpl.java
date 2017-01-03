package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_VERSION;
import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.impl.ReleaseImpl;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.ETag;
import com.gentics.mesh.util.RestModelHelper;

import rx.Single;

/**
 * @see SchemaContainerVersion
 */
public class SchemaContainerVersionImpl extends
		AbstractGraphFieldSchemaContainerVersion<Schema, SchemaReference, SchemaContainerVersion, SchemaContainer>
		implements SchemaContainerVersion {

	@Override
	public String getType() {
		return SchemaContainer.TYPE;
	}

	public static void init(Database database) {
		database.addVertexType(SchemaContainerVersionImpl.class, MeshVertexImpl.class);
	}

	@Override
	protected Class<? extends SchemaContainerVersion> getContainerVersionClass() {
		return SchemaContainerVersionImpl.class;
	}

	@Override
	protected Class<? extends SchemaContainer> getContainerClass() {
		return SchemaContainerImpl.class;
	}

	@Override
	protected String getMigrationAddress() {
		return NodeMigrationVerticle.SCHEMA_MIGRATION_ADDRESS;
	}

	@Override
	public List<? extends NodeGraphFieldContainer> getFieldContainers(String releaseUuid) {
		return in(HAS_SCHEMA_CONTAINER_VERSION).mark().inE(HAS_FIELD_CONTAINER)
				.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.DRAFT.getCode())
				.has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid).back()
				.toListExplicit(NodeGraphFieldContainerImpl.class);
	}

	@Override
	public Schema getSchema() {
		Schema schema = MeshInternal.get().serverSchemaStorage().getSchema(getName(), getVersion());
		if (schema == null) {
			schema = JsonUtil.readValue(getJson(), SchemaModel.class);
			MeshInternal.get().serverSchemaStorage().addSchema(schema);
		}
		return schema;
	}

	@Override
	public Schema transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		// Load the schema and add/overwrite some properties
		// Use getSchema to utilise the schema storage
		Schema restSchema = JsonUtil.readValue(getJson(), SchemaModel.class);
		restSchema.setUuid(getSchemaContainer().getUuid());

		// TODO Get list of projects to which the schema was assigned
		// for (Project project : getProjects()) {
		// }
		// ProjectResponse restProject = new ProjectResponse();
		// restProje ct.setUuid(project.getUuid());
		// restProject.setName(project.getName());
		// schemaResponse.getProjects().add(restProject);
		// }

		// Sort the list by project name
		// restSchema.getProjects()
		// Collections.sort(restSchema.getProjects(), new
		// Comparator<ProjectResponse>() {
		// @Override
		// public int compare(ProjectResponse o1, ProjectResponse o2) {
		// return o1.getName().compareTo(o2.getName());
		// };
		// });

		// Role permissions
		RestModelHelper.setRolePermissions(ac, getSchemaContainer(), restSchema);

		restSchema.setPermissions(ac.getUser().getPermissionNames(getSchemaContainer()));
		return restSchema;

	}

	@Override
	public void setSchema(Schema schema) {
		MeshInternal.get().serverSchemaStorage().removeSchema(schema.getName(), schema.getVersion());
		MeshInternal.get().serverSchemaStorage().addSchema(schema);
		String json = JsonUtil.toJson(schema);
		setJson(json);
		setProperty(VERSION_PROPERTY_KEY, schema.getVersion());
	}

	@Override
	public SchemaReference transformToReference() {
		SchemaReference reference = new SchemaReference();
		reference.setName(getName());
		reference.setUuid(getSchemaContainer().getUuid());
		reference.setVersion(getVersion());
		return reference;
	}

	@Override
	public String getETag(InternalActionContext ac) {
		return ETag.hash(getUuid());
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return null;
	}

	@Override
	public Release getRelease() {
		return in(HAS_SCHEMA_VERSION).nextOrDefaultExplicit(ReleaseImpl.class, null);
	}

	@Override
	public Single<Schema> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return MeshInternal.get().database().operateNoTx(() -> {
			return Single.just(transformToRestSync(ac, level, languageTags));
		});
	}

}
