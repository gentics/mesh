package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;

/**
 * Temporary in-between interface that helps test removal of project root deps.
 */
// TODO move the contents of this to ProjectDao once migration is done
public interface ProjectDaoWrapper extends ProjectDao, ProjectRoot {

	// Uuid is currently needed to print permission error (e.g. lacking perm on project root)
	String getUuid();

	TraversalResult<? extends Project> findAll();

	Project create(String projectName, String hostname, Boolean ssl, String pathPrefix, User creator, SchemaContainerVersion schemaContainerVersion,
		String uuid, EventQueueBatch batch);

	Project findByName(String name);

	Project findByUuid(String uuid);

	String getSubETag(Project project, InternalActionContext ac);

	String getAPIPath(Project project, InternalActionContext ac);

	void delete(Project project, BulkActionContext bc);

}
