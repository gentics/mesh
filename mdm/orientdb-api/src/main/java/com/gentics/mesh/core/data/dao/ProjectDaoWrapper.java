package com.gentics.mesh.core.data.dao;

import java.util.function.Predicate;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * Temporary in-between interface that helps test removal of project root deps.
 */
// TODO move the contents of this to ProjectDao once migration is done
public interface ProjectDaoWrapper extends ProjectDao, DaoWrapper<Project>, DaoTransformable<Project, ProjectResponse> {

	TraversalResult<? extends Project> findAll();

	TransformablePage<? extends Project> findAll(InternalActionContext ac, PagingParameters pagingInfo);

	Page<? extends Project> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<Project> extraFilter);

	Project findByName(String name);

	Project findByName(InternalActionContext ac, String projectName, GraphPermission perm);

	Project findByUuid(String uuid);

	void delete(Project project, BulkActionContext bc);

	boolean update(Project element, InternalActionContext ac, EventQueueBatch batch);

	Project loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm);

	Project loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound);

	Project create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	default Project create(String projectName, String hostname, Boolean ssl, String pathPrefix, HibUser creator,
		SchemaVersion schemaVersion, EventQueueBatch batch) {
		return create(projectName, hostname, ssl, pathPrefix, creator, schemaVersion, null, batch);
	}

	Project create(String name, String hostname, Boolean ssl, String pathPrefix, HibUser creator, SchemaVersion schemaVersion,
		String uuid, EventQueueBatch batch);

}
