package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_EDITOR;
import static com.gentics.mesh.util.VerticleHelper.getUser;

import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.rest.common.AbstractGenericNodeRestModel;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.BlueprintTransaction;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;

import io.vertx.ext.web.RoutingContext;

public abstract class AbstractGenericVertex<T extends RestModel> extends MeshVertexImpl implements GenericVertex<T> {

	private static final String CREATION_TIMESTAMP_PROPERTY_KEY = "creation_timestamp";
	private static final String LAST_EDIT_TIMESTAMP_PROPERTY_KEY = "last_edited_timestamp";

	@Override
	public User getCreator() {
		return out(HAS_CREATOR).has(UserImpl.class).nextOrDefault(UserImpl.class, null);
	}

	@Override
	public void setCreator(User user) {
		setLinkOutTo(user.getImpl(), HAS_CREATOR);
	}

	@Override
	public Long getCreationTimestamp() {
		return getProperty(CREATION_TIMESTAMP_PROPERTY_KEY);
	}

	@Override
	public void setCreationTimestamp(long timestamp) {
		setProperty(CREATION_TIMESTAMP_PROPERTY_KEY, timestamp);
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
			// TODO throw error and log something
		}

		User editor = getEditor();
		if (editor != null) {
			model.setEditor(editor.transformToUserReference());
		} else {
			// TODO throw error and log something
		}
		model.setPermissions(getUser(rc).getPermissionNames(this));

		model.setEdited(getLastEditedTimestamp() == null ? 0 : getLastEditedTimestamp());
		model.setCreated(getCreationTimestamp() == null ? 0 : getCreationTimestamp());
	}

	@Override
	public void setEditor(User user) {
		setLinkOutTo(user.getImpl(), HAS_EDITOR);
	}

	@Override
	public void setLastEditedTimestamp(long timestamp) {
		setProperty(LAST_EDIT_TIMESTAMP_PROPERTY_KEY, timestamp);
	}

	@Override
	public Long getLastEditedTimestamp() {
		return getProperty(LAST_EDIT_TIMESTAMP_PROPERTY_KEY);
	}

}
