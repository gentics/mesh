package com.gentics.mesh.core.data.dao;

import java.util.function.BiFunction;

import com.gentics.mesh.core.data.BaseElement;

/**
 * A developer extension to {@link DaoGlobal} with low level persistent storage access.
 * 
 * @author plyhun
 *
 * @param <T>
 */
public interface PersistingDaoGlobal<T extends BaseElement> extends DaoGlobal<T>, PersistingDao<T>, ElementResolver<BaseElement, T> {

	@Override
	default BiFunction<BaseElement, String, T> getFinder() {
		return (unused, uuid) -> findByUuid(uuid);
	}
}
