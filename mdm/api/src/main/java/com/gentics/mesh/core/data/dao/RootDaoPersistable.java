package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * A sister interface to {@link DaoPersistable}, applicable to the entities,
 * that should be manipulated over the root entity. Use {@link Dao}/{@link RootDao} instead of this when possible,
 * since those are higher level APIs.
 * 
 * @author plyhun
 *
 * @param <R> root entity type
 * @param <L> managed(leaf) entity type
 */
public interface RootDaoPersistable<R extends HibCoreElement<? extends RestModel>, L extends HibCoreElement<? extends RestModel>> {

	/**
	 * Create new uninitialized persisted element within the given root. 
	 * 
	 * @param root
	 * @param uuid if null, the generated UUID will be attached to the created element.
	 * @return
	 */
	L createPersisted(R root, String uuid);
	
	/**
	 * Merge the element data into its persistent state.
	 * 
	 * @param root
	 * @param element
	 * @return
	 */
	L mergeIntoPersisted(R root, L element);
	
	/**
	 * Delete the persistent entity.
	 * 
	 * @param root
	 * @param element
	 */
	void deletePersisted(R root, L element);
}
