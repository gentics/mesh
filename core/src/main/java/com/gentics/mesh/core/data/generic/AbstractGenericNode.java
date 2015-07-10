package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_EDITOR;

import java.util.List;

import com.gentics.mesh.core.data.GenericNode;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.ProjectNode;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.rest.common.AbstractGenericNodeRestModel;
import com.gentics.mesh.core.rest.user.UserReference;

public abstract class AbstractGenericNode extends MeshVertexImpl implements GenericNode, ProjectNode {

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

	protected void fillRest(AbstractGenericNodeRestModel model, MeshAuthUser requestUser) {
		model.setUuid(getUuid());

		User creator = getCreator();
		if (creator != null) {
			model.setCreator(creator.transformToUserReference());
		} else {
			//TODO throw error and log something
		}

		User editor = getEditor();
		if (editor != null) {
			model.setEditor(editor.transformToUserReference());
		} else {
			//TODO throw error and log something
		}
		model.setPermissions(requestUser.getPermissionNames(this));
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
