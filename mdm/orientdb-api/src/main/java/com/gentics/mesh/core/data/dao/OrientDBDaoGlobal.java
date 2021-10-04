package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * An extension to the {@link DaoGlobal} for the OrientDB-based implementations.
 * 
 * @author plyhun
 *
 * @param <T>
 */
public interface OrientDBDaoGlobal<T extends HibCoreElement<? extends RestModel>> extends DaoGlobal<T> {
	
	/**
	 * Since OrientDB does not tell apart POJOs and persistent entities, 
	 * processing the entity updates directly into the persistent state, 
	 * the merge implementation here is empty.
	 */
	@Override
	default T mergeIntoPersisted(T element) {
		return element;
	}
}
