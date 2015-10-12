package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_EDITOR;

import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.rest.common.AbstractGenericNodeRestModel;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AbstractGenericVertex<T extends RestModel> extends MeshVertexImpl implements GenericVertex<T> {

	private static final Logger log = LoggerFactory.getLogger(AbstractGenericVertex.class);

	private static final String CREATION_TIMESTAMP_PROPERTY_KEY = "creation_timestamp";
	private static final String LAST_EDIT_TIMESTAMP_PROPERTY_KEY = "last_edited_timestamp";

	@Override
	public User getCreator() {
		return out(HAS_CREATOR).has(UserImpl.class).nextOrDefault(UserImpl.class, null);
	}

	@Override
	public void setCreator(User user) {
		setLinkOut(user.getImpl(), HAS_CREATOR);
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

	/**
	 * Add common fields to the given rest model object. The method will add common files like creator, editor, uuid, permissions, edited, created.
	 * 
	 * @param model
	 * @param ac
	 * @param handler
	 */
	protected <R extends AbstractGenericNodeRestModel> void fillRest(R model, InternalActionContext ac, Handler<AsyncResult<R>> handler) {

		model.setUuid(getUuid());

		User creator = getCreator();
		if (creator != null) {
			creator.transformToUserReference(rh -> {
				model.setCreator(rh.result());
			});
		} else {
			log.error("The object has no creator. Omitting creator field");
			// TODO throw error and log something
		}

		User editor = getEditor();
		if (editor != null) {
			editor.transformToUserReference(rh -> {
				model.setEditor(rh.result());
			});
		} else {
			// TODO throw error and log something
		}
		model.setEdited(getLastEditedTimestamp() == null ? 0 : getLastEditedTimestamp());
		model.setCreated(getCreationTimestamp() == null ? 0 : getCreationTimestamp());

		ac.getUser().getPermissionNames(ac, this, ph -> {
			if (ph.failed()) {
				handler.handle(Future.failedFuture(ph.cause()));
			} else {
				String[] names = ph.result().toArray(new String[ph.result().size()]);
				model.setPermissions(names);
				handler.handle(Future.succeededFuture(model));
			}
		});

	}

	@Override
	public void setEditor(User user) {
		setLinkOut(user.getImpl(), HAS_EDITOR);
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
