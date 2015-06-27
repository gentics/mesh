package com.gentics.mesh.core.data.model.generic;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_CREATOR;

import java.util.List;

import com.gentics.mesh.core.data.model.GenericNode;
import com.gentics.mesh.core.data.model.MeshUser;
import com.gentics.mesh.core.data.model.Project;
import com.gentics.mesh.core.data.model.impl.MeshUserImpl;
import com.gentics.mesh.core.data.model.impl.ProjectImpl;

public abstract class AbstractGenericNode extends MeshVertexImpl implements GenericNode {

	@Override
	public List<? extends ProjectImpl> getProjects() {
		return out(ASSIGNED_TO_PROJECT).has(ProjectImpl.class).toListExplicit(ProjectImpl.class);
	}

	@Override
	public void addProject(Project project) {
		addFramedEdge(ASSIGNED_TO_PROJECT, project.getImpl());
	}

	@Override
	public void removeProject(Project project) {
		unlinkOut(project.getImpl(), ASSIGNED_TO_PROJECT);
	}

	@Override
	public MeshUser getCreator() {
		return out(HAS_CREATOR).nextOrDefault(MeshUserImpl.class, null);
	}

	@Override
	public void setCreator(MeshUser user) {
		linkOut((MeshUserImpl) user, HAS_CREATOR);
	}

}
