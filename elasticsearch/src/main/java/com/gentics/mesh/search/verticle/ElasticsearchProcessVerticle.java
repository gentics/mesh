package com.gentics.mesh.search.verticle;

import static com.gentics.mesh.core.rest.MeshEvent.INDEX_CHECK_REQUEST;
import static com.gentics.mesh.core.rest.MeshEvent.INDEX_SYNC_REQUEST;
import static com.gentics.mesh.core.rest.MeshEvent.IS_SEARCH_IDLE;
import static com.gentics.mesh.core.rest.MeshEvent.SEARCH_FLUSH_REQUEST;
import static com.gentics.mesh.core.rest.MeshEvent.SEARCH_REFRESH_REQUEST;
import static com.gentics.mesh.search.verticle.eventhandler.RxUtil.retryWithDelay;
import static com.gentics.mesh.search.verticle.eventhandler.Util.logElasticSearchError;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.core.rest.event.search.SearchIndexSyncEventModel;
import com.gentics.mesh.distributed.RequestDelegator;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.impl.ElasticsearchResponseErrorStreamable;
import com.gentics.mesh.search.verticle.bulk.BulkOperator;
import com.gentics.mesh.search.verticle.eventhandler.MainEventHandler;
import com.gentics.mesh.search.verticle.eventhandler.SyncEventHandler;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.exceptions.MissingBackpressureException;
import io.reactivex.processors.FlowableProcessor;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.subjects.BehaviorSubject;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.eventbus.MessageConsumer;

/**
 * <p>
 * Listens to events that require a change in elasticsearch.
 * </p>
 * <p>
 * The basic flow of events can be found in the {@link #assemble()} method. It looks like this:
 * </p>
 * <ol>
 * <li>Event received</li>
 * <li>Generate necessary requests out of the event</li>
 * <li>Bulk bulkable requests together</li>
 * <li>Send request to elasticsearch</li>
 * </ol>
 */
public class ElasticsearchProcessVerticle extends AbstractVerticle {
	private static final Logger log = LoggerFactory.getLogger(ElasticsearchProcessVerticle.class);

	private final MainEventHandler mainEventhandler;
	private final SearchProvider searchProvider;
	private final IdleChecker idleChecker;
	private final SyncEventHandler syncEventHandler;
	private final ElasticSearchOptions options;
	private final RequestDelegator delegator;
	private final String nodeName;
	private final boolean clusteringEnabled;

	private FlowableProcessor<MessageEvent> requests = PublishProcessor.create();

	private List<MessageConsumer<JsonObject>> vertxHandlers;
	private final AtomicBoolean stopped = new AtomicBoolean(false);
	private final BehaviorSubject<Boolean> elasticsearchAvailable = BehaviorSubject.createDefault(true);
	private final AtomicBoolean waitForSync = new AtomicBoolean(false);

	@Inject
	public ElasticsearchProcessVerticle(MainEventHandler mainEventhandler,
		SearchProvider searchProvider,
		IdleChecker idleChecker,
		SyncEventHandler syncEventHandler,
										MeshOptions options,
										RequestDelegator delegator) {
		this.mainEventhandler = mainEventhandler;
		this.searchProvider = searchProvider;
		this.idleChecker = idleChecker;
		this.syncEventHandler = syncEventHandler;
		this.options = options.getSearchOptions();
		this.delegator = delegator;
		this.nodeName = options.getNodeName();
		this.clusteringEnabled = options.getClusterOptions().isEnabled();
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

		// Note: although the handler is registered as localConsumer, there is no guarantee that it will really only handle local messages
		// due to the vert.x bug https://github.com/eclipse-vertx/vert.x/issues/4116
		// therefore the message is only handled, if it passes the check implemented in {@link #isLocal()}, which will check for an additional
		// message header identifying the sender.
		vertxHandlers = mainEventhandler.handledEvents()
			.stream()
			.map(event -> vertx.eventBus().<JsonObject>localConsumer(event.address, message -> {
				if (!stopped.get() && !isDroppedEvent(message) && isLocal(message)) {
					idleChecker.incrementAndGetTransformations();
					// Only continue processing the event if elasticsearch is available.
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

		vertxHandlers.add(replyingEventHandler(IS_SEARCH_IDLE, Single.fromCallable(idleChecker::isIdle)));
		vertxHandlers.add(replyingEventHandler(SEARCH_REFRESH_REQUEST, refresh().andThen(Single.just(true))));

		if (options.getIndexCheckInterval() > 0) {
			log.trace("Setup periodic index check every {} ms", options.getIndexCheckInterval());
			// periodically send the event to check the indices
			vertx.setPeriodic(options.getIndexCheckInterval(), id -> {
				// only do this for the current master
				if (!clusteringEnabled || delegator.isMaster()) {
					vertx.eventBus().publish(INDEX_CHECK_REQUEST.address, null);
				}
			});
		} else {
			log.trace("Periodic index check disabled (interval set to {} ms)", options.getIndexCheckInterval());
		}

		log.trace("Done Initializing Elasticsearch process verticle");
	}

	/**
	 * Subscribe to the local event and add a rely handler which utilizes the provided single response.
	 * 
	 * @param event
	 *            Event to be subscribed to
	 * @param response
	 *            Reply to be send
	 * @return
	 */
	public MessageConsumer<JsonObject> replyingEventHandler(MeshEvent event, Single<?> response) {
		return new Vertx(vertx).eventBus().localConsumer(event.address, message -> response.subscribe(value -> message.reply(value)));
	}

	/**
	 * Tests if an event in a message should be ignore for further processing. Events will be ignored when an index sync has been requested but not yet started.
	 * Effectively this will ignore all events that occurred before the index sync request.
	 *
	 * @param message
	 * @return
	 */
	private boolean isDroppedEvent(Message<JsonObject> message) {
		return waitForSync.get() && !message.address().equals(INDEX_SYNC_REQUEST.address);
	}

	@Override
	public void stop(Promise<Void> promise) {
		log.trace("Stopping Elasticsearch process verticle");
		stopped.set(true);
		Observable.fromIterable(vertxHandlers)
			.flatMapCompletable(MessageConsumer::rxUnregister)
			.andThen(flush())
			.subscribe(() -> {
				requests.onComplete();
				idleChecker.close();
				log.trace("Done stopping Elasticsearch process verticle");
				promise.complete();
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
	 */
	public Completable refresh() {
		return searchProvider.refreshIndex()
			.doOnSubscribe(ignore -> log.trace("Refreshing all Elasticsearch indices..."))
			.doOnComplete(() -> log.trace("Refresh complete."));
	}

	/**
	 * Assembles the main Flowable through which all requests are processed.
	 */
	private void assemble() {
		BulkOperator bulker = new BulkOperator(vertx,
			Duration.ofMillis(options.getBulkDebounceTime()),
			options.getBulkLimit(),
			options.getBulkLengthLimit());
		requests
			.compose(this::bufferEvents)
			.concatMap(this::generateRequests, 1)
			.lift(bulker)
			.concatMap(request -> this.sendRequest(request)
				// To make sure the subscription stays alive
				.onErrorResumeNext(Flowable.empty()), 1)
			// To make sure the subscription stays alive
			.doOnError(err -> log.info("Error at end of ES process chain", err))
			.retry()
			.subscribe();
	}

	/**
	 * Buffers requests to elasticsearch when the requests to elasticsearch are slower than the flow of incoming events. If too many events are queued, the
	 * queue is cleared and an index sync will be requested.
	 *
	 * @see ElasticSearchOptions#getEventBufferSize()
	 * @param upstream
	 * @return
	 */
	private <T> Flowable<T> bufferEvents(Flowable<T> upstream) {
		AtomicInteger bufferedEvents = new AtomicInteger(0);
		return upstream
			.doOnNext(request -> {
				bufferedEvents.incrementAndGet();
			})
			.onBackpressureBuffer(
				options.getEventBufferSize(),
				() -> {
					log.info("Event buffer size of {} was reached. Dropping all pending events and scheduling index sync.",
						options.getEventBufferSize());
					bufferedEvents.set(0);
					idleChecker.resetTransformations();
					startSync();
				})
			.retry(err -> err instanceof MissingBackpressureException)
			.doOnNext(request -> bufferedEvents.decrementAndGet());
	}

	/**
	 * Waits until elasticsearch is reachable and then starts the syncing process.
	 */
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
		vertx.eventBus().publish(INDEX_SYNC_REQUEST.address, new JsonObject(JsonUtil.toJson(new SearchIndexSyncEventModel(), true)));
	}

	/**
	 * Sends a request to elasticsearch. Handles the following errors:
	 *
	 * <h2>index_not_found_exception</h2> Sync indices before any other event is processed.
	 *
	 * <h2>Connection errors</h2> The request will be retried indefinitely in a configurable interval.
	 *
	 * <h2>Errors inside elasticsearch</h2> These errors will not affect this verticle and will be loggend and then ignored.
	 *
	 * @param request
	 * @return
	 */
	private Flowable<SearchRequest> sendRequest(SearchRequest request) {
		return stopped.get()
			? Flowable.empty()
			: request.execute(searchProvider)
				.doOnSubscribe(ignore -> {
					log.trace("Sending request to Elasticsearch: {}", request);
				})
				.doOnComplete(() -> log.trace("Request completed: {}", request))
				.doOnError(err -> logElasticSearchError(err, () -> {
					log.error("Error for request: {}", request);
					log.error("Error after sending request to Elasticsearch", err);
				}))
				.andThen(Flowable.just(request))
				.onErrorResumeNext(ignoreDeleteOnMissingIndexError(request))
				.onErrorResumeNext(this::syncIndices)
				.onErrorResumeNext(ignoreElasticsearchErrors(request))
				.retryWhen(retryWithDelay(
					Duration.ofMillis(options.getRetryInterval()),
					options.getRetryLimit()))
				.doFinally(() -> {
					log.trace("Request-{}", request);
					idleChecker.addAndGetRequests(-request.requestCount());
				});
	}

	/**
	 * Ignores the error if there are only deletes on missing indices.
	 * 
	 * @param request
	 * @return
	 */
	private io.reactivex.functions.Function<Throwable, Flowable<SearchRequest>> ignoreDeleteOnMissingIndexError(SearchRequest request) {
		return error -> {
			if (error instanceof ElasticsearchResponseErrorStreamable) {
				return ((ElasticsearchResponseErrorStreamable) error).stream()
					// Filter out failing deletes on non existing indices
					.filter(err -> !("delete".equals(err.getActionType()) &&
						"index_not_found_exception".equals(err.getType())))
					.findAny()
					// If there are other errors left, throw
					.map(ignore -> Flowable.<SearchRequest>error(error))
					.orElseGet(() -> {
						log.info("Tried to delete document on missing index. This error will be ignored.");
						return Flowable.just(request);
					});
			} else {
				return Flowable.error(error);
			}
		};
	}

	/**
	 * Invokes index sync if an index not found error has been encountered.
	 * 
	 * @param error
	 * @return
	 */
	private Flowable<SearchRequest> syncIndices(Throwable error) {
		if (error instanceof ElasticsearchResponseErrorStreamable) {
			boolean indexNotFound = ((ElasticsearchResponseErrorStreamable) error).stream()
				.anyMatch(err -> "index_not_found_exception".equals(err.getType()));
			if (indexNotFound && !stopped.get()) {
				return syncEventHandler.generateSyncRequests(null)
					.doOnNext(request -> {
						log.trace("SyncRequest+{}", request);
						idleChecker.addAndGetRequests(request.requestCount());
					})
					.doOnSubscribe(ignore -> log.trace("Index not found. Resyncing."))
					.concatMap(this::sendRequest, 1);
			}
		}
		return Flowable.error(error);
	}

	/**
	 * Most errors inside elasticsearch are not recoverable by retrying. These errors will only be logged.
	 * 
	 * @param request
	 * @return
	 */
	private io.reactivex.functions.Function<Throwable, Flowable<SearchRequest>> ignoreElasticsearchErrors(SearchRequest request) {
		return error -> {
			if (error instanceof ElasticsearchResponseErrorStreamable) {
				log.error("Not retrying because it is an error inside elasticsearch.");
				return Flowable.just(request);
			} else {
				return Flowable.error(error);
			}
		};
	}

	/**
	 * Generate events out of the given event. The requests are not sent yet.
	 * 
	 * @param messageEvent
	 * @return
	 */
	private Flowable<? extends SearchRequest> generateRequests(MessageEvent messageEvent) {
		if (stopped.get()) {
			return Flowable.empty();
		}
		try {
			return this.mainEventhandler.handle(messageEvent)
				.doOnNext(request -> {
					if (log.isTraceEnabled()) {
						log.trace("Request+{}", request);
					}
					idleChecker.addAndGetRequests(request.requestCount());
				})
				.retryWhen(retryWithDelay(
					Duration.ofMillis(options.getRetryInterval()),
					options.getRetryLimit()))
				.doOnComplete(
					() -> log.trace("Done transforming event {}. Transformations pending: {}", messageEvent.event, idleChecker.getTransformations()))
				.doOnTerminate(idleChecker::decrementAndGetTransformations);
		} catch (Exception e) {
			// For safety to keep the verticle always running
			e.printStackTrace();
			return Flowable.empty();
		}
	}

	/**
	 * Check whether the message is a local message.
	 * The check will be done by comparing the message header {@link EventQueueBatch#SENDER_HEADER} with the node name
	 * If the header is not found, the message is assumed to be local (i.e. will be handled)
	 * @param message message to check
	 * @return true if the message is assumed to be a local one
	 */
	private boolean isLocal(Message<JsonObject> message) {
		if (message.headers().contains(EventQueueBatch.SENDER_HEADER)) {
			// if the message contains the header, it is local if the value is equal to the name of this mesh instance
			return Objects.equals(message.headers().get(EventQueueBatch.SENDER_HEADER), nodeName);
		} else {
			// if the message does not contain the header, it is assumed to be local
			return true;
		}
	}

	/**
	 * Returns the idle checker for this verticle.
	 * 
	 * @return
	 */
	public IdleChecker getIdleChecker() {
		return idleChecker;
	}
}
