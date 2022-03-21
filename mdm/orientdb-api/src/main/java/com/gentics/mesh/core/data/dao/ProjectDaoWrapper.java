package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.result.Result;

/**
 * GraphDB wrapper over {@link ProjectDao}.
 */
public interface ProjectDaoWrapper extends PersistingProjectDao {

	@Override
	default HibBaseElement getTagFamilyPermissionRoot(HibProject project) {
		return toGraph(project).getTagFamilyRoot();
	}

	@Override
	default HibBaseElement getSchemaContainerPermissionRoot(HibProject project) {
		return toGraph(project).getSchemaContainerRoot();
	}

	@Override
	default HibBaseElement getMicroschemaContainerPermissionRoot(HibProject project) {
		return toGraph(project).getMicroschemaContainerRoot();
	}

	@Override
	default HibBaseElement getBranchPermissionRoot(HibProject project) {
		return toGraph(project).getBranchRoot();
	}

	@Override
	default HibBaseElement getNodePermissionRoot(HibProject project) {
		return toGraph(project).getNodeRoot();
	}

	@Override
	default Result<? extends HibNode> findNodes(HibProject project) {
		return toGraph(project).findNodes();
	}
}
