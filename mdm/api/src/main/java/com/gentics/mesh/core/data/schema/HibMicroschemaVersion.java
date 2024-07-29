package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.ElementType.MICROSCHEMAVERSION;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_DELETED;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.branch.AbstractBranchAssignEventModel;
import com.gentics.mesh.core.rest.event.branch.BranchMicroschemaAssignModel;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

/**
 * Domain model for microschema versions.
 */
public interface HibMicroschemaVersion
	extends HibFieldSchemaVersionElement<MicroschemaResponse, MicroschemaVersionModel, MicroschemaReference, HibMicroschema, HibMicroschemaVersion> {

	static final TypeInfo TYPE_INFO = new TypeInfo(MICROSCHEMAVERSION, MICROSCHEMA_CREATED, MICROSCHEMA_UPDATED, SCHEMA_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Transform the version to a reference POJO.
	 *
	 * @return
	 */
	default MicroschemaReference transformToReference() {
		MicroschemaReference reference = new MicroschemaReferenceImpl();
		reference.setName(getName());
		reference.setUuid(getSchemaContainer().getUuid());
		reference.setVersion(getVersion());
		reference.setVersionUuid(getUuid());
		return reference;
	}

	// TODO MDM rename method
	HibMicroschema getSchemaContainer();

	// TODO MDM rename method
	void setSchemaContainer(HibMicroschema container);

	/**
	 * Return the previous schema version
	 * 
	 * @return previous version or null when no previous version exists
	 * 
	 */
	HibMicroschemaVersion getPreviousVersion();

	/**
	 * Return the next schema version.
	 * 
	 * @return next version or null when no next version exists
	 */
	HibMicroschemaVersion getNextVersion();

	/**
	 * Set the next microschema version.
	 * 
	 * @param version
	 */
	void setNextVersion(HibMicroschemaVersion version);

	@Override
	default MicroschemaVersionModel getSchema() {
		MicroschemaVersionModel microschema = Tx.get().data().serverSchemaStorage().getMicroschema(getName(), getVersion());
		if (microschema == null) {
			microschema = JsonUtil.readValue(getJson(), MicroschemaModelImpl.class);
			Tx.get().data().serverSchemaStorage().addMicroschema(microschema);
		}
		return microschema;
	}

	@Override
	default void setSchema(MicroschemaVersionModel microschema) {
		Tx.get().data().serverSchemaStorage().removeMicroschema(microschema.getName(), microschema.getVersion());
		Tx.get().data().serverSchemaStorage().addMicroschema(microschema);
		// TODO FIXME can we rely on a frontend formatter, to allow de-prettifying the schema JSON?
		String json = microschema.toJson(false);
		setJson(json);
		setVersion(microschema.getVersion());
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
		return MeshEvent.MICROSCHEMA_BRANCH_ASSIGN;
	}

	@Override
	default MeshEvent getBranchUnassignEvent() {
		return MeshEvent.MICROSCHEMA_BRANCH_UNASSIGN;
	}

	@Override
	default Class<? extends AbstractBranchAssignEventModel<MicroschemaReference>> getBranchAssignEventModelClass() {
		return BranchMicroschemaAssignModel.class;
	}

	@Override
	default MicroschemaResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();
		RoleDao roleDao = Tx.get().roleDao();

		// Load the microschema and add/overwrite some properties
		MicroschemaResponse microschema = JsonUtil.readValue(getJson(), MicroschemaResponse.class);
		// TODO apply fields filtering here

		// Role permissions
		HibMicroschema container = getSchemaContainer();
		microschema.setRolePerms(roleDao.getRolePermissions(container, ac, ac.getRolePermissionParameters().getRoleUuid()));
		container.fillCommonRestFields(ac, fields, microschema);

		return microschema;
	}
}
