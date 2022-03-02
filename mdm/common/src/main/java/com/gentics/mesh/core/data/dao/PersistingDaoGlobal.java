package com.gentics.mesh.core.data.dao;

import java.util.function.BiFunction;

import com.gentics.mesh.core.data.HibBaseElement;

/**
 * A developer extension to {@link DaoGlobal} with low level persistent storage access.
 * 
 * @author plyhun
 *
 * @param <T>
 */
public interface PersistingDaoGlobal<T extends HibBaseElement> extends DaoGlobal<T>, PersistingDao<T>, ElementResolver<HibBaseElement, T> {

	@Override
	default BiFunction<HibBaseElement, String, T> getFinder() {
		return (unused, uuid) -> findByUuid(uuid);
	}
}
