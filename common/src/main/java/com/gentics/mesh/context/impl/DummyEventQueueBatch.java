package com.gentics.mesh.context.impl;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.IndexableElement;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.search.BulkEventQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SeperateSearchQueueEntry;
import com.gentics.mesh.core.data.search.context.GenericEntryContext;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.event.EventQueueBatch;

import io.reactivex.Completable;

/**
 * Dummy search queue batch which can be used to avoid creation of unwanted batch entries. This is useful if a drop index is more efficient then removing each
 * entry individually. (e.g.: project deletion)
 */
public class DummyEventQueueBatch implements EventQueueBatch {

	@Override
	public EventQueueBatch createIndex(String indexName, Class<?> elementClass) {
		return this;
	}

	@Override
	public EventQueueBatch dropIndex(String indexName) {
		return this;
	}

	@Override
	public EventQueueBatch createNodeIndex(String projectUuid, String branchUuid, String versionUuid, ContainerType type, Schema schema) {
		return this;
	}

	@Override
	public EventQueueBatch store(Node node, String branchUuid, ContainerType type, boolean addRelatedEntries) {
		return this;
	}

	@Override
	public EventQueueBatch store(IndexableElement element, GenericEntryContext context, boolean addRelatedEntries) {
		return this;
	}

	@Override
	public EventQueueBatch move(NodeGraphFieldContainer oldContainer, NodeGraphFieldContainer newContainer, String branchUuid, ContainerType type) {
		return this;
	}

	@Override
	public EventQueueBatch updatePermissions(IndexableElement element) {
		return this;
	}

	@Override
	public EventQueueBatch delete(IndexableElement element, GenericEntryContext context, boolean addRelatedEntries) {
		return this;
	}

	@Override
	public BulkEventQueueEntry<?> addEntry(BulkEventQueueEntry<?> entry) {
		return null;
	}

	@Override
	public SeperateSearchQueueEntry<?> addEntry(SeperateSearchQueueEntry<?> entry) {
		return null;
	}

	@Override
	public List<? extends SearchQueueEntry> getEntries() {
		return Collections.emptyList();
	}

	@Override
	public String getBatchId() {
		return null;
	}

/**
	@Override
	public Completable processAsync() {
		return Completable.complete();
	}

	@Override
	public void processSync(long timeout, TimeUnit unit) {

	}

	@Override
	public void processSync() {

	}
**/

	@Override
	public void printDebug() {

	}

	@Override
	public EventQueueBatch delete(NodeGraphFieldContainer container, String branchUuid, ContainerType type, boolean addRelatedEntries) {
		return this;
	}

	@Override
	public EventQueueBatch store(NodeGraphFieldContainer container, String branchUuid, ContainerType type, boolean addRelatedElements) {
		return this;
	}

	@Override
	public EventQueueBatch delete(Tag element, boolean addRelatedEntries) {
		return this;
	}

	@Override
	public EventQueueBatch delete(TagFamily tagFymily, boolean addRelatedEntries) {
		return this;
	}

	@Override
	public void clear() {
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public void addAll(EventQueueBatch otherBatch) {
	}

	@Override
	public Completable dispatch() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updated(IndexableElement updateElement) {
		// TODO Auto-generated method stub
		
	}
}