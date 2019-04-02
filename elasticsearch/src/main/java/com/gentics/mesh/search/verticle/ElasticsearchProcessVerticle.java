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

import static com.gentics.mesh.core.rest.MeshEvent.SEARCH_FLUSH_REQUEST;
import static com.gentics.mesh.search.verticle.eventhandler.Util.dummyObject;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ElasticsearchProcessVerticle extends AbstractVerticle {
	private static final Logger log = LoggerFactory.getLogger(ElasticsearchProcessVerticle.class);

	private final MainEventHandler mainEventhandler;
	private final SearchProvider searchProvider;

	private Subject<MessageEvent> requests = PublishSubject.create();

	private List<MessageConsumer<JsonObject>> vertxHandlers;
	private final AtomicBoolean stopped = new AtomicBoolean(false);
	private final IdleChecker idleChecker;

	@Inject
	public ElasticsearchProcessVerticle(MainEventHandler mainEventhandler, SearchProvider searchProvider, IdleChecker idleChecker) {
		this.mainEventhandler = mainEventhandler;
		this.searchProvider = searchProvider;
		this.idleChecker = idleChecker;
	}

	@Override
	public void start() {
		log.trace("Initializing Elasticsearch process verticle");
		assemble();
		idleChecker.idling()
			.subscribe(ignore -> {
				log.trace("All requests completed. Sending idle event");
				vertx.eventBus().publish(MeshEvent.SEARCH_IDLE.address, null);
			});

		vertxHandlers = mainEventhandler.handledEvents()
			.stream()
			.map(event -> vertx.eventBus().<JsonObject>consumer(event.address, message -> {
				if (!stopped.get()) {
					idleChecker.incrementAndGetTransformations();
					log.trace(String.format("Received event message on address {%s}:\n%s", message.address(), message.body()));
					requests.onNext(new MessageEvent(event, MeshEventModel.fromMessage(message)));
				}
			}))
			.map((Function<io.vertx.core.eventbus.MessageConsumer<JsonObject>, MessageConsumer<JsonObject>>) MessageConsumer::new)
			.collect(Collectors.toList());
		log.trace("Done Initializing Elasticsearch process verticle");
	}

	@Override
	public void stop(Future<Void> stopFuture) {
		log.trace("Stopping Elasticsearch process verticle");
		stopped.set(true);
		Observable.fromIterable(vertxHandlers)
			.flatMapCompletable(MessageConsumer::rxUnregister)
			.andThen(flush())
			.subscribe(() -> {
				requests.onComplete();
				idleChecker.close();
				log.trace("Done stopping Elasticsearch process verticle");
				stopFuture.complete();
			});
	}

	/**
	 * Flushes the buffer of Elasticsearch requests and dispatches all pending requests.
	 */
	public Completable flush() {
		return Completable.fromRunnable(() -> vertx.eventBus().publish(SEARCH_FLUSH_REQUEST.address, null));
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

	private void assemble() {
		// TODO Make bulk operator options configurable
		BulkOperator bulker = new BulkOperator(vertx, Duration.ofSeconds(500), 1000);
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
				stopped.get()
				? Observable.empty()
				: request.execute(searchProvider).andThen(Observable.just(request))
					.doOnSubscribe(ignore -> log.trace("Sending request to Elasticsearch:\n" + request)),
				1
			)
			.subscribe(request -> {
				log.trace("Request completed:\n" + request);
				idleChecker.addAndGetRequests(-request.requestCount());
			});
	}

	private Flowable<? extends SearchRequest> generateRequests(MessageEvent messageEvent) {
		if (stopped.get()) {
			return Flowable.empty();
		}
		try {
			return this.mainEventhandler.handle(messageEvent)
				.doOnNext(req -> {
					log.trace("Incoming request");
					idleChecker.addAndGetRequests(req.requestCount());
				})
				.doOnComplete(() -> {
					idleChecker.decrementAndGetTransformations();
					log.trace("Done transforming event {}. Transformations pending: {}", messageEvent.event, idleChecker.getTransformations());
				});
		} catch (Exception e) {
			// TODO Error handling
			e.printStackTrace();
			return Flowable.empty();
		}
	}


}
