package com.gentics.mesh.plugin.forms.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.core.rest.admin.cluster.coordinator.CoordinatorMasterResponse;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.forms.FormsPlugin;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;

/**
 * Service that provides methods for purging stale data nodes and old downloads
 */
@Singleton
public class PurgeService {
	private static final Logger log = LoggerFactory.getLogger(PurgeService.class);

	private final MeshRestClient adminClient;
	private final Vertx rxVertx;
	private final StatusService statusService;
	private final FormService formService;

	private final int downloadPurgeInterval;
	private final int staleDataPurgeInterval;

	@Inject
	public PurgeService(
			@AdminClient MeshRestClient adminClient,
			io.vertx.core.Vertx vertx,
			StatusService statusService,
			FormService formService,
			FormsConfig config) {
		this.adminClient = adminClient;
		this.rxVertx = Vertx.newInstance(vertx);
		this.statusService = statusService;
		this.formService = formService;
		this.downloadPurgeInterval = config.getDownloadPurgeInterval();
		this.staleDataPurgeInterval = config.getStaleDataPurgeInterval();
	}

	/**
	 * Initialize the service. This will schedule purge handlers for all projects, for which the plugin is activated.
	 * @return completable
	 */
	public Completable initialize() {
		return Completable.fromRunnable(() -> {
			if (downloadPurgeInterval <= 0 && staleDataPurgeInterval <= 0) {
				log.warn("All purge intervals are less than 1, purge disabled");
			} else {
				Handler<Long> purgeHandler = purgeHandler();

				log.info("Scheduling periodic purge");
				// Start the first purge in a minute to make sure the purge is started at least once after startup  ...
				rxVertx.setTimer(Duration.of(1, ChronoUnit.MINUTES).toMillis(), purgeHandler);
				// ... and schedule one purge per day.
				rxVertx.setPeriodic(Duration.of(1, ChronoUnit.DAYS).toMillis(), purgeHandler);
			}
		});
	}

	/**
	 * Create a handler for {@link Vertx#setTimer(long, Handler)} or
	 * {@link Vertx#setPeriodic(long, Handler)} that will delete old download
	 * nodes and/or stale data nodes
	 *
	 * @return A timer/periodic handler that will delete old downloads and/or stale data nodes
	 */
	private Handler<Long> purgeHandler() {
		return timerId -> {
			shouldSkipPurge().flatMapCompletable(skip -> {
				if (skip) {
					return Completable.complete()
						.doOnSubscribe(disp -> log.info("Skip purging, because currently not the master"));
				} else {
					return statusService.projectsWithPluginActive().toFlowable(BackpressureStrategy.BUFFER)
						.flatMapCompletable(response -> purge(response.getName()), false, 1)
						.doOnSubscribe(disp -> log.info("Starting periodic purge"))
						.doFinally(() -> log.info("Finished periodic purge"));
				}
			}).subscribe();
		};
	}

	/**
	 * Return a completable which will do the necessary purges for the project with given name when subscribed
	 * @param projectName project name
	 * @return purge completable
	 */
	private Completable purge(String projectName) {
		Completable allJobs = Completable.complete();

		if (downloadPurgeInterval > 0) {
			allJobs = allJobs.andThen(purgeDownloads(projectName));
			log.info("Purging downloads for project {}", projectName);
		}
		if (staleDataPurgeInterval > 0) {
			ZonedDateTime purgeThreshold = ZonedDateTime.now(ZoneOffset.UTC)
					.minus(Duration.ofDays(staleDataPurgeInterval))
					.truncatedTo(ChronoUnit.DAYS);
			allJobs = allJobs.andThen(purgeAllStaleData(projectName, purgeThreshold, 100));
			log.info("Purging stale data nodes for project {}", projectName);
		}

		return allJobs;
	}

	/**
	 * Purge all stale data nodes for the given project
	 * @param projectName project name
	 * @param purgeThreshold purge threshold (everything which was created before that date/time and contains an error is eligible for purging)
	 * @param perPage number of nodes to be fetched per page
	 * @return completable
	 */
	public Completable purgeAllStaleData(String projectName, ZonedDateTime purgeThreshold, int perPage) {
		return formService.listUuids(projectName)
				.toFlowable(BackpressureStrategy.BUFFER)
				.map(formUuid -> formService.getSchemaName(formUuid))
				.flatMapCompletable(schemaName -> purgeStaleNodes(projectName,
						schemaName, purgeThreshold, perPage), false, 1);
	}

	/**
	 * Delete all download nodes (schema {@code form_download}) which are
	 * older than the {@link #downloadPurgeInterval} days.
	 *
	 * @param projectName The project to purge.
	 */
	private Completable purgeDownloads(String projectName) {
		String graphQlQuery;
		String purgeThreshold = ZonedDateTime.now(ZoneOffset.UTC)
			.minus(Duration.ofDays(downloadPurgeInterval))
			.truncatedTo(ChronoUnit.DAYS)
			.format(DateTimeFormatter.ISO_DATE_TIME);
		JsonObject graphQlVariables = new JsonObject().put("purgeThreshold", purgeThreshold);

		try {
			graphQlQuery = IOUtils.toString(FormsPlugin.class.getResource("findFormDownloadsToPurge.graphql"), StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.error("Could not load GraphQL query for download purge: {}", e, e.getMessage());

			return Completable.complete();
		}

		GraphQLRequest request = new GraphQLRequest().setQuery(graphQlQuery).setVariables(graphQlVariables);

		return adminClient.graphql(projectName, request).toSingle()
			.flatMapCompletable(rs -> {
				Optional<JsonObject> nodes = Optional.ofNullable(rs.getData())
					.map(data -> data.getJsonObject("schema"))
					.map(schema -> schema.getJsonObject("nodes"));
				Optional<JsonArray> elements = nodes.map(n -> n.getJsonArray("elements")).filter(ar -> !ar.isEmpty());

				if (!elements.isPresent()) {
					log.debug("No download nodes to purge");

					return Completable.complete();
				}

				return purgeElements(projectName, elements.get(), "download", new AtomicLong())
					.andThen(purgeNextDownloadsPage(projectName, nodes.get().getBoolean("hasNextPage")));
			})
			.doOnError(cause -> log.error("Error while purging old downloads: {}", cause.getMessage()));
	}

	/**
	 * Delete the given nodes from the given project.
	 *
	 * <p>
	 * The JSON array {@code objects} must contain JSON objects with a
	 * {@code uuid} field.
	 * </p>
	 *
	 * @param projectName The project to purge.
	 * @param objects The nodes to purge.
	 * @param description description of the objects (for logging only)
	 * @param counter for counting the purged elements
	 * @return A {@code Completable} that completes when all nodes are deleted.
	 */
	public Completable purgeElements(String projectName, JsonArray objects, String description, AtomicLong counter) {
		return Flowable.fromIterable(objects)
			.doOnSubscribe(ignored -> log.info("Purging {} old {} node(s)", objects.size(), description))
			.cast(JsonObject.class)
			.map(obj -> obj.getString("uuid"))
			.flatMapCompletable(uuid -> adminClient.deleteNode(projectName, uuid).toCompletable().doOnComplete(() -> counter.incrementAndGet()).onErrorComplete(e -> {
				if ((MeshRestClientMessageException.class.isInstance(e) && MeshRestClientMessageException.class.cast(e).getStatusCode() == HttpStatus.SC_NOT_FOUND) 
						|| (MeshRestClientMessageException.class.isInstance(e.getCause()) && MeshRestClientMessageException.class.cast(e.getCause()).getStatusCode() == HttpStatus.SC_NOT_FOUND)
						|| (GenericRestException.class.isInstance(e) && GenericRestException.class.cast(e).getStatus() == HttpResponseStatus.NOT_FOUND)
						|| (GenericRestException.class.isInstance(e.getCause()) && GenericRestException.class.cast(e.getCause()).getStatus() == HttpResponseStatus.NOT_FOUND)) {
					log.info("Node `" + projectName + "/" + uuid + "` is already deleted");
					return true;
				} else {
					return false;
				}
			}), false, 1);
	}

	/**
	 * Call {@link #purgeDownloads(String)} for the given project again, if
	 * there are still elements left.
	 *
	 * @param projectName The project to purge.
	 * @param hasNextPage Whether there are still elements left to delete.
	 * @return A {@code Completable} that completes when all remaining nodes are deleted.
	 */
	private Completable purgeNextDownloadsPage(String projectName, boolean hasNextPage) {
		return Completable.fromAction(() -> {
			if (hasNextPage) {
				purgeDownloads(projectName);
			}
		});
	}

	/**
	 * Delete all stale nodes which are
	 * older than the staleDataPurgeInterval days.
	 *
	 * @param projectName The project to purge.
	 * @param schemaName name of the schema
	 * @param purgeThreshold threshold
	 * @param perPage
	 */
	public Completable purgeStaleNodes(String projectName, String schemaName, ZonedDateTime purgeThreshold, int perPage) {
		String rawGraphQlQuery, graphQlQuery;
		JsonObject graphQlVariables = new JsonObject()
				.put("purgeThreshold", purgeThreshold.format(DateTimeFormatter.ISO_DATE_TIME))
				.put("schemaName", schemaName)
				.put("perPage", perPage);

		try {
			rawGraphQlQuery = IOUtils.toString(FormsPlugin.class.getResource("findStaleFormDataNodesToPurge.graphql"), StandardCharsets.UTF_8);
			Map<String, String> context = new HashMap<>();
			context.put("schemaName", schemaName);
			graphQlQuery = new Handlebars().compileInline(rawGraphQlQuery).apply(Context.newContext(context));
		} catch (IOException e) {
			log.error("Could not load GraphQL query for stale node purge: {}", e, e.getMessage());

			return Completable.complete();
		}

		GraphQLRequest request = new GraphQLRequest().setQuery(graphQlQuery).setVariables(graphQlVariables);

		AtomicBoolean hasNext = new AtomicBoolean(false);
		AtomicBoolean firstPage = new AtomicBoolean(true);
		AtomicLong start = new AtomicLong();
		AtomicLong counter = new AtomicLong();

		return adminClient.graphql(projectName, request).toSingle()
			.flatMapCompletable(rs -> {
				Optional<JsonObject> nodes = Optional.ofNullable(rs.getData())
					.map(data -> data.getJsonObject("schema"))
					.map(schema -> schema.getJsonObject("nodes"));
				if (!nodes.isPresent()) {
					log.warn("Could not get nodes for schema {}. Maybe schema does not exist.", schemaName);
					hasNext.set(false);
					return Completable.complete();
				}
				Optional<JsonArray> elements = nodes.map(n -> n.getJsonArray("elements")).filter(ar -> !ar.isEmpty());

				hasNext.set(nodes.get().getBoolean("hasNextPage"));

				if (!elements.isPresent()) {
					log.debug("No download nodes to purge");

					hasNext.set(false);
					return Completable.complete();
				}

				return purgeElements(projectName, elements.get(), "stale data", counter);
			})
			.doOnError(cause -> log.error("Error while purging old downloads: {}", cause.getMessage()))
			.doOnSubscribe(disp -> {
				if (firstPage.getAndSet(false)) {
					log.info("Start purging stale data nodes with schema {}", schemaName);
					start.set(System.currentTimeMillis());
				}
			})
			.repeatUntil(() -> !hasNext.get())
			.doOnComplete(() -> {
				long duration = System.currentTimeMillis() - start.get();
				log.info("Finished purging stale data nodes with schema {}. Purged {} nodes in {} ms", schemaName, counter.get(), duration);
			});
	}

	/**
	 * Check whether purging should be skipped
	 * @return single boolean which emits true when purging shall be skipped
	 */
	protected Single<Boolean> shouldSkipPurge() {
		Single<CoordinatorMasterResponse> coordinationMasterSingle = adminClient.loadCoordinationMaster().toSingle();
		Single<MeshServerInfoModel> serverInfoSingle = adminClient.getApiInfo().toSingle();

		return Single
				.zip(coordinationMasterSingle, serverInfoSingle,
						(coordMaster, serverInfo) -> coordMaster.getName() != null
								&& !StringUtils.equals(coordMaster.getName(), serverInfo.getMeshNodeName()))
				.onErrorReturnItem(false);
	}
}
