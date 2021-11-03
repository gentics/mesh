package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * A persisting extension to {@link ProjectDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingProjectDao extends ProjectDao, PersistingDaoGlobal<HibProject> {

	@Override
	default HibProject findByName(InternalActionContext ac, String projectName, InternalPermission perm) {
		HibProject project = findByName(projectName);
		return checkPerms(project, project.getUuid(), ac, perm, true);
	}

	@Override
	default HibProject create(String projectName, String hostname, Boolean ssl, String pathPrefix, HibUser creator,
		HibSchemaVersion schemaVersion, EventQueueBatch batch) {
		return create(projectName, hostname, ssl, pathPrefix, creator, schemaVersion, null, batch);
	}
}
