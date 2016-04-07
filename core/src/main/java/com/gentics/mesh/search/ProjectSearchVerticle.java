package com.gentics.mesh.search;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static io.vertx.core.http.HttpMethod.POST;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.search.index.IndexHandler;

import io.vertx.ext.web.Route;

/**
 * Verticle that adds REST endpoints for project specific search (for nodes, tags and tagFamilies)
 */
@Component
@Scope("singleton")
@SpringVerticle
public class ProjectSearchVerticle extends AbstractProjectRestVerticle {
	@Autowired
	private SearchRestHandler searchHandler;

	@Autowired
	private IndexHandlerRegistry registry;

	/**
	 * Create an instance
	 */
	public ProjectSearchVerticle() {
		super("search");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addSearchEndpoints();
	}

	/**
	 * Add various search endpoints using the aggregation nodes.
	 */
	private void addSearchEndpoints() {
		db.noTrx(() -> {
			registerSearchHandler("nodes", boot.meshRoot().getNodeRoot(), NodeListResponse.class, Node.TYPE);
			registerSearchHandler("tags", boot.meshRoot().getTagRoot(), TagListResponse.class, Tag.TYPE);
			registerSearchHandler("tagFamilies", boot.meshRoot().getTagFamilyRoot(), TagFamilyListResponse.class, TagFamily.TYPE);
			return null;
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
	 * @param indexHandlerKey index handler key
	 */
	private <T extends MeshCoreVertex<TR, T>, TR extends RestModel, RL extends ListResponse<TR>> void registerSearchHandler(String typeName,
			RootVertex<T> root, Class<RL> classOfRL, String indexHandlerKey) {
		Route postRoute = route("/" + typeName).method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		postRoute.handler(rc -> {
			try {
				IndexHandler indexHandler = registry.get(indexHandlerKey);
				InternalActionContext ac = InternalActionContext.create(rc);
				searchHandler.handleSearch(ac, root, classOfRL, indexHandler.getAffectedIndices(ac));
			} catch (Exception e) {
				rc.fail(e);
			}
		});
	}
}
