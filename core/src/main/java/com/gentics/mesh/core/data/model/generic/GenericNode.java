package com.gentics.mesh.core.data.model.generic;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_CREATOR;

import java.util.List;

import com.gentics.mesh.core.data.model.MeshUser;
import com.gentics.mesh.core.data.model.Project;

public class GenericNode extends MeshVertex {

	public List<? extends Project> getProjects() {
		return out(ASSIGNED_TO_PROJECT).has(Project.class).toListExplicit(Project.class);
	}

	public void addProject(Project project) {
		addFramedEdge(ASSIGNED_TO_PROJECT, project);
	}

	public void removeProject(Project project) {
		unlinkOut(project, ASSIGNED_TO_PROJECT);
	}

	public MeshUser getCreator() {
		return out(HAS_CREATOR).nextOrDefault(MeshUser.class, null);
	}

	public void setCreator(MeshUser user) {
		linkOut(user, HAS_CREATOR);
	}

}
