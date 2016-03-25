package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashSet;
import java.util.Set;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.CreatorTrackingVertex;
import com.gentics.mesh.core.data.EditorTrackingVertex;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.handler.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

public abstract class AbstractMeshCoreVertex<T extends RestModel, R extends MeshCoreVertex<T, R>> extends MeshVertexImpl
		implements MeshCoreVertex<T, R> {

	private static final Logger log = LoggerFactory.getLogger(AbstractMeshCoreVertex.class);

	/**
	 * Add role permissions to given rest model object.
	 * 
	 * @param ac
	 * @param model
	 * @return
	 */
	protected <R extends AbstractGenericRestResponse> Observable<R> setRolePermissions(InternalActionContext ac, R model) {
		String rolePermissionParameter = ac.getRolePermissionParameter();
		if (isEmpty(rolePermissionParameter)) {
			return Observable.empty();
		} else
			return MeshRootImpl.getInstance().getRoleRoot().loadObjectByUuid(ac, rolePermissionParameter, READ_PERM).map(role -> {
				if (role != null) {
					Set<GraphPermission> permSet = role.getPermissions(this);
					Set<String> humanNames = new HashSet<>();
					for (GraphPermission permission : permSet) {
						humanNames.add(permission.getSimpleName());
					}
					String[] names = humanNames.toArray(new String[humanNames.size()]);
					model.setRolePerms(names);
				}
				return model;
			});

	}

	/**
	 * Add common fields to the given rest model object. The method will add common files like creator, editor, uuid, permissions, edited, created.
	 * 
	 * @param model
	 * @param ac
	 */
	protected <R extends AbstractGenericRestResponse> Observable<R> fillCommonRestFields(InternalActionContext ac, R model) {

		model.setUuid(getUuid());

		if (this instanceof EditorTrackingVertex) {
			EditorTrackingVertex edited = (EditorTrackingVertex)this;

			User editor = edited.getEditor();
			if (editor != null) {
				model.setEditor(editor.transformToReference());
			} else {
				// TODO throw error and log something
			}
			model.setEdited(edited.getLastEditedTimestamp() == null ? 0 : edited.getLastEditedTimestamp());
		}

		if (this instanceof CreatorTrackingVertex) {
			CreatorTrackingVertex created = (CreatorTrackingVertex)this;
			User creator = created.getCreator();
			if (creator != null) {
				model.setCreator(creator.transformToReference());
			} else {
				log.error("The object has no creator. Omitting creator field");
				// TODO throw error and log something
			}
			
			model.setCreated(created.getCreationTimestamp() == null ? 0 : created.getCreationTimestamp());
		}

		if (ac instanceof NodeMigrationActionContextImpl) {
			// when this is a node migration, do not set user permissions
			return Observable.just(model);
		} else {
			return ac.getUser().getPermissionNamesAsync(ac, this).map(list -> {
				String[] names = list.toArray(new String[list.size()]);
				model.setPermissions(names);
				return model;
			});
		}
	}

	@Override
	public SearchQueueBatch addIndexBatch(SearchQueueEntryAction action) {
		SearchQueue queue = BootstrapInitializer.getBoot().meshRoot().getSearchQueue();
		SearchQueueBatch batch = queue.createBatch(UUIDUtil.randomUUID());
		batch.addEntry(this, action);
		addRelatedEntries(batch, action);
		return batch;
	}

	/**
	 * Compare both string values in order to determine whether the graph value should be updated.
	 * 
	 * @param restValue
	 * @param graphValue
	 * @return true if restValue is not empty or null and the restValue is not equal to the graph value. Otherwise false.
	 */
	protected boolean shouldUpdate(String restValue, String graphValue) {
		return !isEmpty(restValue) && !restValue.equals(graphValue);
	}

}
