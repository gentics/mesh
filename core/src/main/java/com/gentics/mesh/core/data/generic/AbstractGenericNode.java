package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_EDITOR;

import java.util.List;

import com.gentics.mesh.core.data.GenericNode;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;

public abstract class AbstractGenericNode extends AbstractMeshVertex implements GenericNode {

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
	public User getCreator() {
		return out(HAS_CREATOR).has(UserImpl.class).nextOrDefault(UserImpl.class, null);
	}

	@Override
	public void setCreator(User user) {
		outE(HAS_CREATOR).removeAll();
		linkOut(user.getImpl(), HAS_CREATOR);
	}

	@Override
	public Long getCreationTimestamp() {
		return getProperty("creation_timestamp");
	}

	@Override
	public void setCreationTimestamp(long timestamp) {
		setProperty("creation_timestamp", timestamp);
	}

	@Override
	public User getEditor() {
		return out(HAS_EDITOR).has(UserImpl.class).nextOrDefaultExplicit(UserImpl.class, null);
	}

	@Override
	public void setEditor(User user) {
		//TODO replace with setlinkout
		outE(HAS_EDITOR).removeAll();
		linkOut(user.getImpl(), HAS_EDITOR);
	}

	@Override
	public void setLastEditedTimestamp(long timestamp) {
		setProperty("last_edited_timestamp", timestamp);
	}

	@Override
	public Long getLastEditedTimestamp() {
		return getProperty("last_edited_timestamp");
	}

}
