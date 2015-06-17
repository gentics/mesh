package com.gentics.mesh.core.data.model.generic;

import java.util.List;

import com.gentics.mesh.core.data.model.relationship.MeshRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.Project;
import com.gentics.mesh.core.data.model.tinkerpop.MeshUser;

public class GenericNode extends MeshVertex {

	public List<? extends Project> getProjects() {
		return out(MeshRelationships.ASSIGNED_TO_PROJECT).toList(Project.class);
	}

	public void addProject(Project project) {
		addFramedEdge(MeshRelationships.ASSIGNED_TO_PROJECT, project);
	}

	public void removeProject(Project project) {
		unlinkOut(project, MeshRelationships.ASSIGNED_TO_PROJECT);
	}

	public MeshUser getCreator() {
		return out(MeshRelationships.HAS_CREATOR).next(MeshUser.class);
	}

	public void setCreator(MeshUser user) {
		linkOut(user, MeshRelationships.HAS_CREATOR);
	}

}
