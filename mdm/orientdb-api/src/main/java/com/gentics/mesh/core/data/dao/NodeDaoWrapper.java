package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;

/**
 * DAO to access {@link HibNode}.
 */
public interface NodeDaoWrapper extends NodeDao, OrientDBRootDao<HibProject, HibNode> {

}
