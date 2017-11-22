package com.gentics.mesh.core.data.search;

import java.util.Map;
import java.util.Set;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.index.IndexInfo;

import rx.Completable;

/**
 * Index handlers are used to interact with the search provider index on a type specific level. Each domain model in mesh which is indexable needs to implement
 * an index handler in order to interact with search index specific documents in the index (CRUD on search index documents).
 *
 * @param <T>
 */
public interface IndexHandler<T extends MeshCoreVertex<?, T>> {

	/**
	 * Clear the index. This will effectively remove all documents from the index without removing the index itself.
	 * 
	 * @return
	 */
	Completable clearIndex();

	/**
	 * Initialise the search index by creating the index first and setting the mapping afterwards.
	 * 
	 * @return
	 */
	Completable init();

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
	 * Delete the document with the given UUID and document type from the search index.
	 * 
	 * @param entry
	 *            search queue entry
	 * @return
	 */
	Completable delete(UpdateDocumentEntry entry);

	/**
	 * Load the given element and invoke store(T element) to store it in the index.
	 * 
	 * @param entry
	 *            search queue entry
	 * @return
	 */
	Completable store(UpdateDocumentEntry entry);

	/**
	 * Reindex all documents for the type which the handler is capable of.
	 * 
	 * @return
	 */
	Completable reindexAll();

	/**
	 * Load a map which contains the applicable indices. The key of the map is the index name.
	 * 
	 * @return Map with index information
	 */
	Map<String, IndexInfo> getIndices();

	/**
	 * Get the names of all selected indices. The action context will be examined to determine the project scope and the release scope. If possible even the
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
	 * Update the permissions for the document which is identified by the entry.
	 * 
	 * @param entry
	 * @return
	 */
	Completable updatePermission(UpdateDocumentEntry entry);

}
