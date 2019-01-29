package com.gentics.mesh.search.verticle.event.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.search.BulkEventQueueEntry;
import com.gentics.mesh.core.data.search.bulk.BulkEntry;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.common.DropIndexHandler;
import com.gentics.mesh.search.index.node.NodeIndexHandler;
import com.gentics.mesh.search.index.tag.TagIndexHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilyIndexHandler;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;

@Singleton
public class EventProcessorImpl {

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
	SearchProvider searchProvider;
	
	
	
//	@Override
//	public Completable processAsync() {
//		if (!searchProvider.isActive()) {
//			return Completable.create(s -> {
//				clear();
//				s.onComplete();
//			});
//		}
//		return Completable.defer(() -> {
//			Completable obs = Completable.complete();
//
//			if (!seperateEntries.isEmpty()) {
//				List<Completable> seperateEntryList = seperateEntries.stream().map(entry -> entry.process()).collect(Collectors.toList());
//				obs = Completable.concat(Flowable.fromIterable(seperateEntryList), 1);
//			}
//			int bulkLimit = Mesh.mesh().getOptions().getSearchOptions().getBulkLimit();
//			if (!bulkEntries.isEmpty()) {
//				Observable<BulkEntry> bulks = Observable.fromIterable(bulkEntries)
//					.flatMap(BulkEventQueueEntry::process);
//
//				AtomicLong counter = new AtomicLong();
//				Completable bulkProcessing = bulks
//					.buffer(bulkLimit)
//					.flatMapCompletable(bulk -> searchProvider.processBulk(bulk).doOnComplete(() -> {
//						log.debug("Bulk completed {" + counter.incrementAndGet() + "}");
//					}));
//				obs = obs.andThen(bulkProcessing);
//			}
//
//			return obs.andThen(searchProvider.refreshIndex()).doOnComplete(() -> {
//				if (log.isDebugEnabled()) {
//					log.debug("Handled all search queue items.");
//				}
//				// Clear the batch entries so that the GC can claim the memory
//				clear();
//			}).doOnError(error -> {
//				log.error("Error while processing batch {" + batchId + "}");
//				if (log.isDebugEnabled()) {
//					printDebug();
//				}
//				clear();
//			});
//		});
//	}
//
//	@Override
//	public void processSync(long timeout, TimeUnit unit) {
//		if (searchProvider.isActive()) {
//			if (!processAsync().blockingAwait(timeout, unit)) {
//				throw error(INTERNAL_SERVER_ERROR,
//					"Batch {" + getBatchId() + "} did not finish in time. Timeout of {" + timeout + "} / {" + unit.name()
//						+ "} exceeded.");
//			}
//		} else {
//			clear();
//		}
//	}
//
//	@Override
//	public void processSync() {
//		processSync(120, TimeUnit.SECONDS);
//	}
}
