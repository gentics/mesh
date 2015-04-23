package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.fromJson;
import static com.gentics.cailun.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.AsyncResult;
import io.vertx.ext.apex.Route;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.Transaction;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCoreApiVerticle;
import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.PropertyType;
import com.gentics.cailun.core.data.model.PropertyTypeSchema;
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.rest.common.response.GenericMessageResponse;
import com.gentics.cailun.core.rest.schema.request.ObjectSchemaCreateRequest;
import com.gentics.cailun.core.rest.schema.request.ObjectSchemaUpdateRequest;
import com.gentics.cailun.core.rest.schema.response.ObjectSchemaListResponse;
import com.gentics.cailun.core.rest.schema.response.PropertyTypeSchemaResponse;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
import com.gentics.cailun.paging.PagingInfo;
import com.gentics.cailun.util.RestModelPagingHelper;

@Component
@Scope("singleton")
@SpringVerticle
public class ObjectSchemaVerticle extends AbstractCoreApiVerticle {

	protected ObjectSchemaVerticle() {
		super("schemas");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addSchemaProjectHandlers();

		addCreateHandler();
		addReadHandlers();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addSchemaProjectHandlers() {
		Route route = route("/:schemaUuid/projects/:projectUuid").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			loadObject(rc, "projectUuid", PermissionType.UPDATE, (AsyncResult<Project> rh) -> {
				loadObject(rc, "schemaUuid", PermissionType.READ, (AsyncResult<ObjectSchema> srh) -> {
					Project project = rh.result();
					ObjectSchema schema = srh.result();
					if (schema.addProject(project)) {
						schema = schemaService.save(schema);
					}
					rc.response().setStatusCode(200).end(toJson(schemaService.transformToRest(schema)));

				});
			});

		});

		route = route("/:schemaUuid/projects/:projectUuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {

			loadObject(rc, "projectUuid", PermissionType.UPDATE, (AsyncResult<Project> rh) -> {
				loadObject(rc, "schemaUuid", PermissionType.READ, (AsyncResult<ObjectSchema> srh) -> {
					ObjectSchema schema = srh.result();
					Project project = rh.result();
					if (schema.removeProject(project)) {
						schema = schemaService.save(schema);
					}

					rc.response().setStatusCode(200).end(toJson(schemaService.transformToRest(schema)));
				});

			});
		});
	}

	// TODO set creator
	private void addCreateHandler() {
		Route route = route("/").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {

			ObjectSchemaCreateRequest requestModel = fromJson(rc, ObjectSchemaCreateRequest.class);
			if (StringUtils.isEmpty(requestModel.getName())) {
				// TODO i18n
				rc.fail(new HttpStatusCodeErrorException(400, "The name of a schema is mandatory and cannot be omitted."));
			}

			if (StringUtils.isEmpty(requestModel.getProjectUuid())) {
				// TODO i18n
				rc.fail(new HttpStatusCodeErrorException(400, "The project uuid is mandatory for schema creation and cannot be omitted."));
			}

			loadObjectByUuid(rc, requestModel.getProjectUuid(), PermissionType.CREATE, (AsyncResult<Project> srh) -> {
				Project project = srh.result();

				ObjectSchema schema = new ObjectSchema(requestModel.getName());
				schema.setDescription(requestModel.getDescription());
				schema.setDisplayName(requestModel.getDisplayName());

				for (PropertyTypeSchemaResponse restPropSchema : requestModel.getPropertyTypeSchemas()) {
					// TODO validate field?
					PropertyTypeSchema propSchema = new PropertyTypeSchema();
					propSchema.setDescription(restPropSchema.getDesciption());
					propSchema.setKey(restPropSchema.getKey());
					PropertyType type = PropertyType.valueOfName(restPropSchema.getType());
					propSchema.setType(type);
					schema.addPropertyTypeSchema(propSchema);
				}
				schema.addProject(project);
				schema = schemaService.save(schema);

				roleService.addCRUDPermissionOnRole(rc, new CaiLunPermission(project, PermissionType.CREATE), schema);

				rc.response().setStatusCode(200).end(toJson(schemaService.transformToRest(schema)));
			});
		});

	}

	private void addUpdateHandler() {
		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			loadObject(rc, "uuid", PermissionType.UPDATE, (AsyncResult<ObjectSchema> srh) -> {
				ObjectSchema schema = srh.result();
				ObjectSchemaUpdateRequest requestModel = fromJson(rc, ObjectSchemaUpdateRequest.class);

				if (StringUtils.isEmpty(requestModel.getName())) {
					rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_name_must_be_set")));
				}
				if (!schema.getName().equals(requestModel.getName())) {
					schema.setName(requestModel.getName());
				}

				// Update description
					if (schema.getDescription() != null && (!schema.getDescription().equals(requestModel.getDescription()))) {
						schema.setDescription(requestModel.getDescription());
					}
					// TODO update modification timestamps
					schema = schemaService.save(schema);
					rc.response().setStatusCode(200).end(toJson(schemaService.transformToRest(schema)));
				});

		});
	}

	private void addDeleteHandler() {
		Route route = route("/:uuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			String uuid = rc.request().params().get("uuid");

			loadObject(rc, "uuid", PermissionType.DELETE, (AsyncResult<ObjectSchema> srh) -> {
				ObjectSchema schema = srh.result();
				schemaService.delete(schema);
				rc.response().setStatusCode(200).end(toJson(new GenericMessageResponse(i18n.get(rc, "schema_deleted", uuid))));
			});
		});

	}

	private void addReadHandlers() {
		route("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				loadObject(rc, "uuid", PermissionType.READ, (AsyncResult<ObjectSchema> srh) -> {
					ObjectSchema schema = srh.result();
					rc.response().setStatusCode(200).end(toJson(schemaService.transformToRest(schema)));
				});
			}
		});

		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			ObjectSchemaListResponse listResponse = new ObjectSchemaListResponse();
			PagingInfo pagingInfo = getPagingInfo(rc);
			User user = userService.findUser(rc);
			Page<ObjectSchema> schemaPage = schemaService.findAllVisible(user, pagingInfo);
			for (ObjectSchema schema : schemaPage) {
				listResponse.getData().add(schemaService.transformToRest(schema));
			}
			RestModelPagingHelper.setPaging(listResponse, schemaPage, pagingInfo);
			rc.response().setStatusCode(200).end(toJson(listResponse));
		});

	}

}
