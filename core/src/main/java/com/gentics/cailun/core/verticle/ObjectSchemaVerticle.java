package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.UUIDUtil.isUUID;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import java.util.HashMap;
import java.util.Map;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.service.ObjectSchemaService;
import com.gentics.cailun.core.rest.response.RestObjectSchema;

@Component
@Scope("singleton")
@SpringVerticle
public class ObjectSchemaVerticle extends AbstractProjectRestVerticle {

	@Autowired
	private ObjectSchemaService schemaService;

	protected ObjectSchemaVerticle() {
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
		route("/:uuidOrName").method(PUT).handler(rc -> {

		});
	}

	private void addDeleteHandler() {
		route("/:uuidOrName").method(DELETE).handler(rc -> {

		});

	}

	private void addReadHandlers() {
		// produces(APPLICATION_JSON)
		route("/").method(GET).handler(rc -> {
			String projectName = getProjectName(rc);
			Iterable<ObjectSchema> projectSchemas = schemaService.findAll(projectName);
			Map<String, RestObjectSchema> resultMap = new HashMap<>();
			if (projectSchemas == null) {
				rc.response().end(toJson(resultMap));
				return;
			}
			for (ObjectSchema schema : projectSchemas) {
				RestObjectSchema restSchema = schemaService.getReponseObject(schema);
				resultMap.put(schema.getName(), restSchema);
			}
			rc.response().end(toJson(resultMap));
			return;
		});

		route("/:uuidOrName").method(GET).handler(rh -> {

			String projectName = getProjectName(rh);
			String uuidOrName = rh.request().params().get("uuidOrName");
			if (isUUID(uuidOrName)) {
				ObjectSchema projectSchema = schemaService.findByUUID(projectName, uuidOrName);
				rh.response().end(toJson(projectSchema));
				return;
			} else {
				ObjectSchema projectSchema = schemaService.findByName(projectName, uuidOrName);
				RestObjectSchema schemaForRest = schemaService.getReponseObject(projectSchema);
				rh.response().end(toJson(schemaForRest));
				return;
			}

		});

	}

}
