package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.service.ServerSchemaStorage.getSchemaStorage;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static com.gentics.mesh.util.VerticleHelper.getUser;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.gentics.mesh.json.JsonUtil.*;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.generic.AbstractGenericVertex;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.core.rest.schema.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class SchemaContainerImpl extends AbstractGenericVertex<SchemaResponse>implements SchemaContainer {

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
	public Schema getSchema() {
		Schema schema = getSchemaStorage().getSchema(getName());
		if (schema == null) {
			try {
				schema = JsonUtil.readSchema(getJson(), SchemaImpl.class);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
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

	@Override
	public void update(RoutingContext rc) {

		SchemaUpdateRequest requestModel = fromJson(rc, SchemaUpdateRequest.class);
		I18NService i18n = I18NService.getI18n();
		if (StringUtils.isEmpty(requestModel.getName())) {
			rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "error_name_must_be_set")));
			return;
		}
		//TODO update name? check for conflicting names?
		setSchema(requestModel);
	}
	
	@Override
	public void updateIndex(Handler<Future> handler) {
		// TODO Auto-generated method stub
		
	}

}
