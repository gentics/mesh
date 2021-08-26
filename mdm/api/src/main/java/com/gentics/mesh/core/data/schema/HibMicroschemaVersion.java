package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

/**
 * Domain model for microschema versions.
 */
public interface HibMicroschemaVersion
	extends HibFieldSchemaVersionElement<MicroschemaResponse, MicroschemaVersionModel, HibMicroschema, HibMicroschemaVersion> {

	MicroschemaReference transformToReference();

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
	default String getSubETag(InternalActionContext ac) {
		return "";
	}

	@Override
	default String getAPIPath(InternalActionContext ac) {
		return null;
	}

	@Override
	default MicroschemaResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();
		MicroschemaDao microschemaDao = Tx.get().microschemaDao();

		// Load the microschema and add/overwrite some properties
		MicroschemaResponse microschema = JsonUtil.readValue(getJson(), MicroschemaResponse.class);
		// TODO apply fields filtering here

		// Role permissions
		HibMicroschema container = getSchemaContainer();
		microschema.setRolePerms(microschemaDao.getRolePermissions(container, ac, ac.getRolePermissionParameters().getRoleUuid()));
		container.fillCommonRestFields(ac, fields, microschema);

		return microschema;
	}
}
