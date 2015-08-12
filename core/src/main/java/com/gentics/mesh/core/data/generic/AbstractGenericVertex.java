package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_EDITOR;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import io.vertx.ext.web.RoutingContext;

import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.rest.common.AbstractGenericNodeRestModel;
import com.gentics.mesh.core.rest.common.RestModel;
public abstract class AbstractGenericVertex<T extends RestModel> extends MeshVertexImpl implements GenericVertex<T> {


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

	protected void fillRest(AbstractGenericNodeRestModel model, RoutingContext rc) {
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
		model.setPermissions(getUser(rc).getPermissionNames(this));

		model.setEdited(getLastEditedTimestamp() == null ? 0 : getLastEditedTimestamp());
		model.setCreated(getCreationTimestamp() == null ? 0 : getCreationTimestamp());

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
