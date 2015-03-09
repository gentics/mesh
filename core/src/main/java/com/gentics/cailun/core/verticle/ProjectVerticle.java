package com.gentics.cailun.core.verticle;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractCoreApiVerticle;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.service.ProjectService;
import com.gentics.cailun.core.rest.common.response.GenericMessageResponse;
import com.gentics.cailun.core.rest.project.request.ProjectCreateRequest;
import com.gentics.cailun.core.rest.project.response.ProjectResponse;
import com.gentics.cailun.util.UUIDUtil;

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

	private void addDeleteHandler() {
		route("/:uuidOrName").method(DELETE).handler(rc -> {
			String uuidOrName = rc.request().params().get("uuidOrName");
			Project project = null;

			if (UUIDUtil.isUUID(uuidOrName)) {
				project = projectService.findByName(uuidOrName);
			} else {
				project = projectService.findByName(uuidOrName);
			}
			if (project != null) {
				String name = project.getName();
				routerStorage.removeProjectRouter(name);
				projectService.delete(project);
				// TODO json
				rc.response().end("Deleted project {" + name + "}");
			} else {
				// TODO i18n error message?
				String message = "Project not found {" + uuidOrName + "}";
				rc.response().setStatusCode(404);
				rc.response().end(toJson(new GenericMessageResponse(message)));
			}
		});
	}

	private void addUpdateHandler() {
		route("/:uuidOrName").method(PUT).consumes(APPLICATION_JSON).handler(rc -> {

		});
	}

	private void addCreateHandler() {
		route("/").method(POST).consumes(APPLICATION_JSON).handler(rc -> {
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
					rc.response().end(toJson(new GenericMessageResponse("Mandatory field name was not specified.")));
					return;
				}

				if (projectService.findByName(requestModel.getName()) != null) {
					// TODO i18n
					rc.response().setStatusCode(400);
					rc.response().end(toJson(new GenericMessageResponse("Conflicting username")));
					return;
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
						rc.response().end(toJson(project));
					} catch (Exception e) {
						rc.fail(409);
						rc.fail(e);
					}
				}
			});
	}

	private void addReadHandler() {
		route("/:uuidOrName").method(GET).handler(rc -> {
			String uuidOrName = rc.request().params().get("uuidOrName");
			// TODO prefix uuids to identify them "urn:uuid:" or similar
			// TODO check for uuid or name
			// TODO add check whether project was already registered/added
				Project project = null;

				if (UUIDUtil.isUUID(uuidOrName)) {
					project = projectService.findByUUID(uuidOrName);
				} else {
					project = projectService.findByName(uuidOrName);
				}
				if (project != null) {
					ProjectResponse restProject = projectService.getResponseObject(project);
					rc.response().setStatusCode(200);
					rc.response().end(toJson(restProject));
				} else {
					// TODO i18n error message?
					String message = "Project not found {" + uuidOrName + "}";
					rc.response().setStatusCode(404);
					rc.response().end(toJson(new GenericMessageResponse(message)));
				}

			});

	}
}
