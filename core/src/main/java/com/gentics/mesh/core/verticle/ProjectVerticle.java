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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
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
		addLanguagesHandler();
	}

	private void addLanguagesHandler() {
		//TODO Add method that allows assigning languages from and to the project
		Route createRoute = route("/:projectUuid/languages").method(POST).produces(APPLICATION_JSON);
		createRoute.handler(rc -> {

		});

		Route deleteRoute = route("/:projectUuid/languages").method(DELETE).produces(APPLICATION_JSON);
		deleteRoute.handler(rc -> {

		});

		Route getRoute = route("/:projectUuid/languages").method(GET).produces(APPLICATION_JSON);
		getRoute.handler(rc -> {

		});
	}

	private void addUpdateHandler() {
		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			MeshAuthUser requestUser = getUser(rc);

			rcs.loadObject(rc, "uuid", UPDATE_PERM, ProjectImpl.class, (AsyncResult<Project> rh) -> {
				Project project = rh.result();

				ProjectUpdateRequest requestModel = fromJson(rc, ProjectUpdateRequest.class);

				// Check for conflicting project name
					if (requestModel.getName() != null && project.getName() != requestModel.getName()) {
						if (boot.projectRoot().findByName(requestModel.getName()) != null) {
							rc.fail(new HttpStatusCodeErrorException(409, i18n.get(rc, "project_conflicting_name")));
							return;
						}
						project.setName(requestModel.getName());
					}

				}, trh -> {
					Project project = trh.result();
					rc.response().setStatusCode(200).end(toJson(project.transformToRest(requestUser)));
				});
		});
	}

	// TODO when the root tag is not saved the project can't be saved. Unfortunately this did not show up as an http error. We must handle those
	// cases. They must show up in any case.
	private void addCreateHandler() {
		Route route = route("/").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {

			// TODO also create a default object schema for the project. Move this into service class
			// ObjectSchema defaultContentSchema = objectSchemaRoot.findByName(, name)
			ProjectCreateRequest requestModel = fromJson(rc, ProjectCreateRequest.class);
			MeshAuthUser requestUser = getUser(rc);

			if (StringUtils.isEmpty(requestModel.getName())) {
				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "project_missing_name")));
				return;
			}

			Future<Project> projectCreated = Future.future();
			// TODO replace rcs.hasPerm with requestUser.isAuthorised
			// requestUser.isAuthorised(meshRoot.getProjectRoot(), CREATE_PERM, rh-> {
			//
			// });

			rcs.hasPermission(rc, boot.projectRoot(), CREATE_PERM, rh -> {
				if (boot.projectRoot().findByName(requestModel.getName()) != null) {
					rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "project_conflicting_name")));
					return;
				}

				ProjectRoot projectRoot = boot.projectRoot();
				Project project = projectRoot.create(requestModel.getName());
				project.setCreator(requestUser);

				try {
					routerStorage.addProjectRouter(project.getName());
					String msg = "Registered project {" + project.getName() + "}";
					log.info(msg);
					requestUser.addCRUDPermissionOnRole(boot.meshRoot(), CREATE_PERM, project);
					projectCreated.complete(project);
				} catch (Exception e) {
					// TODO should we really fail here?
					rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "Error while adding project to router storage")));
					return;
				}
			}, trh -> {
				Project project = projectCreated.result();
				rc.response().end(toJson(project.transformToRest(requestUser)));
			});

		});
	}

	private void addReadHandler() {

		route("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			MeshAuthUser requestUser = getUser(rc);

			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				rcs.loadObject(rc, "uuid", READ_PERM, ProjectImpl.class, (AsyncResult<Project> rh) -> {
					if (rh.failed()) {
						rc.fail(rh.cause());
					}
					Project project = rh.result();
					rc.response().setStatusCode(200).end(toJson(project.transformToRest(requestUser)));
				});
			}
		});

		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {

			PagingInfo pagingInfo = getPagingInfo(rc);
			MeshAuthUser requestUser = getUser(rc);

			vertx.executeBlocking((Future<ProjectListResponse> bcr) -> {
				ProjectListResponse listResponse = new ProjectListResponse();
				Page<? extends Project> projectPage;
				try {
					projectPage = boot.projectRoot().findAll(requestUser, pagingInfo);
					for (Project project : projectPage) {
						listResponse.getData().add(project.transformToRest(requestUser));
					}
					RestModelPagingHelper.setPaging(listResponse, projectPage, pagingInfo);
					bcr.complete(listResponse);

				} catch (Exception e) {
					bcr.fail(e);
				}
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
			rcs.loadObject(rc, "uuid", DELETE_PERM, ProjectImpl.class, (AsyncResult<Project> rh) -> {
				Project project = rh.result();
				String name = project.getName();
				routerStorage.removeProjectRouter(name);
				project.delete();
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
