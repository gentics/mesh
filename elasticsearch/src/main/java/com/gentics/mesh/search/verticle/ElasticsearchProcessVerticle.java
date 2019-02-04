package com.gentics.mesh.search.verticle;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.search.impl.SearchClient;
import com.gentics.mesh.search.verticle.request.ElasticSearchRequest;
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

	private final Eventhandler eventhandler;
	private final SearchClient elasticSearchClient;
	private BulkOperator bulker;

	private Subject<MessageEvent> requests = PublishSubject.create();
	private Subject<Object> idling = PublishSubject.create();
	private static final Object dummyObject = new Object();
	private AtomicInteger pendingRequests = new AtomicInteger();
	private AtomicInteger pendingTransformations = new AtomicInteger();
	private List<MessageConsumer<JsonObject>> vertxHandlers;

	@Inject
	public ElasticsearchProcessVerticle(Eventhandler eventhandler, SearchClient elasticSearchClient) {
		this.eventhandler = eventhandler;
		this.elasticSearchClient = elasticSearchClient;
	}

	@Override
	public void start() {
		log.trace("Initializing ElasticSearch process verticle");
		assemble();

		vertxHandlers = eventhandler.getHandledEvents()
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
	 * Flushes the buffer of elastic search requests and dispatches all pending requests.
	 */
	public Completable flush() {
		return Completable.defer(() -> {
			if (bulker != null) {
				bulker.flush();
			}
			return idling.firstOrError().toCompletable();
		});
	}

	/**
	 * Refreshes the ElasticSearch indices so that all changes are readable
	 * @return
	 */
	public Completable refresh() {
		return elasticSearchClient.refresh().async()
			.doOnSuccess(response -> {
				if (log.isTraceEnabled()) {
					log.trace("Refreshing response from ElasticSearch:\n" + response.encodePrettily());
				}
			}).toCompletable()
			.doOnSubscribe(ignore -> log.trace("Refreshing all ElasticSearch indices..."));
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
		bulker = new BulkOperator(vertx, Duration.ofSeconds(1), 1000);
		requests.toFlowable(BackpressureStrategy.MISSING)
			.onBackpressureBuffer(1000)
			.concatMap(this::generateRequests, 1)
			.doOnNext(request -> {
				if (log.isTraceEnabled()) {
					log.trace(String.format("Generated request with class {%s}", request.getClass().getSimpleName()));
				}
			})
			.toObservable()
			.lift(bulker)
			.doOnNext(ignore -> pendingRequests.incrementAndGet())
			.concatMap(request ->
				request.execute(elasticSearchClient).andThen(Observable.just(request))
					.doOnSubscribe(ignore -> log.trace("Sending request to ElasticSearch:\n" + request)),
				1
			)
			.subscribe(request -> {
				log.trace("Request completed:\n" + request);
				pendingRequests.decrementAndGet();
				if (isIdle()) {
					log.trace("All requests completed. Sending idle event");
					vertx.eventBus().send(MeshEvent.SEARCH_IDLE.address, null);
					idling.onNext(dummyObject);
				}
			});
	}

	private Flowable<ElasticSearchRequest> generateRequests(MessageEvent messageEvent) {
		try {
			List<ElasticSearchRequest> esRequests = this.eventhandler.handle(messageEvent);
			pendingTransformations.decrementAndGet();
			return Flowable.fromIterable(esRequests);
		} catch (Exception e) {
			// TODO Error handling
			e.printStackTrace();
			return Flowable.empty();
		}
	}
}
