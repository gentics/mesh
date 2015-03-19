package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.fromJson;
import static com.gentics.cailun.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.ext.apex.Route;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCoreApiVerticle;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.service.ProjectService;
import com.gentics.cailun.core.rest.common.response.GenericMessageResponse;
import com.gentics.cailun.core.rest.project.request.ProjectCreateRequest;
import com.gentics.cailun.core.rest.project.request.ProjectUpdateRequest;
import com.gentics.cailun.core.rest.project.response.ProjectListResponse;
import com.gentics.cailun.core.rest.project.response.ProjectResponse;
import com.gentics.cailun.error.HttpStatusCodeErrorException;

@Component
@Scope("singleton")
@SpringVerticle
public class ProjectVerticle extends AbstractCoreApiVerticle {

	private static final Logger log = LoggerFactory.getLogger(ProjectVerticle.class);

	@Autowired
	private ProjectService projectService;

	protected ProjectVerticle() {
		super("projects");
	}

	@Override
	public void registerEndPoints() throws Exception {
		addCRUDHandlers();
	}

	private void addCRUDHandlers() {
		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addUpdateHandler() {
		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON);
		route.handler(rc -> {
			Project project = getObject(rc, "uuid", PermissionType.UPDATE);
			ProjectUpdateRequest requestModel = fromJson(rc, ProjectUpdateRequest.class);
			if (requestModel == null) {
				// TODO exception would be nice, add i18n
				String message = "Could not parse request json.";
				throw new HttpStatusCodeErrorException(400, message);
			}

			// Update the project or show 404

			if (requestModel.getName() != null && project.getName() != requestModel.getName()) {
				if (projectService.findByName(requestModel.getName()) != null) {
					rc.response().setStatusCode(409);
					// TODO i18n
					rc.response().end(
							toJson(new GenericMessageResponse("A project with the name {" + requestModel.getName()
									+ "} already exists. Please choose a different name.")));
					return;
				}
				project.setName(requestModel.getName());
			}

			try {
				project = projectService.save(project);
			} catch (ConstraintViolationException e) {
				// TODO log
				// TODO correct msg?
				// TODO i18n
				rc.response().setStatusCode(409);
				rc.response().end(toJson(new GenericMessageResponse("Project can't be saved. Unknown error.")));
				return;
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

			if (requestModel == null) {
				// TODO exception would be nice, add i18n
				String message = "Could not parse request json.";
				rc.response().setStatusCode(400);
				rc.response().end(toJson(new GenericMessageResponse(message)));
				return;
			}

			if (StringUtils.isEmpty(requestModel.getName())) {
				rc.response().setStatusCode(400);
				// TODO i18n
				throw new HttpStatusCodeErrorException(400, "Mandatory field name was not specified.");
			}

			if (projectService.findByName(requestModel.getName()) != null) {
				// TODO i18n
				throw new HttpStatusCodeErrorException(400, "Conflicting username");
			}

			Project project = projectService.transformFromRest(requestModel);
			if (project == null) {
				// TODO handle error?
			} else {
				project = projectService.save(project);
				project = projectService.reload(project);
				try {
					routerStorage.addProjectRouter(project.getName());
					String msg = "Registered project {" + project.getName() + "}";
					log.info(msg);
					rc.response().end(toJson(projectService.transformToRest(project)));
				} catch (Exception e) {
					rc.fail(409);
					rc.fail(e);
				}
			}
		});
	}

	private void addReadHandler() {

		route("/:uuid").method(GET).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				Project project = getObject(rc, "uuid", PermissionType.READ);
				ProjectResponse restProject = projectService.transformToRest(project);
				rc.response().setStatusCode(200);
				rc.response().end(toJson(restProject));
			}
		});

		route("/").method(GET).handler(rc -> {
			List<Project> projects = projectService.findAll();
			ProjectListResponse response = new ProjectListResponse();

			for (Project project : projects) {
				if (hasPermission(rc, project, PermissionType.READ)) {
					response.addProject(projectService.transformToRest(project));
				}
			}
			rc.response().setStatusCode(200);
			rc.response().end(toJson(response));
		});

	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).handler(rc -> {
			Project project = getObject(rc, "uuid", PermissionType.DELETE);
			String name = project.getName();
			routerStorage.removeProjectRouter(name);
			projectService.delete(project);
			// TODO i18n
				String msg = "Deleted project {" + name + "}";
				rc.response().setStatusCode(200);
				rc.response().end(toJson(new GenericMessageResponse(msg)));
			});
	}
}
