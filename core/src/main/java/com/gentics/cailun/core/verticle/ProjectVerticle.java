package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.fromJson;
import static com.gentics.cailun.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.ext.apex.Route;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCoreApiVerticle;
import com.gentics.cailun.core.data.model.CaiLunRoot;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.rest.common.response.GenericMessageResponse;
import com.gentics.cailun.core.rest.project.request.ProjectCreateRequest;
import com.gentics.cailun.core.rest.project.request.ProjectUpdateRequest;
import com.gentics.cailun.core.rest.project.response.ProjectListResponse;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
import com.gentics.cailun.paging.PagingInfo;
import com.gentics.cailun.util.RestModelPagingHelper;

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
		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addUpdateHandler() {
		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON);
		route.handler(rc -> {
			Project project;
			try (Transaction tx = graphDb.beginTx()) {

				project = getObject(rc, "uuid", PermissionType.UPDATE);
				ProjectUpdateRequest requestModel = fromJson(rc, ProjectUpdateRequest.class);

				// Check for conflicting project name
				if (requestModel.getName() != null && project.getName() != requestModel.getName()) {
					if (projectService.findByName(requestModel.getName()) != null) {
						throw new HttpStatusCodeErrorException(409, i18n.get(rc, "project_conflicting_name"));
					}
					project.setName(requestModel.getName());
				}

				project = projectService.save(project);
				tx.success();
			}
			rc.response().setStatusCode(200);
			rc.response().end(toJson(projectService.transformToRest(project)));
		});
	}

	private void addCreateHandler() {
		Route route = route("/").method(POST).consumes(APPLICATION_JSON);
		route.handler(rc -> {
			// TODO also create a default object schema for the project. Move this into service class
			// ObjectSchema defaultContentSchema = objectSchemaService.findByName(, name)
			ProjectCreateRequest requestModel = fromJson(rc, ProjectCreateRequest.class);

			if (StringUtils.isEmpty(requestModel.getName())) {
				throw new HttpStatusCodeErrorException(400, i18n.get(rc, "project_missing_name"));
			}

			Project project;
			try (Transaction tx = graphDb.beginTx()) {

				CaiLunRoot cailunRoot = cailunRootService.findRoot();
				failOnMissingPermission(rc, cailunRoot, PermissionType.CREATE);

				if (projectService.findByName(requestModel.getName()) != null) {
					throw new HttpStatusCodeErrorException(400, i18n.get(rc, "project_conflicting_name"));
				}

				project = projectService.transformFromRest(requestModel);
				project = projectService.save(project);

				try {
					routerStorage.addProjectRouter(project.getName());
					String msg = "Registered project {" + project.getName() + "}";
					log.info(msg);
					tx.success();
					roleService.addCRUDPermissionOnRole(rc, new CaiLunPermission(cailunRoot, PermissionType.CREATE), project);

				} catch (Exception e) {
					// TODO should we really fail here?
					tx.failure();
					throw new HttpStatusCodeErrorException(400, i18n.get(rc, "Error while adding project to router storage"));
				}
			}
			project = projectService.reload(project);
			rc.response().end(toJson(projectService.transformToRest(project)));

		});
	}

	private void addReadHandler() {

		route("/:uuid").method(GET).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				Project project;
				try (Transaction tx = graphDb.beginTx()) {
					project = getObject(rc, "uuid", PermissionType.READ);
					tx.success();
				}
				rc.response().setStatusCode(200);
				rc.response().end(toJson(projectService.transformToRest(project)));
			}
		});

		route("/").method(GET).handler(rc -> {

			ProjectListResponse listResponse = new ProjectListResponse();
			try (Transaction tx = graphDb.beginTx()) {
				PagingInfo pagingInfo = getPagingInfo(rc);
				
				//User requestUser = springConfiguration.authService().getUser(rc);
				User requestUser = null;
				Page<Project> projectPage = projectService.findAllVisible(requestUser, pagingInfo);
				for (Project project  : projectPage) {
					listResponse.getData().add(projectService.transformToRest(project));
				}
				RestModelPagingHelper.setPaging(listResponse, projectPage, pagingInfo);
				tx.success();
			}
			rc.response().setStatusCode(200);
			rc.response().end(toJson(listResponse));

				});

	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			try (Transaction tx = graphDb.beginTx()) {

				Project project = getObject(rc, "uuid", PermissionType.DELETE);
				String name = project.getName();
				routerStorage.removeProjectRouter(name);
				projectService.delete(project);
				tx.success();
			}
			rc.response().setStatusCode(200);
			rc.response().end(toJson(new GenericMessageResponse(i18n.get(rc, "project_deleted", uuid))));
		});
	}
}
