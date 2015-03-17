package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.core.data.model.auth.PermissionType.READ;
import static com.gentics.cailun.core.data.model.auth.PermissionType.UPDATE;
import static com.gentics.cailun.util.JsonUtils.fromJson;
import static com.gentics.cailun.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.ext.apex.core.Route;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCoreApiVerticle;
import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.service.ObjectSchemaService;
import com.gentics.cailun.core.data.service.ProjectService;
import com.gentics.cailun.core.rest.common.response.GenericMessageResponse;
import com.gentics.cailun.core.rest.schema.request.ObjectSchemaCreateRequest;
import com.gentics.cailun.core.rest.schema.request.ObjectSchemaUpdateRequest;
import com.gentics.cailun.core.rest.schema.response.ObjectSchemaResponse;
import com.gentics.cailun.error.EntityNotFoundException;
import com.gentics.cailun.error.HttpStatusCodeErrorException;

@Component
@Scope("singleton")
@SpringVerticle
public class ObjectSchemaVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private ObjectSchemaService schemaService;

	@Autowired
	private ProjectService projectService;

	protected ObjectSchemaVerticle() {
		super("schemas");
	}

	@Override
	public void registerEndPoints() throws Exception {
		addCRUDHandlers();
	}

	private void addCRUDHandlers() {

		addCreateHandler();
		addReadHandlers();
		addUpdateHandler();
		addDeleteHandler();

		addSchemaProjectHandlers();

	}

	private void addSchemaProjectHandlers() {
		// TODO consumes, produces, document needed permissions?
		Route route = route("/:schemaUUid/projects/:projectUuid").method(POST);
		route.handler(rc -> {
			String schemaUuid = rc.request().params().get("schemaUUid");
			String projectUuid = rc.request().params().get("projectUuid");

			ObjectSchema schema = schemaService.findByUUID(schemaUuid);
			if (schema == null) {
				throw new EntityNotFoundException(i18n.get(rc, "schema_not_found", schemaUuid));
			}
			failOnMissingPermission(rc, schema, PermissionType.READ);

			Project project = projectService.findByUUID(projectUuid);
			if (project == null) {
				throw new EntityNotFoundException(i18n.get(rc, "project_not_found", projectUuid));
			}
			failOnMissingPermission(rc, schema, PermissionType.UPDATE);

			if (schema.addProject(project)) {
				schema = schemaService.save(schema);
				rc.response().setStatusCode(200);
				// TODO return updated schema?
				rc.response().end(
						toJson(new GenericMessageResponse("Added schema with uuid {" + schemaUuid + "} to project with uuid {" + projectUuid + "}")));
			} else {
				// TODO 200?
			}
		});

		route = route("/:schemaUUid/projects/:projectUuid").method(DELETE);
		route.handler(rc -> {
			String schemaUuid = rc.request().params().get("schemaUUid");
			String projectUuid = rc.request().params().get("projectUuid");

			ObjectSchema schema = schemaService.findByUUID(schemaUuid);
			if (schema == null) {
				throw new EntityNotFoundException(i18n.get(rc, "schema_not_found", schemaUuid));
			}
			failOnMissingPermission(rc, schema, PermissionType.READ);

			Project project = projectService.findByUUID(projectUuid);
			if (project == null) {
				throw new EntityNotFoundException(i18n.get(rc, "project_not_found", projectUuid));
			}
			failOnMissingPermission(rc, schema, PermissionType.UPDATE);

			if (schema.removeProject(project)) {
				schema = schemaService.save(schema);
				rc.response().setStatusCode(200);
				// TODO return updated schema?
				rc.response().end(
						toJson(new GenericMessageResponse("Removed schema with uuid {" + schemaUuid + "} from project with uuid {" + projectUuid
								+ "}")));
			} else {
				// TODO - 200?
			}

		});
	}

	private void addCreateHandler() {
		// TODO add consumes, produces
		Route route = route("/").method(POST);
		route.handler(rc -> {
			ObjectSchemaCreateRequest requestModel = fromJson(rc, ObjectSchemaCreateRequest.class);
			if (StringUtils.isEmpty(requestModel.getName())) {
				// TODO i18n
				throw new HttpStatusCodeErrorException(400, "The name of a schema is mandatory and can't be omitted.");
			}
			ObjectSchema schema = new ObjectSchema(requestModel.getName());
			schemaService.save(schema);
			rc.response().setStatusCode(200);
			rc.response().end(toJson(schemaService.transformToRest(schema)));
		});

	}

	private void addUpdateHandler() {
		// TODO consumes, produces
		Route route = route("/:uuid").method(PUT);
		route.handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			ObjectSchema schema = schemaService.findByUUID(uuid);
			if (schema == null) {
				throw new EntityNotFoundException(i18n.get(rc, "schema_not_found", uuid));
			}
			failOnMissingPermission(rc, schema, UPDATE);
			ObjectSchemaUpdateRequest requestModel = fromJson(rc, ObjectSchemaUpdateRequest.class);

			// Update name
			if (StringUtils.isEmpty(requestModel.getName())) {
				// TODO i18n
				throw new HttpStatusCodeErrorException(400, "A name must be set.");
			}
			if (!schema.getName().equals(requestModel.getName())) {
				schema.setName(requestModel.getName());
			}

			// Update description
			if (!schema.getDescription().equals(requestModel.getDescription())) {
				schema.setDescription(requestModel.getDescription());
			}

			schema = schemaService.save(schema);

			// TODO update modification timestamps
			rc.response().setStatusCode(200);
			rc.response().end(toJson(schemaService.transformToRest(schema)));

		});
	}

	private void addDeleteHandler() {
		// TODO consumes, produces
		Route route = route("/:uuid").method(DELETE);
		route.handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			ObjectSchema schema = schemaService.findByUUID(uuid);
			if (schema == null) {
				throw new EntityNotFoundException(i18n.get(rc, "schema_not_found", uuid));
			}
			failOnMissingPermission(rc, schema, PermissionType.DELETE);
			schemaService.delete(schema);
			rc.response().setStatusCode(200);
			rc.response().end(toJson(new GenericMessageResponse("schema with uuid \"+uuid+\" deleted")));
		});

	}

	private void addReadHandlers() {
		// produces(APPLICATION_JSON)
		route("/").method(GET).handler(rc -> {
			// TODO handle paging
				Iterable<ObjectSchema> schemas = schemaService.findAll();
				Map<String, ObjectSchemaResponse> resultMap = new HashMap<>();
				if (schemas == null) {
					rc.response().end(toJson(resultMap));
					return;
				}
				for (ObjectSchema schema : schemas) {
					ObjectSchemaResponse restSchema = schemaService.transformToRest(schema);
					resultMap.put(schema.getName(), restSchema);
				}
				rc.response().end(toJson(resultMap));
			});

		route("/:uuid").method(GET).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			ObjectSchema schema = schemaService.findByUUID(uuid);
			if (schema == null) {
				throw new EntityNotFoundException(i18n.get(rc, "schema_not_found", uuid));
			}

			failOnMissingPermission(rc, schema, READ);
			rc.response().setStatusCode(200);
			rc.response().end(toJson(schema));
		});

	}

}
