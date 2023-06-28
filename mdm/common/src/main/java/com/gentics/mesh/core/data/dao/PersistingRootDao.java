package com.gentics.mesh.core.data.dao;

import java.util.Map;
import java.util.function.BiFunction;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.rest.SortOrder;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.SortingParameters;

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
		findAll(root).list().forEach(entity -> delete(root, entity, bac));
	}

	@Override
	default BiFunction<R, String, L> getFinder() {
		return this::findByUuid;
	}

	/**
	 * Check if the sort params request sorting. Used for choosing of picking the sort-enabled or unsorted (performant) data fetcher.
	 * 
	 * @param sortBy
	 * @param sortOrder
	 * @return
	 */
	static boolean shouldSort(String sortBy, SortOrder sortOrder) {
		return StringUtils.isNotBlank(sortBy) && sortOrder != null && sortOrder != SortOrder.UNSORTED;
	}

	/**
	 * Check if the sort params map request sorting. Used for choosing of picking the sort-enabled or unsorted (performant) data fetcher.
	 * 
	 * @param sorting
	 * @return
	 */
	static boolean shouldSort(Map<String, SortOrder> sorting) {
		return (sorting == null || sorting.size() < 1) ? false : sorting.entrySet().stream().allMatch(e -> shouldSort(e.getKey(), e.getValue()));
	}

	/**
	 * Check if the REST API sort params request sorting. Used for choosing of picking the sort-enabled or unsorted (performant) data fetcher.
	 * 
	 * @param sorting
	 * @return
	 */
	static boolean shouldSort(SortingParameters sorting) {
		return sorting == null ? false : shouldSort(sorting.getSort());
	}

	/**
	 * Check if REST API paging params actually request pagination.
	 * 
	 * @param paging
	 * @return
	 */
	static boolean shouldPage(PagingParameters paging) {
		return paging == null ? false : shouldPage(paging.getPerPage(), paging.getPage());
	}

	/**
	 * Check if paging params actually request pagination.
	 * 
	 * @param perPage
	 * @param page
	 * @return
	 */
	static boolean shouldPage(Long perPage, int page) {
		return perPage != null && perPage > 0 && page > 0;
	}
}
