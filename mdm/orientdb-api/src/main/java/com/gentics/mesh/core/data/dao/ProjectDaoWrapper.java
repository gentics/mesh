package com.gentics.mesh.core.data.dao;

import java.util.function.Predicate;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * Temporary in-between interface that helps test removal of project root deps.
 */
// TODO move the contents of this to ProjectDao once migration is done
public interface ProjectDaoWrapper extends ProjectDao, DaoWrapper<HibProject>, DaoTransformable<HibProject, ProjectResponse> {

	TraversalResult<? extends HibProject> findAll();

	TransformablePage<? extends HibProject> findAll(InternalActionContext ac, PagingParameters pagingInfo);

	Page<? extends HibProject> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibProject> extraFilter);

	HibProject findByName(String name);

	HibProject findByName(InternalActionContext ac, String projectName, InternalPermission perm);

	HibProject findByUuid(String uuid);

	void delete(HibProject project, BulkActionContext bc);

	boolean update(HibProject element, InternalActionContext ac, EventQueueBatch batch);

	HibProject loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm);

	HibProject loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound);

	HibProject create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	default HibProject create(String projectName, String hostname, Boolean ssl, String pathPrefix, HibUser creator,
		HibSchemaVersion schemaVersion, EventQueueBatch batch) {
		return create(projectName, hostname, ssl, pathPrefix, creator, schemaVersion, null, batch);
	}

	HibProject create(String name, String hostname, Boolean ssl, String pathPrefix, HibUser creator, HibSchemaVersion schemaVersion,
		String uuid, EventQueueBatch batch);

	String getETag(HibProject project, InternalActionContext ac);

}
