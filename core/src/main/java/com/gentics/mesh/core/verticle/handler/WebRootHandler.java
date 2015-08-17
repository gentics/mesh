package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.util.VerticleHelper.getProjectName;
import static com.gentics.mesh.util.VerticleHelper.getSelectedLanguageTags;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.error.EntityNotFoundException;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

@Component
public class WebRootHandler {

	@Autowired
	private WebRootService webrootService;

	@Autowired
	private I18NService i18n;

	@Autowired
	private Database database;

	public void handleGetPath(RoutingContext rc) {

		String path = rc.request().params().get("param0");
		String projectName = getProjectName(rc);
		MeshAuthUser requestUser = getUser(rc);
		List<String> languageTags = getSelectedLanguageTags(rc);

		Mesh.vertx().executeBlocking((Future<Node> bch) -> {
			Path nodePath = webrootService.findByProjectPath(rc, projectName, path);
			PathSegment lastSegment = nodePath.getLast();

			if (lastSegment != null) {
				try (Trx tx = new Trx(database)) {
					Node node = tx.getGraph().frameElement(lastSegment.getVertex(), Node.class);
					if (node == null) {
						String message = i18n.get(rc, "node_not_found_for_path", path);
						throw new EntityNotFoundException(message);
					}

					requestUser.isAuthorised(node, READ_PERM, rh -> {
						languageTags.add(lastSegment.getLanguageTag());
						if (rh.result()) {
							bch.complete(node);
						} else {
							bch.fail(new HttpStatusCodeErrorException(FORBIDDEN, i18n.get(rc, "error_missing_perm", node.getUuid())));
						}
					});
				}

			} else {
				throw new EntityNotFoundException(i18n.get(rc, "node_not_found_for_path", path));
			}
		} , arh -> {
			if (arh.failed()) {
				rc.fail(arh.cause());
			}
			/* TODO copy this to all other handlers. We need to catch async errors as well elsewhere */
			if (arh.succeeded()) {
				Node node = arh.result();
				node.transformToRest(rc, th -> {
					if (hasSucceeded(rc, th)) {
						rc.response().end(JsonUtil.toJson(th.result()));
					}
				});
			}
		});
	}

}
