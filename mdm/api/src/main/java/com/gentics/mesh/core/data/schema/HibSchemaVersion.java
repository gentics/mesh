package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.ElementType.SCHEMAVERSION;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_UPDATED;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.event.branch.AbstractBranchAssignEventModel;
import com.gentics.mesh.core.rest.event.branch.BranchSchemaAssignEventModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.etc.config.ContentConfig;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

/**
 * Each schema update is stored within a dedicated schema container version in order to be able to keep track of changes in between different schema container
 * versions.
 */
public interface HibSchemaVersion extends HibFieldSchemaVersionElement<SchemaResponse, SchemaVersionModel, SchemaReference, HibSchema, HibSchemaVersion> {

	static final TypeInfo TYPE_INFO = new TypeInfo(SCHEMAVERSION, SCHEMA_CREATED, SCHEMA_UPDATED, SCHEMA_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Get container entity bound to this version.
	 */
	HibSchema getSchemaContainer();

	/**
	 * Bind container entity to this version.
	 */
	void setSchemaContainer(HibSchema container);

	/**
	 * Return jobs which reference the schema version.
	 * 
	 * @return
	 */
	Iterable<? extends HibJob> referencedJobsViaTo();

	/**
	 * Check the autopurge flag of the version.
	 * 
	 * @return
	 */
	default boolean isAutoPurgeEnabled() {
		Boolean schemaAutoPurge = getSchema().getAutoPurge();
		if (schemaAutoPurge == null) {
			if (log.isDebugEnabled()) {
				log.debug("No schema auto purge flag set. Falling back to mesh global setting");
			}
			ContentConfig contentOptions = Tx.get().data().options().getContentOptions();
			if (contentOptions != null) {
				return contentOptions.isAutoPurge();
			} else {
				return true;
			}
		} else {
			return schemaAutoPurge;
		}
	}

	/**
	 * Transform the version to a reference POJO.
	 * 
	 * @return
	 */
	default SchemaReference transformToReference() {
		SchemaReferenceImpl reference = new SchemaReferenceImpl();
		reference.setName(getName());
		reference.setUuid(getSchemaContainer().getUuid());
		reference.setVersion(getVersion());
		reference.setVersionUuid(getUuid());
		return reference;
	}

	@Override
	default SchemaVersionModel getSchema() {
		ServerSchemaStorage serverSchemaStorage = Tx.get().data().serverSchemaStorage();
		SchemaVersionModel schema = serverSchemaStorage.getSchema(getName(), getVersion());
		if (schema == null) {
			schema = JsonUtil.readValue(getJson(), SchemaModelImpl.class);
			serverSchemaStorage.addSchema(schema);
		}
		return schema;
	}

	@Override
	default SchemaResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		// Load the schema and add/overwrite some properties
		// Use getSchema to utilize the schema storage
		SchemaResponse restSchema = JsonUtil.readValue(getJson(), SchemaResponse.class);
		HibSchema container = getSchemaContainer();
		container.fillCommonRestFields(ac, fields, restSchema);
		restSchema.setRolePerms(Tx.get().roleDao().getRolePermissions(container, ac, ac.getRolePermissionParameters().getRoleUuid()));
		return restSchema;
	}

	@Override
	default void setSchema(SchemaVersionModel schema) {
		ServerSchemaStorage serverSchemaStorage = Tx.get().data().serverSchemaStorage();
		serverSchemaStorage.removeSchema(schema.getName(), schema.getVersion());
		serverSchemaStorage.addSchema(schema);
		String json = schema.toJson();
		setJson(json);
		setVersion(schema.getVersion());
	}

	@Override
	default String getSubETag(InternalActionContext ac) {
		return "";
	}

	@Override
	default String getAPIPath(InternalActionContext ac) {
		return null;
	}

	@Override
	default MeshEvent getBranchAssignEvent() {
		return MeshEvent.SCHEMA_BRANCH_ASSIGN;
	}

	@Override
	default MeshEvent getBranchUnassignEvent() {
		return MeshEvent.SCHEMA_BRANCH_UNASSIGN;
	}

	@Override
	default Class<? extends AbstractBranchAssignEventModel<SchemaReference>> getBranchAssignEventModelClass() {
		return BranchSchemaAssignEventModel.class;
	}
}
