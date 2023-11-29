package com.gentics.mesh.core.data.dao;

import java.util.function.BiFunction;
import java.util.function.Consumer;

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
public interface PersistingRootDao<R extends HibCoreElement<? extends RestModel>, L extends HibCoreElement<? extends RestModel>> extends RootDao<R, L>, ElementResolver<R, L> {

	/**
	 * Get the persistent leaf entity class upon the root entity.
	 * 
	 * @param root
	 * @return
	 */
	Class<? extends L> getPersistenceClass(R root);
	
	/**
	 * Created a persisted entity within the given root. 
	 * 
	 * @param root
	 * @param uuid if null, the generated UUID will be attached to the created element.
	 * @param inflater the instance inflater
	 * @return
	 */
	L createPersisted(R root, String uuid, Consumer<L> inflater);

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
		findAll(root).list().forEach(entity -> delete(root, entity, bac));
	}

	@Override
	default BiFunction<R, String, L> getFinder() {
		return this::findByUuid;
	}
}
