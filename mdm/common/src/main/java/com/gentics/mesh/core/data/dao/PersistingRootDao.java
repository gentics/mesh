package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * A sister interface to {@link PersistingDao}, applicable to the entities,
 * that should be manipulated over the root entity. Use {@link Dao}/{@link RootDao} instead of this when possible,
 * since those are higher level APIs.
 * 
 * @author plyhun
 *
 * @param <R> root entity type
 * @param <L> managed(leaf) entity type
 */
public interface PersistingRootDao<R extends HibCoreElement<? extends RestModel>, L extends HibCoreElement<? extends RestModel>> extends RootDao<R, L> {

	/**
	 * Created a persisted entity within the given root. 
	 * 
	 * @param root
	 * @param uuid if null, the generated UUID will be attached to the created element.
	 * @return
	 */
	L createPersisted(R root, String uuid);

	/**
	 * Merge the entity data into its persistent state.
	 * 
	 * @param root
	 * @param entity
	 * @return
	 */
	L mergeIntoPersisted(R root, L entity);

	/**
	 * Delete the entity from the persistent storage.
	 * 
	 * @param root
	 * @param entity
	 */
	void deletePersisted(R root, L entity);

	@Override
	default void onRootDeleted(R root, BulkActionContext bac) {
		findAll(root).forEach(entity -> delete(root, entity, bac));
	}
}
