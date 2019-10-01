package com.gentics.mesh.plugin.comment;

import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.plugin.comment.config.Configuration;
import com.gentics.mesh.plugin.comment.dagger.AdminClient;
import com.gentics.mesh.plugin.comment.model.CommentPluginSchema;
import com.gentics.mesh.plugin.comment.service.SchemaService;
import com.gentics.mesh.rest.client.MeshRestClient;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.inject.Inject;

public class SchemaInitializer {
	private final MeshRestClient adminClient;
	private final Configuration configuration;
	private final SchemaService schemaService;
	private final Resources resources;

	private static final Logger log = LoggerFactory.getLogger(SchemaInitializer.class);

	@Inject
	public SchemaInitializer(@AdminClient MeshRestClient adminClient, Configuration configuration, SchemaService schemaService, Resources resources) {
		this.adminClient = adminClient;
		this.configuration = configuration;
		this.schemaService = schemaService;
		this.resources = resources;
	}

	/**
	 * Tests if the necessary schemas for this plugin are existing in Mesh.
	 * If not, the missing schemas will be created and assigned to the project.
	 */
	public Completable initialize() {
		return ensureSchemas()
			.flatMapCompletable(this::assignToProjects)
			.doOnSubscribe(ignore -> log.info("Initializing schemas"));
	}

	/**
	 * Makes sure that all necessary schemas exist and returns the responses.
	 */
	private Observable<SchemaResponse> ensureSchemas() {
		return adminClient.findSchemas().toSingle().flatMapObservable(schemas ->
			Observable.fromArray(CommentPluginSchema.values())
				.flatMapSingle(schema -> ensureSchema(schemas, schema))
		);
	}

	/**
	 * Assigns a schema to all configured projects.
	 */
	private Completable assignToProjects(SchemaResponse schema) {
		// TODO Maybe performance could be gained by checking if the assignment exists beforehand
		return Observable.fromIterable(configuration.getProjectNames())
			.flatMapCompletable(name -> adminClient.assignSchemaToProject(name, schema.getUuid()).toCompletable()
				.doOnComplete(() -> log.info(String.format("Assigned schema %s to project %s", schema.getName(), name)))
			);
	}

	/**
	 * Tests if the given schema exists. If not, it is created.
	 */
	private Single<SchemaResponse> ensureSchema(SchemaListResponse existingSchemas, CommentPluginSchema schema) {
		return existingSchemas.getData().stream()
			.filter(existingSchema -> existingSchema.getName().equals(schemaService.getMeshSchemaName(schema)))
			.findAny()
			.map(Single::just)
			.orElseGet(() -> createSchema(schema));
	}

	/**
	 * Creates a schema by loading the json from the resources.
	 */
	private Single<SchemaResponse> createSchema(CommentPluginSchema schema) {
		SchemaCreateRequest request = resources.loadSchema(schema);
		log.info("Creating schema " + request.getName());
		return adminClient.createSchema(request).toSingle()
			.doOnSuccess(response -> log.info(String.format("Created schema %s with uuid %s", response.getName(), response.getUuid())));
	}


}
