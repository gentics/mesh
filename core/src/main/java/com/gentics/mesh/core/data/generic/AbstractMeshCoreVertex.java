package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashSet;
import java.util.Set;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
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
import com.gentics.mesh.dagger.MeshCore;
import com.gentics.mesh.util.DateUtils;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Completable;

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
	protected <R extends AbstractGenericRestResponse> Completable setRolePermissions(InternalActionContext ac, R model) {
		return Completable.defer(() -> {
			String roleUuid = ac.getRolePermissionParameters().getRoleUuid();
			if (isEmpty(roleUuid)) {
				return Completable.complete();
			} else
				return MeshRootImpl.getInstance().getRoleRoot().loadObjectByUuid(ac, roleUuid, READ_PERM).map(role -> {
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
				}).toCompletable();
		});

	}

	/**
	 * Add common fields to the given rest model object. The method will add common files like creator, editor, uuid, permissions, edited, created.
	 * 
	 * @param model
	 * @param ac
	 */
	protected <R extends AbstractGenericRestResponse> Completable fillCommonRestFields(InternalActionContext ac, R model) {
		return Completable.defer(() -> {

			model.setUuid(getUuid());

			if (this instanceof EditorTrackingVertex) {
				EditorTrackingVertex edited = (EditorTrackingVertex) this;

				User editor = edited.getEditor();
				if (editor != null) {
					model.setEditor(editor.transformToReference());
				} else {
					// TODO throw error and log something
				}

				// Convert unixtime to iso-8601
				String date = DateUtils.toISO8601(edited.getLastEditedTimestamp(), 0);
				model.setEdited(date);
			}

			if (this instanceof CreatorTrackingVertex) {
				CreatorTrackingVertex created = (CreatorTrackingVertex) this;
				User creator = created.getCreator();
				if (creator != null) {
					model.setCreator(creator.transformToReference());
				} else {
					log.error("The object {" + getClass().getSimpleName() + "} with uuid {" + getUuid() + "} has no creator. Omitting creator field");
					// TODO throw error and log something
				}

				String date = DateUtils.toISO8601(created.getCreationTimestamp(), 0);
				model.setCreated(date);
			}

			if (ac instanceof NodeMigrationActionContextImpl) {
				// when this is a node migration, do not set user permissions
				return Completable.complete();
			} else {
				return ac.getUser().getPermissionNamesAsync(ac, this).doOnSuccess(list -> {
					String[] names = list.toArray(new String[list.size()]);
					model.setPermissions(names);
				}).toCompletable();
			}
		});
	}

	@Override
	public SearchQueueBatch createIndexBatch(SearchQueueEntryAction action) {
		SearchQueue queue = MeshCore.get().boot().meshRoot().getSearchQueue();
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
