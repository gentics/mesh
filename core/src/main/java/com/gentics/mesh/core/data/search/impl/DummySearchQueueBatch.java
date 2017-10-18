package com.gentics.mesh.core.data.search.impl;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.IndexableElement;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.context.GenericEntryContext;
import com.gentics.mesh.core.rest.schema.Schema;

import io.reactivex.Completable;

/**
 * Dummy search queue batch which can be used to avoid creation of unwanted batch entries. This is useful if a drop index is more efficient then removing each
 * entry individually. (e.g.: project deletion)
 */
public class DummySearchQueueBatch implements SearchQueueBatch {

	@Override
	public SearchQueueBatch createIndex(String indexName, String indexType, Class<?> elementClass) {
		return this;
	}

	@Override
	public SearchQueueBatch dropIndex(String indexName) {
		return this;
	}

	@Override
	public SearchQueueBatch createNodeIndex(String projectUuid, String releaseUuid, String versionUuid, ContainerType type, Schema schema) {
		return this;
	}

	@Override
	public SearchQueueBatch store(Node node, String releaseUuid, ContainerType type, boolean addRelatedEntries) {
		return this;
	}

	@Override
	public SearchQueueBatch store(IndexableElement element, GenericEntryContext context, boolean addRelatedEntries) {
		return this;
	}

	@Override
	public SearchQueueBatch move(NodeGraphFieldContainer oldContainer, NodeGraphFieldContainer newContainer, String releaseUuid, ContainerType type) {
		return this;
	}

	@Override
	public SearchQueueBatch delete(IndexableElement element, GenericEntryContext context, boolean addRelatedEntries) {
		return this;
	}

	@Override
	public SearchQueueEntry addEntry(SearchQueueEntry entry) {
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

	@Override
	public void printDebug() {

	}

	@Override
	public SearchQueueBatch delete(NodeGraphFieldContainer container, String releaseUuid, ContainerType type, boolean addRelatedEntries) {
		return this;
	}

	@Override
	public SearchQueueBatch store(NodeGraphFieldContainer container, String releaseUuid, ContainerType type, boolean addRelatedElements) {
		return this;
	}

	@Override
	public SearchQueueBatch delete(Tag element, boolean addRelatedEntries) {
		return this;
	}

	@Override
	public SearchQueueBatch delete(TagFamily tagFymily, boolean addRelatedEntries) {
		return this;
	}

	@Override
	public void clear() {
	}
}
