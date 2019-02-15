package com.gentics.mesh.context.impl;

import java.util.List;

import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * Dummy search queue batch which can be used to avoid creation of unwanted batch entries. This is useful if a drop index is more efficient then removing each
 * entry individually. (e.g.: project deletion)
 */
public class DummyEventQueueBatch implements EventQueueBatch {

	// @Override
	// public EventQueueBatch createIndex(String indexName, Class<?> elementClass) {
	// return this;
	// }
	//
	// @Override
	// public EventQueueBatch dropIndex(String indexName) {
	// return this;
	// }
	//
	// @Override
	// public EventQueueBatch createNodeIndex(String projectUuid, String branchUuid, String versionUuid, ContainerType type, Schema schema) {
	// return this;
	// }
	//
	// @Override
	// public EventQueueBatch store(Node node, String branchUuid, ContainerType type, boolean addRelatedEntries) {
	// return this;
	// }
	//
	// @Override
	// public EventQueueBatch store(IndexableElement element, GenericEntryContext context, boolean addRelatedEntries) {
	// return this;
	// }
	//
	// @Override
	// public EventQueueBatch move(NodeGraphFieldContainer oldContainer, NodeGraphFieldContainer newContainer, String branchUuid, ContainerType type) {
	// return this;
	// }
	//
	// @Override
	// public EventQueueBatch updatePermissions(IndexableElement element) {
	// return this;
	// }
	//
	// @Override
	// public EventQueueBatch delete(IndexableElement element, GenericEntryContext context, boolean addRelatedEntries) {
	// return this;
	// }
	//
	// @Override
	// public BulkEventQueueEntry<?> addEntry(BulkEventQueueEntry<?> entry) {
	// return null;
	// }
	//
	// @Override
	// public SeperateSearchQueueEntry<?> addEntry(SeperateSearchQueueEntry<?> entry) {
	// return null;
	// }
	//
	// @Override
	// public List<? extends SearchQueueEntry> getEntries() {
	// return Collections.emptyList();
	// }

	/**
	 * @Override public String getBatchId() { return null; }
	 * 
	 * @Override public Completable processAsync() { return Completable.complete(); }
	 * 
	 * @Override public void processSync(long timeout, TimeUnit unit) {
	 * 
	 *           }
	 * 
	 * @Override public void processSync() {
	 * 
	 *           }
	 **/

	// @Override
	// public void printDebug() {
	//
	// }
	//
	// @Override
	// public EventQueueBatch delete(NodeGraphFieldContainer container, String branchUuid, ContainerType type, boolean addRelatedEntries) {
	// return this;
	// }
	//
	// @Override
	// public EventQueueBatch store(NodeGraphFieldContainer container, String branchUuid, ContainerType type, boolean addRelatedElements) {
	// return this;
	// }
	//
	// @Override
	// public EventQueueBatch delete(Tag element, boolean addRelatedEntries) {
	// return this;
	// }
	//
	// @Override
	// public EventQueueBatch delete(TagFamily tagFymily, boolean addRelatedEntries) {
	// return this;
	// }

	@Override
	public String getBatchId() {
		return null;
	}

	@Override
	public void dispatch() {

	}

	@Override
	public List<MeshEventModel> getEntries() {
		return null;
	}

	@Override
	public void setRootCause(String type, String uuid, String action) {

	}

	@Override
	public EventCauseInfo getCause() {
		return null;
	}

}