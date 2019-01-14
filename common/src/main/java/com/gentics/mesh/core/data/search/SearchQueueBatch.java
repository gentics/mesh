package com.gentics.mesh.core.data.search;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.IndexableElement;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.context.GenericEntryContext;
import com.gentics.mesh.core.data.search.context.impl.GenericEntryContextImpl;
import com.gentics.mesh.core.rest.schema.Schema;

import io.reactivex.Completable;

/**
 * A batch of search queue entries. Usually a batch groups those elements that need to be updated in order to sync the search index with the graph database
 * changes.
 */
public interface SearchQueueBatch {

	/**
	 * Add an entry to the list which includes index creation information.
	 * 
	 * @param indexName
	 *            Name of the index which should be created
	 * @param elementClass
	 *            Class of the elements that are stored in the index. This value is used to determine the correct index handler when creating the index
	 * @return Fluent API
	 */
	SearchQueueBatch createIndex(String indexName, Class<?> elementClass);

	/**
	 * Queue an drop index action.
	 * 
	 * @param indexName
	 * @return Fluent API
	 */
	SearchQueueBatch dropIndex(String indexName);

	/**
	 * Add a new node index to the search database and construct the index name using the provided values. See
	 * {@link NodeContainerEntry#composeIndexName(String, String, String, ContainerType)} for details.
	 * 
	 * @param project
	 * @param branch
	 * @param version
	 * @param type
	 * @return Fluent API
	 */
	default SearchQueueBatch addNodeIndex(Project project, Branch branch, SchemaContainerVersion version, ContainerType type) {
		return createNodeIndex(project.getUuid(), branch.getUuid(), version.getUuid(), type, version.getSchema());
	}

	/**
	 * Add a new node index to the search database. The node index name is constructed using the provided values. See
	 * {@link NodeContainerEntry#composeIndexName(String, String, String, ContainerType)} for details.
	 * 
	 * @param projectUuid
	 * @param branchUuid
	 * @param versionUuid
	 * @param schema
	 * @param type
	 * @return Fluent API
	 */
	SearchQueueBatch createNodeIndex(String projectUuid, String branchUuid, String versionUuid, ContainerType type, Schema schema);

	/**
	 * Add the tag family index to the search database. See {@link TagFamilyEntry#composeIndexName(String)} for details.
	 * 
	 * @param projectUuid
	 * @return Fluent API
	 */
	default SearchQueueBatch createTagFamilyIndex(String projectUuid) {
		return createIndex(TagFamily.composeIndexName(projectUuid), TagFamily.class);
	}

	/**
	 * Add the tag index to the search database. See {@link TagEntry#composeIndexName(String)} for details.
	 * 
	 * @param projectUuid
	 * @return Fluent API
	 */
	default SearchQueueBatch createTagIndex(String projectUuid) {
		return createIndex(Tag.composeIndexName(projectUuid), Tag.class);
	}

	/**
	 * Add the the given element to the search index.
	 * 
	 * @param element
	 *            Element to be stored in the index
	 * @param addRelatedEntries
	 *            Flag which indicates whether related elements should also be stored
	 * @return Fluent API
	 */
	default SearchQueueBatch store(IndexableElement element, boolean addRelatedEntries) {
		return store(element, new GenericEntryContextImpl(), addRelatedEntries);
	}

	/**
	 * Add a store entry for the container.
	 * 
	 * @param element
	 * @param context
	 * @param addRelatedEntries
	 * @return Fluent API
	 */
	SearchQueueBatch store(IndexableElement element, GenericEntryContext context, boolean addRelatedEntries);

	/**
	 * Move the node from the old index and store it in the new index.
	 * 
	 * @param oldContainer
	 * @param newContainer
	 * @param branchUuid
	 * @param type
	 * @return Fluent API
	 */
	SearchQueueBatch move(NodeGraphFieldContainer oldContainer, NodeGraphFieldContainer newContainer, String branchUuid, ContainerType type);

	/**
	 * Delete the given element from the its index.
	 * 
	 * @param element
	 * @param context
	 * @param addRelatedEntries
	 * @return Fluent API
	 */
	SearchQueueBatch delete(IndexableElement element, GenericEntryContext context, boolean addRelatedEntries);

	/**
	 * Add an entry to this batch.
	 * 
	 * @param entry
	 * @return Added entry
	 */
	BulkSearchQueueEntry<?> addEntry(BulkSearchQueueEntry<?> entry);

	/**
	 * Add an entry to this batch.
	 *
	 * @param entry
	 * @return Added entry
	 */
	SeperateSearchQueueEntry<?> addEntry(SeperateSearchQueueEntry<?> entry);

	/**
	 * Return a list of entries for this batch.
	 * 
	 * @return
	 */
	List<? extends SearchQueueEntry> getEntries();

	/**
	 * Return the batch id for this batch.
	 * 
	 * @return Id of the batch
	 */
	String getBatchId();

	/**
	 * Process this batch by invoking process on all batch entries.
	 * 
	 * @return
	 */
	Completable processAsync();

	/**
	 * Process this batch blocking and fail if the given timeout was exceeded.
	 * 
	 * @param timeout
	 * @param unit
	 */
	void processSync(long timeout, TimeUnit unit);

	/**
	 * Process this batch and block until it finishes. Apply a default timeout on this operation.
	 */
	void processSync();

	/**
	 * Print debug output which contains information about all entries of the batch.
	 */
	void printDebug();

	/**
	 * Delete the given element from the index.
	 * 
	 * @param element
	 * @param addRelatedEntries
	 * @return Fluent API
	 */
	default SearchQueueBatch delete(IndexableElement element, boolean addRelatedEntries) {
		return delete(element, new GenericEntryContextImpl(), addRelatedEntries);
	}

	/**
	 * Delete the tag document from the index.
	 * 
	 * @param tag
	 * @param addRelatedEntries
	 * @return Fluent API
	 */
	SearchQueueBatch delete(Tag tag, boolean addRelatedEntries);

	/**
	 * Add an store entry for the given node. The index handler will automatically handle all languages of the found containers.
	 * 
	 * @param node
	 * @param branchUuid
	 * @param type
	 * @param addRelatedElements
	 *            Whether to also add related elements (e.g: child nodes)
	 * @return Fluent API
	 */
	SearchQueueBatch store(Node node, String branchUuid, ContainerType type, boolean addRelatedElements);

	/**
	 * Add a store entry which just contains the node element information. This will effectively store all documents of the node (eg. all branches, all
	 * languages, all types). Use a more restricted {@link #store(Node, String, ContainerType, boolean)} call if you know what needs to be updated to reduce the
	 * overhead.
	 * 
	 * @param node
	 * @return Fluent API
	 */
	default SearchQueueBatch store(Node node) {
		return store(node, null, null, false);
	}

	/**
	 * Add a store entry which just contains the node element information. This will effectively store all documents of the node for the given branch but
	 * include (eg. all languages, all types). Use a more restricted {@link #store(Node, String, ContainerType, boolean)} call if you know what needs to be
	 * updated to reduce the overhead.
	 * 
	 * @param node
	 * @param branchUuid
	 * @return Fluent API
	 */
	default SearchQueueBatch store(Node node, String branchUuid) {
		return store(node, branchUuid, null, false);
	}

	SearchQueueBatch store(NodeGraphFieldContainer container, String branchUuid, ContainerType type, boolean addRelatedElements);

	/**
	 * Add an delete entry to the batch.
	 * 
	 * @param container
	 *            Affected node container which will provide needed information for the index handler
	 * @param branchUuid
	 *            Branch uuid of the container which should be handled
	 * @param type
	 *            Type of the container which should be removed from the index
	 * @param addRelatedEntries
	 * @return Fluent API
	 */
	SearchQueueBatch delete(NodeGraphFieldContainer container, String branchUuid, ContainerType type, boolean addRelatedEntries);

	/**
	 * Store the tag family in the search index.
	 * 
	 * @param tagFammily
	 * @param addRelatedEntries
	 * @return Fluent API
	 */
	SearchQueueBatch delete(TagFamily tagFammily, boolean addRelatedEntries);

	/**
	 * Clear all entries.
	 */
	void clear();

	/**
	 * Update the permission of the given element.
	 * 
	 * @param element
	 * @return Fluent API
	 */
	SearchQueueBatch updatePermissions(IndexableElement element);

	/**
	 * Return the current size of the batch.
	 */
	int size();

	/**
	 * Adds all entries from another batch to this batch
	 * @param otherBatch
	 */
	void addAll(SearchQueueBatch otherBatch);
}
