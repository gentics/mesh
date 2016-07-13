package com.gentics.mesh.search;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.common.ListResponse;
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
import com.gentics.mesh.rest.Endpoint;
import com.gentics.mesh.search.index.IndexHandler;

import io.vertx.ext.web.Route;
import rx.functions.Func0;

@Component
@Scope("singleton")
@SpringVerticle
public class SearchVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private SearchRestHandler searchHandler;

	@Autowired
	private IndexHandlerRegistry registry;

	public SearchVerticle() {
		super("search");
	}

	@Override
	public void registerEndPoints() throws Exception {
		if (springConfiguration != null) {
			route("/*").handler(springConfiguration.authHandler());
		}
		addSearchEndpoints();
	}

	/**
	 * Add various search endpoints using the aggregation nodes.
	 */
	private void addSearchEndpoints() {
		registerSearchHandler("users", () -> boot.meshRoot().getUserRoot(), UserListResponse.class, User.TYPE);
		registerSearchHandler("groups", () -> boot.meshRoot().getGroupRoot(), GroupListResponse.class, Group.TYPE);
		registerSearchHandler("roles", () -> boot.meshRoot().getRoleRoot(), RoleListResponse.class, Role.TYPE);
		registerSearchHandler("nodes", () -> boot.meshRoot().getNodeRoot(), NodeListResponse.class, Node.TYPE);
		registerSearchHandler("tags", () -> boot.meshRoot().getTagRoot(), TagListResponse.class, Tag.TYPE);
		registerSearchHandler("tagFamilies", () -> boot.meshRoot().getTagFamilyRoot(), TagFamilyListResponse.class, TagFamily.TYPE);
		registerSearchHandler("projects", () -> boot.meshRoot().getProjectRoot(), ProjectListResponse.class, Project.TYPE);
		registerSearchHandler("schemas", () -> boot.meshRoot().getSchemaContainerRoot(), SchemaListResponse.class, SchemaContainer.TYPE);
		registerSearchHandler("microschemas", () -> boot.meshRoot().getMicroschemaContainerRoot(), MicroschemaListResponse.class,
				MicroschemaContainer.TYPE);
		addAdminHandlers();
	}

	private void addAdminHandlers() {
		Route statusRoute = route("/status").method(GET).produces(APPLICATION_JSON);
		statusRoute.handler(rc -> {
			searchHandler.handleStatus(InternalActionContext.create(rc));
		});

		Route reindexRoute = route("/reindex").method(GET).produces(APPLICATION_JSON);
		reindexRoute.handler(rc -> {
			searchHandler.handleReindex(InternalActionContext.create(rc));
		});

	}

	/**
	 * Register the selected search handler.
	 * 
	 * @param typeName
	 *            Name of the search endpoint
	 * @param root
	 *            Aggregation node that should be used to load the objects that were found within the search index
	 * @param classOfRL
	 *            Class of matching list response
	 * @param indexHandlerKey
	 *            key of the index handlers
	 */
	private <T extends MeshCoreVertex<TR, T>, TR extends RestModel, RL extends ListResponse<TR>> void registerSearchHandler(String typeName,
			Func0<RootVertex<T>> root, Class<RL> classOfRL, String indexHandlerKey) {
		Endpoint endpoint = createEndpoint();
		endpoint.path("/" + typeName);
		endpoint.method(POST);
		endpoint.description("Invoke a search query for " + typeName + " and return a paged list response.");
		endpoint.consumes(APPLICATION_JSON);
		endpoint.produces(APPLICATION_JSON);
		endpoint.handler(rc -> {
			try {
				IndexHandler indexHandler = registry.get(indexHandlerKey);
				InternalActionContext ac = InternalActionContext.create(rc);
				searchHandler.handleSearch(ac, root, classOfRL, indexHandler.getAffectedIndices(ac), indexHandler.getReadPermission(ac));
			} catch (Exception e) {
				// fail(rc, "search_error_query");
				rc.fail(e);
			}
		});
	}

}
