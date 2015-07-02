package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.json.JsonUtil.toJson;
import static com.gentics.mesh.util.RoutingContextHelper.getPagingInfo;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.ext.web.Route;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.rest.schema.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaUpdateRequest;
import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.RestModelPagingHelper;

@Component
@Scope("singleton")
@SpringVerticle
public class SchemaVerticle extends AbstractCoreApiVerticle {

	protected SchemaVerticle() {
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
			MeshAuthUser requestUser = getUser(rc);

			rcs.loadObject(rc, "projectUuid", UPDATE_PERM, Project.class, (AsyncResult<Project> rh) -> {
				rcs.loadObject(rc, "schemaUuid", READ_PERM, SchemaContainer.class, (AsyncResult<SchemaContainer> srh) -> {
					Project project = rh.result();
					SchemaContainer schema = srh.result();
					schema.addProject(project);
				}, trh -> {
					if (trh.failed()) {
						rc.fail(trh.cause());
					}
					SchemaContainer schema = trh.result();
					//TODO add simple message or return schema?
					//rc.response().setStatusCode(200).end(toJson(schema.transformToRest(requestUser)));
					});
			});

		});

		route = route("/:schemaUuid/projects/:projectUuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			MeshAuthUser requestUser = getUser(rc);

			rcs.loadObject(rc, "projectUuid", UPDATE_PERM, Project.class, (AsyncResult<Project> rh) -> {
				rcs.loadObject(rc, "schemaUuid", READ_PERM, SchemaContainer.class, (AsyncResult<SchemaContainer> srh) -> {
					SchemaContainer schema = srh.result();
					Project project = rh.result();
					schema.removeProject(project);
				}, trh -> {
					if (trh.failed()) {
						rc.fail(trh.cause());
					}
					SchemaContainer schema = trh.result();
					//					rc.response().setStatusCode(200).end(toJson(schema.transformToRest(requestUser)));
					});
			});
		});
	}

	// TODO set creator
	private void addCreateHandler() {
		Route route = route("/").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			MeshAuthUser requestUser = getUser(rc);

			SchemaCreateRequest schema = fromJson(rc, SchemaCreateRequest.class);

			//			if (StringUtils.isEmpty(requestModel.getName())) {
			//				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "schema_missing_name")));
			//				return;
			//			}

			//			if (StringUtils.isEmpty(requestModel.getProjectUuid())) {
			//				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "schema_missing_project_uuid")));
			//				return;
			//			}

			Future<SchemaContainer> schemaCreated = Future.future();
			//			rcs.loadObjectByUuid(rc, requestModel.getProjectUuid(), CREATE_PERM, Project.class, (AsyncResult<Project> srh) -> {
			rcs.loadObjectByUuid(rc, null, CREATE_PERM, Project.class, (AsyncResult<Project> srh) -> {
				Project project = srh.result();
				SchemaContainerRoot root = project.getSchemaRoot();
				//SchemaContainer schema = root.create(requestModel.getName());
				//				schema.setDescription(requestModel.getDescription());
					//				schema.setDisplayName(requestModel.getDisplayName());
					//
					//				for (PropertyTypeSchemaResponse restPropSchema : requestModel.getPropertyTypeSchemas()) {
					//					// TODO validate field?
					//					PropertyType type = PropertyType.valueOfName(restPropSchema.getType());
					//					String key = restPropSchema.getKey();
					//					BasicPropertyType propSchema = schema.createBasicPropertyTypeSchema(key, type);
					//					propSchema.setDescription(restPropSchema.getDesciption());
					//					propSchema.setType(type);
					//					schema.addPropertyTypeSchema(propSchema);
					//				}
					//				schema.addProject(project);
					//				roleService.addCRUDPermissionOnRole(requestUser, project, CREATE_PERM, schema);
					//				schemaCreated.complete(schema);
				}, trh -> {
					if (trh.failed()) {
						rc.fail(trh.cause());
					}
					SchemaContainer schemaContainer = schemaCreated.result();
					//				rc.response().setStatusCode(200).end(toJson(schema.transformToRest(requestUser)));
				});
		});

	}

	// TODO update modification timestamps

	private void addUpdateHandler() {
		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			MeshAuthUser requestUser = getUser(rc);

			rcs.loadObject(rc, "uuid", UPDATE_PERM, SchemaContainer.class, (AsyncResult<SchemaContainer> srh) -> {
				SchemaContainer schemaContainer = srh.result();
				SchemaUpdateRequest requestModel = fromJson(rc, SchemaUpdateRequest.class);

				if (StringUtils.isEmpty(requestModel.getName())) {
					rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_name_must_be_set")));
					return;
				}
				
				schemaContainer.setSchema(requestModel);
//				if (!schema.getName().equals(requestModel.getName())) {
//					schema.setName(requestModel.getName());
//				}

			}, trh -> {
				if (trh.failed()) {
					rc.fail(trh.cause());
				}
				SchemaContainer schema = trh.result();
				//				rc.response().setStatusCode(200).end(toJson(schema.transformToRest(requestUser)));
				});

		});
	}

	private void addDeleteHandler() {
		Route route = route("/:uuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			rcs.loadObject(rc, "uuid", DELETE_PERM, SchemaContainer.class, (AsyncResult<SchemaContainer> srh) -> {
				SchemaContainer schema = srh.result();
				schema.delete();
			}, trh -> {
				SchemaContainer schema = trh.result();
				//				rc.response().setStatusCode(200).end(toJson(new GenericMessageResponse(i18n.get(rc, "schema_deleted", schema.getName()))));
				});
		});

	}

	private void addReadHandlers() {
		route("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			MeshAuthUser requestUser = getUser(rc);

			String uuid = rc.request().params().get("uuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				rcs.loadObject(rc, "uuid", READ_PERM, SchemaContainer.class, (AsyncResult<SchemaContainer> srh) -> {
				}, trh -> {
					if (trh.failed()) {
						rc.fail(trh.cause());
					}
					SchemaContainer schema = trh.result();
					//					rc.response().setStatusCode(200).end(toJson(schema.transformToRest(requestUser)));
					});
			}
		});

		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			MeshAuthUser requestUser = getUser(rc);

			PagingInfo pagingInfo = getPagingInfo(rc);
			vertx.executeBlocking((Future<SchemaListResponse> bch) -> {
				SchemaListResponse listResponse = new SchemaListResponse();
				Page<SchemaContainer> schemaPage = schemaService.findAllVisible(requestUser, pagingInfo);
				for (SchemaContainer schema : schemaPage) {
					//					listResponse.getData().add(schema.transformToRest(requestUser));
				}
				RestModelPagingHelper.setPaging(listResponse, schemaPage, pagingInfo);
				bch.complete(listResponse);
			}, rh -> {
				if (rh.failed()) {
					rc.fail(rh.cause());
				}
				SchemaListResponse listResponse = rh.result();
				rc.response().setStatusCode(200).end(toJson(listResponse));
			});

		});

	}
}
