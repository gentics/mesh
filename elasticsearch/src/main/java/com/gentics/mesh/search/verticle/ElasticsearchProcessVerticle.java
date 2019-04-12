package com.gentics.mesh.search.verticle;

import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.impl.ElasticsearchResponseErrorStreamable;
import com.gentics.mesh.search.verticle.eventhandler.MainEventHandler;
import com.gentics.mesh.search.verticle.eventhandler.SyncEventHandler;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.eventbus.MessageConsumer;

import javax.inject.Inject;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.gentics.mesh.core.rest.MeshEvent.INDEX_SYNC_REQUEST;
import static com.gentics.mesh.core.rest.MeshEvent.SEARCH_FLUSH_REQUEST;
import static com.gentics.mesh.search.verticle.eventhandler.RxUtil.retryWithDelay;

public class ElasticsearchProcessVerticle extends AbstractVerticle {
	private static final Logger log = LoggerFactory.getLogger(ElasticsearchProcessVerticle.class);

	private final MainEventHandler mainEventhandler;
	private final SearchProvider searchProvider;
	private final IdleChecker idleChecker;
	private final SyncEventHandler syncEventHandler;
	private final ElasticSearchOptions options;

	private Subject<MessageEvent> requests = PublishSubject.create();

	private List<MessageConsumer<JsonObject>> vertxHandlers;
	private final AtomicBoolean stopped = new AtomicBoolean(false);
	private final BehaviorSubject<Boolean> elasticsearchAvailable = BehaviorSubject.createDefault(true);
	private final AtomicBoolean waitForSync = new AtomicBoolean(false);

	@Inject
	public ElasticsearchProcessVerticle(MainEventHandler mainEventhandler,
										SearchProvider searchProvider,
										IdleChecker idleChecker,
										SyncEventHandler syncEventHandler,
										MeshOptions options) {
		this.mainEventhandler = mainEventhandler;
		this.searchProvider = searchProvider;
		this.idleChecker = idleChecker;
		this.syncEventHandler = syncEventHandler;
		this.options = options.getSearchOptions();
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
			.map(event -> vertx.eventBus().<JsonObject>localConsumer(event.address, message -> {
				if (!stopped.get() && !isDroppedEvent(message)) {
					idleChecker.incrementAndGetTransformations();
					elasticsearchAvailable.filter(available -> available)
						.firstOrError()
						.subscribe(ignore -> {
							waitForSync.set(false);
							log.trace(String.format("Received event message on address {%s}:\n%s", message.address(), message.body()));
							requests.onNext(new MessageEvent(event, MeshEventModel.fromMessage(message)));
						});
				}
			}))
			.map((Function<io.vertx.core.eventbus.MessageConsumer<JsonObject>, MessageConsumer<JsonObject>>) MessageConsumer::new)
			.collect(Collectors.toList());
		log.trace("Done Initializing Elasticsearch process verticle");
	}

	private boolean isDroppedEvent(Message<JsonObject> message) {
		return waitForSync.get() && !message.address().equals(INDEX_SYNC_REQUEST.address);
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
		BulkOperator bulker = new BulkOperator(vertx, Duration.ofMillis(options.getBulkDebounceTime()), options.getBulkLimit());
		requests.concatMap(event -> generateRequests(event).toObservable(), 1)
			.lift(bulker)
			.to(this::bufferRequests)
			.concatMap(this::sendRequest,1)
			// To make sure the subscription stays alive
			.onErrorResumeNext(Flowable.empty())
			.subscribe();
	}

	private Flowable<SearchRequest> bufferRequests(Observable<SearchRequest> upstream) {
		AtomicInteger bufferedRequests = new AtomicInteger(0);
		return upstream
			.doOnNext(request -> {
				int count = request.requestCount();
				bufferedRequests.addAndGet(count);
				idleChecker.addAndGetRequests(count);
			})
			.toFlowable(BackpressureStrategy.MISSING)
			.onBackpressureBuffer(
				options.getEventBufferSize(),
				() -> {
					log.info("Request buffer size of {} was reached. Dropping all pending requests and scheduling index sync.", options.getEventBufferSize());
					idleChecker.addAndGetRequests(-bufferedRequests.get());
					bufferedRequests.set(0);
					idleChecker.resetTransformations();
					startSync();
				}
		).retry(err -> err instanceof MissingBackpressureException)
		.doOnNext(request -> bufferedRequests.addAndGet(-request.requestCount()));
	}

	private void startSync() {
		waitForSync.set(true);
		elasticsearchAvailable.onNext(false);
		Observable.interval(options.getRetryInterval(), TimeUnit.MILLISECONDS)
			.flatMapSingle(ignore -> searchProvider.isAvailable())
			.filter(available -> available)
			.firstOrError()
			.subscribe(available -> {
				log.info("Elasticsearch is available again. Starting sync.");
				elasticsearchAvailable.onNext(available);
			});
		vertx.eventBus().publish(INDEX_SYNC_REQUEST.address, null);
	}

	private Flowable<SearchRequest> sendRequest(SearchRequest request) {
		return stopped.get()
			? Flowable.empty()
			: request.execute(searchProvider)
			.doOnSubscribe(ignore -> {
				log.trace("Sending request to Elasticsearch:\n" + request);
			})
			.doOnComplete(() -> log.trace("Request completed:\n" + request))
			.doOnError(err -> log.error("Error after sending request to Elasticsearch", err))
			.doFinally(() -> idleChecker.addAndGetRequests(-request.requestCount()))
			.andThen(Flowable.just(request))
			.onErrorResumeNext(this::syncIndices)
			.retryWhen(retryWithDelay(Duration.ofMillis(options.getRetryInterval())));
	}

	/**
	 * Invokes index sync if an index not found error has been encountered.
	 * @param error
	 * @return
	 */
	private Flowable<SearchRequest> syncIndices(Throwable error) {
		if (error instanceof ElasticsearchResponseErrorStreamable) {
			boolean indexNotFound = ((ElasticsearchResponseErrorStreamable) error).stream()
				.anyMatch(err -> "index_not_found_exception".equals(err.getType()));
			if (indexNotFound && !stopped.get()) {
				return syncEventHandler.generateSyncRequests()
					.concatMap(this::sendRequest, 1);
			}
		}
		return Flowable.error(error);
	}

	private Flowable<? extends SearchRequest> generateRequests(MessageEvent messageEvent) {
		if (stopped.get()) {
			return Flowable.empty();
		}
		try {
			return this.mainEventhandler.handle(messageEvent)
				.doOnNext(request -> {
					if (log.isTraceEnabled()) {
						log.trace(String.format("Generated request of class {%s}", request.getClass().getSimpleName()));
					}
				})
				.doOnError(err -> log.error("Error while transforming event", err))
				.doOnComplete(() -> log.trace("Done transforming event {}. Transformations pending: {}", messageEvent.event, idleChecker.getTransformations()))
				.doOnTerminate(idleChecker::decrementAndGetTransformations);
		} catch (Exception e) {
			// TODO Error handling
			e.printStackTrace();
			return Flowable.empty();
		}
	}


}
