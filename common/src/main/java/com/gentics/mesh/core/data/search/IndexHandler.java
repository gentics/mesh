package com.gentics.mesh.core.data.search;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.bulk.DeleteBulkEntry;
import com.gentics.mesh.core.data.search.bulk.IndexBulkEntry;
import com.gentics.mesh.core.data.search.bulk.UpdateBulkEntry;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;

import java.util.Map;
import java.util.Set;

/**
 * Index handlers are used to interact with the search provider index on a type specific level. Each domain model in mesh which is indexable needs to implement
 * an index handler in order to interact with search index specific documents in the index (CRUD on search index documents).
 *
 * @param <T>
 */
public interface IndexHandler<T extends MeshCoreVertex<?, T>> {

	/**
	 * Initialise the search index by creating the index first and setting the mapping afterwards.
	 * 
	 * @return
	 */
	Completable init();

	/**
	 * Shortname of the type which is handled by the handler. The name must not contain spaces.
	 * 
	 * @return
	 */
	String getType();

	/**
	 * Return the root vertex of the index handler. The root vertex is used to retrieve nodes by UUID in order to update the search index.
	 * 
	 * @return
	 */
	RootVertex<T> getRootVertex();

	/**
	 * Return the class of elements which can be handled by this handler.
	 * 
	 * @return
	 */
	Class<?> getElementClass();

	/**
	 * Process the entry and generate bulk entries.
	 * 
	 * @param entry
	 * @return
	 */
	Observable<DeleteBulkEntry> deleteForBulk(UpdateDocumentEntry entry);

	/**
	 * Process the update entry into bulk entries which can be used form a bulk update request.
	 * 
	 * @param entry
	 * @return
	 */
	Observable<IndexBulkEntry> storeForBulk(UpdateDocumentEntry entry);

	/**
	 * Diff the elements within all indices that are handled by the index handler and synchronize the data.
	 * 
	 * @return
	 */
	Flowable<SearchRequest> syncIndices();

	/**
	 * Filter the given list and return only indices which match the type of the handler but are no longer in use or unknown.
	 * 
	 * @param indices
	 * @return
	 */
	Set<String> filterUnknownIndices(Set<String> indices);

	/**
	 * Load a map which contains the applicable indices. The key of the map is the index name.
	 * 
	 * @return Map with index information
	 */
	Map<String, IndexInfo> getIndices();

	/**
	 * Get the names of all selected indices. The action context will be examined to determine the project scope and the branch scope. If possible even the
	 * version type will be extracted from the action context in order to generate the set of indices which are selected.
	 * 
	 * @param ac
	 *            action context
	 * @return name of selected indices
	 */
	Set<String> getSelectedIndices(InternalActionContext ac);

	/**
	 * Get the permission required to read the elements found in the index.
	 * 
	 * @param ac
	 *            action context
	 * @return read permission
	 */
	default GraphPermission getReadPermission(InternalActionContext ac) {
		return GraphPermission.READ_PERM;
	}

	/**
	 * Check whether the given element class can be handled by this handler.
	 * 
	 * @param clazzOfElement
	 * @return
	 */
	boolean accepts(Class<?> clazzOfElement);

	/**
	 * Create the index, if it is one of the indices handled by this index handler. If the index name is not handled by this index handler, an error will be
	 * thrown.
	 * 
	 * @param entry
	 *            Search queue entry for create index action
	 * @return
	 */
	Completable createIndex(CreateIndexEntry entry);

	/**
	 * Generate the version for the given element.
	 * 
	 * @param element
	 * @return
	 */
	String generateVersion(T element);

	/**
	 * Returns a map of metrics which the handler is aware of.
	 * 
	 * @return
	 */
	Map<String, Object> getMetrics();

	/**
	 * Process the entry and generate the bulk entry.
	 * 
	 * @param entry
	 * @return
	 */
	Observable<UpdateBulkEntry> updatePermissionForBulk(UpdateDocumentEntry entry);

}
