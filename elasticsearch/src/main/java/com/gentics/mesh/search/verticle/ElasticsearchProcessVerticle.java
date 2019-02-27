package com.gentics.mesh.search.verticle;

import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.verticle.eventhandler.MainEventHandler;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.eventbus.MessageConsumer;

import javax.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ElasticsearchProcessVerticle extends AbstractVerticle {
	private static final Logger log = LoggerFactory.getLogger(ElasticsearchProcessVerticle.class);

	private final MainEventHandler mainEventhandler;
	private final SearchProvider searchProvider;
	private BulkOperator bulker;

	private Subject<MessageEvent> requests = PublishSubject.create();
	private Subject<Object> idling = PublishSubject.create();
	private static final Object dummyObject = new Object();
	// TODO put the counters in a dedicated class and use it here
	private AtomicInteger pendingRequests = new AtomicInteger();
	private AtomicInteger pendingTransformations = new AtomicInteger();
	private List<MessageConsumer<JsonObject>> vertxHandlers;

	@Inject
	public ElasticsearchProcessVerticle(MainEventHandler mainEventhandler, SearchProvider searchProvider) {
		this.mainEventhandler = mainEventhandler;
		this.searchProvider = searchProvider;
	}

	@Override
	public void start() {
		log.trace("Initializing Elasticsearch process verticle");
		assemble();

		vertxHandlers = mainEventhandler.handledEvents()
			.stream()
			.map(event -> vertx.eventBus().<JsonObject>consumer(event.address, message -> {
				pendingTransformations.incrementAndGet();
				log.trace(String.format("Received event message on address {%s}:\n%s", message.address(), message.body()));
				requests.onNext(new MessageEvent(event, MeshEventModel.fromMessage(message)));
			}))
			.map((Function<io.vertx.core.eventbus.MessageConsumer<JsonObject>, MessageConsumer<JsonObject>>) MessageConsumer::new)
			.collect(Collectors.toList());
	}

	@Override
	public void stop(Future<Void> stopFuture) {
		Observable.fromIterable(vertxHandlers)
			.flatMapCompletable(MessageConsumer::rxUnregister)
			.andThen(flush())
			.subscribe(() -> {
				requests.onComplete();
				idling.onComplete();
				stopFuture.complete();
			});
	}

	/**
	 * Flushes the buffer of Elasticsearch requests and dispatches all pending requests.
	 */
	public Completable flush() {
		return Completable.fromRunnable(() -> vertx.eventBus().send(MeshEvent.SEARCH_FLUSH_REQUEST.address, null));
//		return Completable.defer(() -> {
//			if (bulker != null) {
//				bulker.flush();
//			}
//			return idling.firstOrError().toCompletable();
//		});
	}

	/**
	 * Refreshes the Elasticsearch indices so that all changes are readable
	 * @return
	 */
	public Completable refresh() {
		return searchProvider.refreshIndex()
			.doOnSubscribe(ignore -> log.trace("Refreshing all Elasticsearch indices..."))
			.doOnComplete(() -> log.trace("Refresh complete."));
	}

	/**
	 * Tests if the verticle has any queued requests.
	 * @return
	 */
	public boolean isIdle() {
		return pendingRequests.get() == 0 && pendingTransformations.get() == 0 && !bulker.bulking();
	}

	private void assemble() {
		// TODO Make bulk operator options configurable
		bulker = new BulkOperator(vertx, Duration.ofSeconds(500), 1000);
		requests.toFlowable(BackpressureStrategy.MISSING)
			.onBackpressureBuffer(1000)
			.concatMap(this::generateRequests, 1)
			.doOnNext(request -> {
				if (log.isTraceEnabled()) {
					log.trace(String.format("Generated request of class {%s}", request.getClass().getSimpleName()));
				}
			})
			.toObservable()
			.lift(bulker)
			.concatMap(request ->
				request.execute(searchProvider).andThen(Observable.just(request))
					.doOnSubscribe(ignore -> log.trace("Sending request to Elasticsearch:\n" + request)),
				1
			)
			.subscribe(request -> {
				log.trace("Request completed:\n" + request);
				pendingRequests.addAndGet(-request.requestCount());
				idleCheck();
			});
	}

	private void idleCheck() {
		if (isIdle()) {
			log.trace("All requests completed. Sending idle event");
			vertx.eventBus().send(MeshEvent.SEARCH_IDLE.address, null);
			idling.onNext(dummyObject);
		} else {
			log.trace("Remaining: {} requests, {} transformations, bulking: {}",
				pendingRequests.get(), pendingTransformations.get(), bulker.bulking());
		}
	}

	private Flowable<SearchRequest> generateRequests(MessageEvent messageEvent) {
		try {
			return this.mainEventhandler.handle(messageEvent)
				.doOnNext(req -> pendingRequests.addAndGet(req.requestCount()))
				.doOnComplete(() -> {
					pendingTransformations.decrementAndGet();
					log.trace("Done transforming event {}. Transformations pending: {}", messageEvent.event, pendingTransformations);
					idleCheck();
				});
		} catch (Exception e) {
			// TODO Error handling
			e.printStackTrace();
			return Flowable.empty();
		}
	}


}
