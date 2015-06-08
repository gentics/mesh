package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.GET;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.apex.Route;

import java.util.List;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.auth.TPMeshPermission;
import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.data.service.LanguageService;
import com.gentics.mesh.core.data.service.TagService;
import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.error.EntityNotFoundException;
import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;

@Component
@Scope("singleton")
@SpringVerticle
public class WebRootVerticle extends AbstractProjectRestVerticle {

	private static final Logger log = LoggerFactory.getLogger(WebRootVerticle.class);

	@Autowired
	private TagService tagService;

	@Autowired
	private LanguageService languageService;

	@Autowired
	private WebRootService webrootService;

	protected WebRootVerticle() {
		super("webroot");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addPathHandler();
	}

	private Route pathRoute() {
		return getRouter().routeWithRegex("\\/(.*)");
	}

	// TODO findbyproject path should also handle files and contents and store the type of the segment
	// TODO last segment can also be a file or a content. Handle this
	private void addPathHandler() {

		pathRoute().method(GET).produces(APPLICATION_JSON).handler(rc -> {
			String path = rc.request().params().get("param0");
			String projectName = rcs.getProjectName(rc);
			List<String> languageTags = rcs.getSelectedLanguageTags(rc);

			vertx.executeBlocking((Future<MeshNode> bch) -> {
				Path nodePath = webrootService.findByProjectPath(rc, projectName, path);
				PathSegment lastSegment = nodePath.getLast();

				if (lastSegment != null) {
					//					try (Transaction tx = graphDb.beginTx()) {

					MeshNode node = framedGraph.frame(lastSegment.getVertex(), MeshNode.class);
					if (node == null) {
						String message = i18n.get(rc, "node_not_found_for_path", path);
						throw new EntityNotFoundException(message);
					}

					rc.session().hasPermission(new TPMeshPermission(node, PermissionType.READ).toString(), rh -> {
						languageTags.add(lastSegment.getLanguageTag());
						if (rh.result()) {
							bch.complete(node);
						} else {
							bch.fail(new HttpStatusCodeErrorException(403, i18n.get(rc, "error_missing_perm", node.getUuid())));
						}
					});

					//						tx.success();
					//					}
				} else {
					throw new EntityNotFoundException(i18n.get(rc, "node_not_found_for_path", path));
				}
			}, arh -> {
				if (arh.failed()) {
					rc.fail(arh.cause());
				}
				/* TODO copy this to all other handlers. We need to catch async errors as well elsewhere */
				if (arh.succeeded()) {
					MeshNode node = arh.result();
					rc.response().end(toJson(nodeService.transformToRest(rc, node)));

				}
			});

		});

	}

}
