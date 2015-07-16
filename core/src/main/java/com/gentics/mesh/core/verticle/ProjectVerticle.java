package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.Future;
import io.vertx.ext.web.Route;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.util.BlueprintTransaction;

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
			loadObject(rc, "uuid", UPDATE_PERM, boot.projectRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {

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

					transformAndResponde(rc, project);
				}
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
				} else {
					try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
						ProjectRoot projectRoot = boot.projectRoot();
						Project project = projectRoot.create(requestModel.getName(), requestUser);
						project.setCreator(requestUser);
						try {
							routerStorage.addProjectRouter(project.getName());
							String msg = "Registered project {" + project.getName() + "}";
							log.info(msg);
							requestUser.addCRUDPermissionOnRole(boot.meshRoot(), CREATE_PERM, project);
							requestUser.addCRUDPermissionOnRole(boot.meshRoot(), CREATE_PERM, project.getBaseNode());
							tx.success();
							projectCreated.complete(project);
						} catch (Exception e) {
							// TODO should we really fail here?
					rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "Error while adding project to router storage"), e));
					tx.failure();
					return;
				}
			}
		}

	}, trh -> {
		Project project = projectCreated.result();
		transformAndResponde(rc, project);
	}		);

		});
	}

	private void addReadHandler() {
		route("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				loadTransformAndResponde(rc, "uuid", READ_PERM, boot.projectRoot());
			}
		});

		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			loadTransformAndResponde(rc, boot.projectRoot(), new ProjectListResponse());
		});
	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			delete(rc, "uuid", "project_deleted", boot.projectRoot());
		});
	}
}
