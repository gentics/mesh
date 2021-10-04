package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.project.HibProject;

/**
 * Temporary in-between interface that helps test removal of project root deps.
 */
public interface ProjectDaoWrapper extends ProjectDao, OrientDBDaoGlobal<HibProject> {
}
