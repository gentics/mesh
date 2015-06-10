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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.auth.MeshPermission;
import com.gentics.mesh.core.data.model.root.MeshRoot;
import com.gentics.mesh.core.data.model.tinkerpop.Project;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.gentics.mesh.core.rest.common.response.GenericMessageResponse;
import com.gentics.mesh.core.rest.project.request.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.request.ProjectUpdateRequest;
import com.gentics.mesh.core.rest.project.response.ProjectListResponse;
import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.RestModelPagingHelper;

@Component
@Scope("singleton")
@SpringVerticle
public class ProjectVerticle extends AbstractCoreApiVerticle {

	private static final Logger log = LoggerFactory.getLogger(ProjectVerticle.class);

	protected ProjectVerticle() {
		super("projects");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addUpdateHandler() {
		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {

			rcs.loadObject(rc, "uuid", PermissionType.UPDATE, (AsyncResult<Project> rh) -> {
				Project project = rh.result();

				ProjectUpdateRequest requestModel = fromJson(rc, ProjectUpdateRequest.class);

				// Check for conflicting project name
					if (requestModel.getName() != null && project.getName() != requestModel.getName()) {
						if (projectService.findByName(requestModel.getName()) != null) {
							rc.fail(new HttpStatusCodeErrorException(409, i18n.get(rc, "project_conflicting_name")));
							return;
						}
						project.setName(requestModel.getName());
					}

				}, trh -> {
					Project project = trh.result();
					rc.response().setStatusCode(200).end(toJson(projectService.transformToRest(rc, project)));
				});
		});
	}

	// TODO when the root tag is not saved the project can't be saved. Unfortunately this did not show up as an http error. We must handle those
	// cases. They must show up in any case.
	private void addCreateHandler() {
		Route route = route("/").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			// TODO also create a default object schema for the project. Move this into service class
			// ObjectSchema defaultContentSchema = objectSchemaService.findByName(, name)
			ProjectCreateRequest requestModel = fromJson(rc, ProjectCreateRequest.class);

			if (StringUtils.isEmpty(requestModel.getName())) {
				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "project_missing_name")));
				return;
			}

			Future<Project> projectCreated = Future.future();
			MeshRoot meshRoot = meshRootService.findRoot();
			rcs.hasPermission(rc, meshRoot, PermissionType.CREATE, rh -> {
				if (projectService.findByName(requestModel.getName()) != null) {
					rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "project_conflicting_name")));
					return;
				}

				Project project = projectService.create(requestModel.getName());
				User user = userService.findUser(rc);
				project.setRootNode(nodeService.create());
				project.setCreator(user);

				try {
					routerStorage.addProjectRouter(project.getName());
					String msg = "Registered project {" + project.getName() + "}";
					log.info(msg);
					roleService.addCRUDPermissionOnRole(rc, new MeshPermission(meshRoot, PermissionType.CREATE), project);
					projectCreated.complete(project);
				} catch (Exception e) {
					// TODO should we really fail here?
					rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "Error while adding project to router storage")));
					return;
				}
			}, trh -> {
				Project project = projectCreated.result();
				rc.response().end(toJson(projectService.transformToRest(rc, project)));
			});

		});
	}

	private void addReadHandler() {

		route("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				rcs.loadObject(rc, "uuid", PermissionType.READ, (AsyncResult<Project> rh) -> {
					if (rh.failed()) {
						rc.fail(rh.cause());
					}
					Project project = rh.result();
					rc.response().setStatusCode(200).end(toJson(projectService.transformToRest(rc, project)));
				});
			}
		});

		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {

			PagingInfo pagingInfo = rcs.getPagingInfo(rc);

			vertx.executeBlocking((Future<ProjectListResponse> bcr) -> {
				ProjectListResponse listResponse = new ProjectListResponse();
				User requestUser = userService.findUser(rc);
				Page<Project> projectPage = projectService.findAllVisible(requestUser, pagingInfo);
				for (Project project : projectPage) {
					listResponse.getData().add(projectService.transformToRest(rc, project));
				}
				RestModelPagingHelper.setPaging(listResponse, projectPage, pagingInfo);
				bcr.complete(listResponse);
			}, arh -> {
				if (arh.failed()) {
					rc.fail(arh.cause());
				}
				ProjectListResponse listResponse = arh.result();
				rc.response().setStatusCode(200).end(toJson(listResponse));
			});
		});

	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			rcs.loadObject(rc, "uuid", PermissionType.DELETE, (AsyncResult<Project> rh) -> {
				Project project = rh.result();
				String name = project.getName();
				routerStorage.removeProjectRouter(name);
				projectService.delete(project);
			}, trh -> {
				if (trh.failed()) {
					rc.fail(trh.cause());
				}
				String name = trh.result().getName();
				rc.response().setStatusCode(200).end(toJson(new GenericMessageResponse(i18n.get(rc, "project_deleted", name))));
			});
		});
	}
}
