package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.service.ServerSchemaStorage.getSchemaStorage;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.generic.AbstractGenericVertex;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.json.JsonUtil;

public class SchemaContainerImpl extends AbstractGenericVertex<SchemaResponse> implements SchemaContainer {

	private static final Logger log = LoggerFactory.getLogger(SchemaContainerImpl.class);

	@Override
	public String getType() {
		return SchemaContainer.TYPE;
	}

	@Override
	public SchemaContainer transformToRest(RoutingContext rc, Handler<AsyncResult<SchemaResponse>> handler) {
		try {
			SchemaResponse schemaResponse = JsonUtil.readSchema(getJson(), SchemaResponse.class);
			schemaResponse.setUuid(getUuid());

			//			for (ProjectImpl project : getProjects()) {
			//				ProjectResponse restProject = new ProjectResponse();
			//				restProject.setUuid(project.getUuid());
			//				restProject.setName(project.getName());
			//				schemaResponse.getProjects().add(restProject);
			//			}

			// Sort the list by project name
			Collections.sort(schemaResponse.getProjects(), new Comparator<ProjectResponse>() {
				@Override
				public int compare(ProjectResponse o1, ProjectResponse o2) {
					return o1.getName().compareTo(o2.getName());
				};
			});

			schemaResponse.setPermissions(getUser(rc).getPermissionNames(this));

			handler.handle(Future.succeededFuture(schemaResponse));
		} catch (IOException e) {
			handler.handle(Future.failedFuture(e));
		}
		return this;
	}

	@Override
	public void delete() {
		//TODO should all references be updated to a new fallback schema?
		getElement().remove();
	}

	@Override
	public SchemaContainerImpl getImpl() {
		return this;
	}

	private String getJson() {
		return getProperty("json");
	}

	private void setJson(String json) {
		setProperty("json", json);
	}

	@Override
	public Schema getSchema() throws IOException {
		Schema schema = getSchemaStorage().getSchema(getName());
		if (schema == null) {
			schema = JsonUtil.readSchema(getJson(), SchemaImpl.class);
			getSchemaStorage().addSchema(schema);
		}
		return schema;

	}

	@Override
	public void setSchema(Schema schema) {
		getSchemaStorage().removeSchema(schema.getName());
		getSchemaStorage().addSchema(schema);
		String json = JsonUtil.toJson(schema);
		setJson(json);
	}

	@Override
	public void setName(String name) {
		setProperty("name", name);
	}

	@Override
	public String getName() {
		return getProperty("name");
	}

}
