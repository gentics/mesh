package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.UUIDUtil.isUUID;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.conversion.Result;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.core.rest.model.ObjectSchema;
import com.gentics.cailun.core.rest.service.ObjectSchemaService;

//TODO rename to schema verticle?
public class TypesVerticle extends AbstractProjectRestVerticle {

	@Autowired
	private ObjectSchemaService schemaService;

	protected TypesVerticle() {
		super("types");
	}

	@Override
	public void registerEndPoints() throws Exception {
		addCRUDHandlers();
	}

	private void addCRUDHandlers() {

		addCreateHandler();
		addReadHandlers();
		addUpdateHandler();
		addDeleteHandler();

	}

	private void addCreateHandler() {
		route("/:uuidOrName").method(POST).handler(rh -> {

		});

	}

	private void addUpdateHandler() {
		route("/:uuidOrName").method(PUT).handler(rh -> {

		});
	}

	private void addDeleteHandler() {
		route("/:uuidOrName").method(DELETE).handler(rh -> {

		});

	}

	private void addReadHandlers() {
		route("/:uuidOrName").method(GET).handler(rh -> {
			String project = getProjectName(rh);
			String uuidOrName = rh.request().params().get("uuidOrName");
			if (isUUID(uuidOrName)) {
				ObjectSchema projectSchema = schemaService.findByUUID(project, uuidOrName);
				rh.response().end(toJson(projectSchema));
			} else {
				ObjectSchema projectSchema = schemaService.findByName(project, uuidOrName);
				rh.response().end(toJson(projectSchema));
			}

		});

		route("/").method(GET).handler(rh -> {
			String project = getProjectName(rh);
			Result<ObjectSchema> projectSchemas = schemaService.findAll(project);
			rh.response().end(toJson(projectSchemas));
		});

	}

}
