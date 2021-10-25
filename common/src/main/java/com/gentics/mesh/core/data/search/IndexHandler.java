package com.gentics.mesh.core.data.search;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.search.EntityMetrics;
import com.gentics.mesh.search.BucketableElement;

import io.reactivex.Completable;
import io.reactivex.Flowable;

/**
 * Index handlers are used to interact with the search provider index on a type specific level. Each domain model in mesh which is indexable needs to implement
 * an index handler in order to interact with search index specific documents in the index (CRUD on search index documents).
 *
 * @param <T>
 */
public interface IndexHandler<T extends MeshCoreVertex<?, T>> {
	public final static Pattern MATCH_ALL = Pattern.compile(".*");

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
	 * Returns loader function which can be used to find and load elements by uuid.
	 * 
	 * @return
	 */
	Function<String, T> elementLoader();

	/**
	 * Load all elements from the graph that are needed for the index handler.
	 * 
	 * @return
	 */
	Stream<? extends T> loadAllElements();

	/**
	 * Return the class of elements which can be handled by this handler.
	 * 
	 * @return
	 */
	Class<? extends BucketableElement> getElementClass();

	/**
	 * Diff the elements within all indices that are handled by the index handler and synchronize the data.
	 * 
	 * @param indexPattern optional index pattern to restrict synchronized indices
	 * @return
	 */
	Flowable<SearchRequest> syncIndices(Optional<Pattern> indexPattern);

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
	 * Get the names of all indices for searching purposes. The action context will be examined to determine the project scope and the branch scope.
	 * If possible even the version type will be extracted from the action context in order to generate the set of indices which are selected.
	 *
	 * This can also create
	 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/multi-index.html">wildcard patterns</a>
	 * catching multiple indices.
	 * 
	 * @param ac
	 *            action context
	 * @return name of selected indices
	 */
	Set<String> getIndicesForSearch(InternalActionContext ac);

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
	EntityMetrics getMetrics();

	/**
	 * Check indices handled by this handler for existence and correctness (mapping)
	 * @return completable
	 */
	Completable check();
}
