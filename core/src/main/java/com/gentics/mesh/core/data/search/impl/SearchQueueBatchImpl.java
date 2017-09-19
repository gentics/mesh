package com.gentics.mesh.core.data.search.impl;

import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.HandleContext;
import com.gentics.mesh.core.data.IndexableElement;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.search.CreateIndexEntry;
import com.gentics.mesh.core.data.search.DropIndexEntry;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.index.common.CreateIndexEntryImpl;
import com.gentics.mesh.search.index.common.DropIndexEntryImpl;
import com.gentics.mesh.search.index.common.DropIndexHandler;
import com.gentics.mesh.search.index.entry.UpdateDocumentEntryImpl;
import com.gentics.mesh.search.index.node.NodeIndexHandler;
import com.gentics.mesh.search.index.tag.TagIndexHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilyIndexHandler;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Completable;
import rx.Observable;

/**
 * @see SearchQueueBatch
 */
public class SearchQueueBatchImpl implements SearchQueueBatch {

	private String batchId;
	private List<SearchQueueEntry> entries = new ArrayList<>();

	private static final Logger log = LoggerFactory.getLogger(SearchQueueBatchImpl.class);

	@Inject
	IndexHandlerRegistry registry;

	@Inject
	NodeIndexHandler nodeContainerIndexHandler;

	@Inject
	TagFamilyIndexHandler tagfamilyIndexHandler;

	@Inject
	TagIndexHandler tagIndexHandler;

	@Inject
	DropIndexHandler commonHandler;

	@Inject
	public SearchQueueBatchImpl() {
	}

	@Override
	public SearchQueueBatch createIndex(String indexName, String indexType, Class<?> elementClass) {
		CreateIndexEntry entry = new CreateIndexEntryImpl(registry.getForClass(elementClass), indexName, indexType);
		addEntry(entry);
		return this;
	}

	@Override
	public SearchQueueBatch createNodeIndex(String projectUuid, String releaseUuid, String versionUuid, ContainerType type, Schema schema) {
		String indexName = NodeGraphFieldContainer.composeIndexName(projectUuid, releaseUuid, versionUuid, type);
		String indexType = NodeGraphFieldContainer.composeIndexType();
		CreateIndexEntry entry = new CreateIndexEntryImpl(nodeContainerIndexHandler, indexName, indexType);
		entry.setSchema(schema);
		addEntry(entry);
		return this;
	}

	@Override
	public SearchQueueBatch dropIndex(String indexName) {
		DropIndexEntry entry = new DropIndexEntryImpl(commonHandler, indexName);
		addEntry(entry);
		return this;
	}

	@Override
	public SearchQueueBatch store(Node node, String releaseUuid, ContainerType type, boolean addRelatedElements) {
		HandleContext context = new HandleContext();
		context.setContainerType(type);
		context.setReleaseUuid(releaseUuid);
		// context.setLanguageTag(node.getLanguage().getLanguageTag());
		context.setProjectUuid(node.getProject().getUuid());
		store((IndexableElement) node, context, addRelatedElements);
		return this;
	}

	@Override
	public SearchQueueBatch store(NodeGraphFieldContainer container, String releaseUuid, ContainerType type, boolean addRelatedElements) {
		Node node = container.getParentNode();
		HandleContext context = new HandleContext();
		context.setContainerType(type);
		context.setReleaseUuid(releaseUuid);
		context.setLanguageTag(container.getLanguage().getLanguageTag());
		context.setSchemaContainerVersionUuid(container.getSchemaContainerVersion().getUuid());
		context.setProjectUuid(node.getProject().getUuid());
		store((IndexableElement) node, context, addRelatedElements);
		return this;
	}

	@Override
	public SearchQueueBatch delete(Tag tag, boolean addRelatedEntries) {
		// We need to add the project uuid to the context because the index handler for tags will not be able to
		// determine the project uuid once the tag has been removed from the graph.
		HandleContext context = new HandleContext();
		context.setProjectUuid(tag.getProject().getUuid());
		delete((IndexableElement) tag, context, addRelatedEntries);
		return this;
	}

	@Override
	public SearchQueueBatch delete(TagFamily tagFymily, boolean addRelatedEntries) {
		// We need to add the project uuid to the context because the index handler for tagfamilies will not be able to
		// determine the project uuid once the tagfamily has been removed from the graph.
		HandleContext context = new HandleContext();
		context.setProjectUuid(tagFymily.getProject().getUuid());
		delete((IndexableElement) tagFymily, context, addRelatedEntries);
		return this;
	}

	@Override
	public SearchQueueBatch delete(NodeGraphFieldContainer container, String releaseUuid, ContainerType type, boolean addRelatedEntries) {
		HandleContext context = new HandleContext();
		context.setContainerType(type);
		context.setProjectUuid(container.getParentNode().getProject().getUuid());
		context.setReleaseUuid(releaseUuid);
		context.setSchemaContainerVersionUuid(container.getSchemaContainerVersion().getUuid());
		context.setLanguageTag(container.getLanguage().getLanguageTag());
		delete((IndexableElement) container.getParentNode(), context, addRelatedEntries);
		return this;
	}

	@Override
	public SearchQueueBatch store(IndexableElement element, HandleContext context, boolean addRelatedEntries) {
		UpdateDocumentEntryImpl entry = new UpdateDocumentEntryImpl(registry.getForClass(element), element, context, STORE_ACTION);
		addEntry(entry);

		if (addRelatedEntries) {
			// We need to store (e.g: Update related entries)
			element.handleRelatedEntries((relatedElement, relatedContext) -> {
				store(relatedElement, relatedContext, false);
			});
		}
		return this;
	}

	@Override
	public SearchQueueBatch delete(IndexableElement element, HandleContext context, boolean addRelatedEntries) {
		UpdateDocumentEntry entry = new UpdateDocumentEntryImpl(registry.getForClass(element), element, context, DELETE_ACTION);
		addEntry(entry);

		if (addRelatedEntries) {
			// We need to store (e.g: Update related entries)
			element.handleRelatedEntries((relatedElement, relatedContext) -> {
				this.store(relatedElement, relatedContext, false);
			});
		}
		return this;
	}

	@Override
	public SearchQueueEntry addEntry(SearchQueueEntry entry) {
		entries.add(entry);
		return entry;
	}

	@Override
	public List<? extends SearchQueueEntry> getEntries() {
		entries.sort((o1, o2) -> o1.getElementAction().compareTo(o2.getElementAction()));

		if (log.isDebugEnabled()) {
			for (SearchQueueEntry entry : entries) {
				log.debug("Loaded entry {" + entry.toString() + "} for batch {" + getBatchId() + "}");
			}
		}
		return entries;
	}

	@Override
	public String getBatchId() {
		return batchId;
	}

	@Override
	public void printDebug() {
		for (SearchQueueEntry entry : getEntries()) {
			log.debug("Entry {" + entry.toString() + "} in batch {" + getBatchId() + "}");
		}
	}

	@Override
	public Completable processAsync() {
		return Completable.defer(() -> {
			// Process the batch
			Completable obs = Completable.complete();
			List<? extends SearchQueueEntry> nonStoreEntries = getEntries().stream().filter(i -> i.getElementAction() != STORE_ACTION)
					.collect(Collectors.toList());

			List<? extends SearchQueueEntry> storeEntries = getEntries().stream().filter(i -> i.getElementAction() == STORE_ACTION)
					.collect(Collectors.toList());

			if (!nonStoreEntries.isEmpty()) {
				obs = Completable.concat(nonStoreEntries.stream().map(entry -> entry.process()).collect(Collectors.toList()));
			}
			AtomicLong counter = new AtomicLong();
			if (!storeEntries.isEmpty()) {
				List<Completable> entryList = storeEntries.stream().map(entry -> entry.process()).collect(Collectors.toList());
				// Handle all entries sequentially since some entries create entries and those need to be executed first
				int batchSize = 8;
				long totalBatchCount = entryList.size() / batchSize;
				Observable<Completable> obs2 = Observable.from(entryList);
				Observable<List<Completable>> buffers = obs2.buffer(batchSize);
				// First ensure that the non-store events are processed before handling the store batches
				obs = obs.andThen(Completable.concat(buffers.map(i -> Completable.merge(i).doOnCompleted(() -> {
					if (totalBatchCount > 0) {
						log.info("Search queue entry batch completed {" + counter.incrementAndGet() + "/" + totalBatchCount + "}");
					}
				}))));
			}

			return obs.doOnCompleted(() -> {
				if (log.isDebugEnabled()) {
					log.debug("Handled all search queue items.");
				}
				// Clear the batch entries so that the GC can claim the memory
				clear();
			}).doOnError(error -> {
				log.error("Error while processing batch {" + batchId + "}");
				if (log.isDebugEnabled()) {
					printDebug();
				}
				clear();
			});
		});
	}

	@Override
	public void processSync(long timeout, TimeUnit unit) {
		if (!processAsync().await(timeout, unit)) {
			throw error(INTERNAL_SERVER_ERROR,
					"Batch {" + getBatchId() + "} did not finish in time. Timeout of {" + timeout + "} / {" + unit.name() + "} exceeded.");
		}
	}

	@Override
	public void processSync() {
		processSync(120, TimeUnit.SECONDS);
	}

	@Override
	public void clear() {
		entries.clear();
	}

}
