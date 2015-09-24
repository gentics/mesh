package com.gentics.mesh.core.data.search.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;

public class SearchQueueBatchImpl extends MeshVertexImpl implements SearchQueueBatch {

	private static final Logger log = LoggerFactory.getLogger(SearchQueueBatchImpl.class);

	@Override
	public void addEntry(String uuid, String type, SearchQueueEntryAction action, String indexType) {
		SearchQueueEntry entry = getGraph().addFramedVertex(SearchQueueEntryImpl.class);
		entry.setElementUuid(uuid);
		entry.setElementType(type);
		entry.setElementAction(action.getName());
		entry.setElementIndexType(indexType);
		addEntry(entry);
	}

	@Override
	public void addEntry(SearchQueueEntry batch) {
		setLinkOutTo(batch.getImpl(), HAS_ITEM);
	}

	@Override
	public void addEntry(GenericVertex<?> vertex, SearchQueueEntryAction action) {
		addEntry(vertex.getUuid(), vertex.getType(), action);
	}

	@Override
	public void addEntry(String uuid, String type, SearchQueueEntryAction action) {
		addEntry(uuid, type, action, null);
	}

	@Override
	public List<? extends SearchQueueEntry> getEntries() {
		return out(HAS_ITEM).has(SearchQueueEntryImpl.class).toListExplicit(SearchQueueEntryImpl.class);
	}

	@Override
	public String getBatchId() {
		return getProperty(BATCH_ID_PROPERTY_KEY);
	}

	@Override
	public void setBatchId(String batchId) {
		setProperty(BATCH_ID_PROPERTY_KEY, batchId);
	}

	@Override
	public long getTimestamp() {
		return getProperty("timestamp");
	}

	@Override
	public void setTimestamp(long timestamp) {
		setProperty("timestamp", timestamp);

	}

	@Override
	public void delete() {
		for (SearchQueueEntry entry : getEntries()) {
			entry.delete();
		}
		getVertex().remove();
	}

	@Override
	public void printDebug() {
		for (SearchQueueEntry entry : getEntries()) {
			log.debug("Entry {" + entry.toString() + "} in batch {" + getBatchId() + "}");
		}
	}

	@Override
	public void process(Handler<AsyncResult<Void>> handler) {

		if (log.isDebugEnabled()) {
			log.debug("Processing batch {" + getBatchId() + "}");
			printDebug();
		}
		Set<ObservableFuture<Void>> futures = new HashSet<>();
		for (SearchQueueEntry entry : getEntries()) {
			ObservableFuture<Void> obs = RxHelper.observableFuture();
			entry.process(obs.toHandler());
			futures.add(obs);
		}
		Observable.merge(futures).subscribe(item -> {
			if (log.isDebugEnabled()) {
				log.debug("Handled search queue item.");
			}
		} , error -> {
			log.error("Could not process batch {" + getBatchId() + "}.", error);
			handler.handle(Future.failedFuture(error));
		} , () -> {
			MeshSpringConfiguration springConfiguration = MeshSpringConfiguration.getInstance();
			SearchProvider provider = springConfiguration.searchProvider();
			Database db = springConfiguration.database();
			// We successfully finished this batch. Delete it.
			try (Trx txDelete = db.trx()) {
				reload();
				delete();
				txDelete.success();
			}
			if (provider != null) {
				provider.refreshIndex();
			} else {
				log.error("Could not refresh index since the elastic search provider has not been initalized");
			}
			handler.handle(Future.succeededFuture());
		});
	}

}
