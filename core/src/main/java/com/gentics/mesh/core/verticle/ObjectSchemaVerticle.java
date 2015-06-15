package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.util.JsonUtils.fromJson;
import static com.gentics.mesh.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.ext.apex.Route;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.auth.MeshPermission;
import com.gentics.mesh.core.data.model.schema.propertytype.BasicPropertyType;
import com.gentics.mesh.core.data.model.schema.propertytype.PropertyType;
import com.gentics.mesh.core.data.model.tinkerpop.Schema;
import com.gentics.mesh.core.data.model.tinkerpop.Project;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.gentics.mesh.core.rest.common.response.GenericMessageResponse;
import com.gentics.mesh.core.rest.schema.request.ObjectSchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.request.ObjectSchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.response.ObjectSchemaListResponse;
import com.gentics.mesh.core.rest.schema.response.PropertyTypeSchemaResponse;
import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.RestModelPagingHelper;

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
		Route route = route("/:schemaUuid/projects/:projectUuid").method(POST).produces(APPLICATION_JSON);
		route.handler(rc -> {
			rcs.loadObject(rc, "projectUuid", PermissionType.UPDATE, (AsyncResult<Project> rh) -> {
				rcs.loadObject(rc, "schemaUuid", PermissionType.READ, (AsyncResult<Schema> srh) -> {
					Project project = rh.result();
					Schema schema = srh.result();
					schema.addProject(project);
//						schema = schemaService.save(schema);
				}, trh -> {
					Schema schema = trh.result();
					rc.response().setStatusCode(200).end(toJson(schemaService.transformToRest(schema)));
				});
			});

		});

		route = route("/:schemaUuid/projects/:projectUuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			rcs.loadObject(rc, "projectUuid", PermissionType.UPDATE, (AsyncResult<Project> rh) -> {
				rcs.loadObject(rc, "schemaUuid", PermissionType.READ, (AsyncResult<Schema> srh) -> {
					Schema schema = srh.result();
					Project project = rh.result();
					schema.removeProject(project);
//						schema = schemaService.save(schema);
//					}
				}, trh -> {
					Schema schema = trh.result();
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
				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "schema_missing_name")));
				return;
			}

			if (StringUtils.isEmpty(requestModel.getProjectUuid())) {
				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "schema_missing_project_uuid")));
				return;
			}

			Future<Schema> schemaCreated = Future.future();
			rcs.loadObjectByUuid(rc, requestModel.getProjectUuid(), PermissionType.CREATE, (AsyncResult<Project> srh) -> {
				Project project = srh.result();

				Schema schema =  schemaService.create(requestModel.getName());
				schema.setDescription(requestModel.getDescription());
				schema.setDisplayName(requestModel.getDisplayName());

				for (PropertyTypeSchemaResponse restPropSchema : requestModel.getPropertyTypeSchemas()) {
					// TODO validate field?
					PropertyType type = PropertyType.valueOfName(restPropSchema.getType());
					String key = restPropSchema.getKey();
					BasicPropertyType propSchema = schemaService.createBasicPropertyTypeSchema(key, type);
					propSchema.setDescription(restPropSchema.getDesciption());
					propSchema.setType(type);
					schema.addPropertyTypeSchema(propSchema);
				}
				schema.addProject(project);
				roleService.addCRUDPermissionOnRole(rc, new MeshPermission(project, PermissionType.CREATE), schema);
				schemaCreated.complete(schema);
			}, trh -> {
				Schema schema = schemaCreated.result();
				rc.response().setStatusCode(200).end(toJson(schemaService.transformToRest(schema)));
			});
		});

	}

	// TODO update modification timestamps

	private void addUpdateHandler() {
		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			rcs.loadObject(rc, "uuid", PermissionType.UPDATE, (AsyncResult<Schema> srh) -> {
				Schema schema = srh.result();
				ObjectSchemaUpdateRequest requestModel = fromJson(rc, ObjectSchemaUpdateRequest.class);

				if (StringUtils.isEmpty(requestModel.getName())) {
					rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_name_must_be_set")));
					return;
				}
				if (!schema.getName().equals(requestModel.getName())) {
					schema.setName(requestModel.getName());
				}

				if (schema.getDescription() != null && (!schema.getDescription().equals(requestModel.getDescription()))) {
					schema.setDescription(requestModel.getDescription());
				}
			}, trh -> {
				Schema schema = trh.result();
				rc.response().setStatusCode(200).end(toJson(schemaService.transformToRest(schema)));
			});

		});
	}

	private void addDeleteHandler() {
		Route route = route("/:uuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			rcs.loadObject(rc, "uuid", PermissionType.DELETE, (AsyncResult<Schema> srh) -> {
				Schema schema = srh.result();
				schemaService.delete(schema);
			}, trh -> {
				Schema schema = trh.result();
				rc.response().setStatusCode(200).end(toJson(new GenericMessageResponse(i18n.get(rc, "schema_deleted", schema.getName()))));
			});
		});

	}

	private void addReadHandlers() {
		route("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				rcs.loadObject(rc, "uuid", PermissionType.READ, (AsyncResult<Schema> srh) -> {
				}, trh -> {
					Schema schema = trh.result();
					rc.response().setStatusCode(200).end(toJson(schemaService.transformToRest(schema)));
				});
			}
		});

		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			PagingInfo pagingInfo = rcs.getPagingInfo(rc);
			vertx.executeBlocking((Future<ObjectSchemaListResponse> bch) -> {
				ObjectSchemaListResponse listResponse = new ObjectSchemaListResponse();
				User user = userService.findUser(rc);
				Page<Schema> schemaPage = schemaService.findAllVisible(user, pagingInfo);
				for (Schema schema : schemaPage) {
					listResponse.getData().add(schemaService.transformToRest(schema));
				}
				RestModelPagingHelper.setPaging(listResponse, schemaPage, pagingInfo);
				bch.complete(listResponse);
			}, rh -> {
				ObjectSchemaListResponse listResponse = rh.result();
				rc.response().setStatusCode(200).end(toJson(listResponse));
			});

		});

	}
}
