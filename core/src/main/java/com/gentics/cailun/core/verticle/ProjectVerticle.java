package com.gentics.cailun.core.verticle;

import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractCoreApiVerticle;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.repository.ProjectRepository;

public class ProjectVerticle extends AbstractCoreApiVerticle {

	private static final Logger log = LoggerFactory.getLogger(ProjectVerticle.class);

	@Autowired
	private ProjectRepository projectRepository;

	protected ProjectVerticle() {
		super("projects");
	}

	@Override
	public void registerEndPoints() throws Exception {
		// TODO Auto-generated method stub
		addProjectHandlers();
	}

	private void addProjectHandlers() {
		route("/:nameOrUuid").method(GET).handler(ctx -> {
			String nameOrUuid = ctx.request().params().get("nameOrUuid");
			// TODO prefix uuids to identify them "@uid:" or similar
			// TODO check for uuid or name
			// TODO add check whether project was already registered/added
				Project project = projectRepository.findByName(nameOrUuid);
				if (project == null) {
					project = new Project(nameOrUuid);
					projectRepository.save(project);
				}
				try {
					springConfig.routerStorage().addProjectRouter(nameOrUuid);
					log.info("Registered project {" + nameOrUuid + "}");
					ctx.response().end("Registered project {" + nameOrUuid + "}");
				} catch (Exception e) {
					ctx.fail(409);
					ctx.fail(e);
				}
			});

		route("/projects/:name").method(DELETE).handler(ctx -> {
			String name = ctx.request().params().get("name");
			springConfig.routerStorage().removeProjectRouter(name);
			ctx.response().end("Deleted project {" + name + "}");
		});

		route("/projects/").method(POST).handler(ctx -> {
			// TODO also create a default object schema for the project. Move this into service class
			// ObjectSchema defaultContentSchema = objectSchemaService.findByName(, name)
			});

		route("/projects/:nameOrUuid").method(PUT).handler(ctx -> {

		});
	}
}
