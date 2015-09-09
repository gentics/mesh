package com.gentics.mesh.search;

import static com.gentics.mesh.core.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.POST;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.AbstractListResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.handler.ActionContext;

import io.vertx.ext.web.Route;

@Component
@Scope("singleton")
@SpringVerticle
public class SearchVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private SearchRestHandler searchHandler;

	public SearchVerticle() {
		super("search");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addSearchEndpoints();
	}

	private void addSearchEndpoints() {
		try (Trx tx = db.trx()) {
			addSearch("users", boot.meshRoot().getUserRoot(), UserListResponse.class);
			addSearch("groups", boot.meshRoot().getGroupRoot(), GroupListResponse.class);
			addSearch("roles", boot.meshRoot().getRoleRoot(), RoleListResponse.class);
			addSearch("nodes", boot.meshRoot().getNodeRoot(), NodeListResponse.class);
			addSearch("tags", boot.meshRoot().getTagRoot(), TagListResponse.class);
			addSearch("tagFamilies", boot.meshRoot().getTagFamilyRoot(), TagFamilyListResponse.class);
			addSearch("projects", boot.meshRoot().getProjectRoot(), ProjectListResponse.class);
			addSearch("schemas", boot.meshRoot().getSchemaContainerRoot(), SchemaListResponse.class);
			addSearch("microschemas", boot.meshRoot().getMicroschemaContainerRoot(), MicroschemaListResponse.class);
		}
	}

	private <T extends GenericVertex<TR>, TR extends RestModel, RL extends AbstractListResponse<TR>> void addSearch(String typeName,
			RootVertex<T> root, Class<RL> classOfRL) {
		Route postRoute = route("/" + typeName).method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		postRoute.handler(rc -> {
			try {
				searchHandler.handleSearch(ActionContext.create(rc), root, classOfRL);
			} catch (Exception e) {
				//fail(rc, "search_error_query");
				rc.fail(e);
			}
		});
	}

}
