package com.gentics.mesh.core.verticle.webroot;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.error.EntityNotFoundException;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;

import io.vertx.core.Future;

@Component
public class WebRootHandler {

	@Autowired
	private WebRootService webrootService;

	@Autowired
	private Database db;

	public void handleGetPath(ActionContext ac) {

		String path = ac.getParameter("param0");
		String projectName = ac.getProject().getName();
		MeshAuthUser requestUser = ac.getUser();
		List<String> languageTags = ac.getSelectedLanguageTags();

		Mesh.vertx().executeBlocking((Future<Node> bch) -> {
			try (Trx tx = db.trx()) {
				Path nodePath = webrootService.findByProjectPath(ac, projectName, path);
				PathSegment lastSegment = nodePath.getLast();

				if (lastSegment != null) {
					Node node = tx.getGraph().frameElement(lastSegment.getVertex(), Node.class);
					if (node == null) {
						String message = ac.i18n("node_not_found_for_path", path);
						throw new EntityNotFoundException(message);
					}

					requestUser.isAuthorised(node, READ_PERM, rh -> {
						languageTags.add(lastSegment.getLanguageTag());
						if (rh.result()) {
							bch.complete(node);
						} else {
							bch.fail(new HttpStatusCodeErrorException(FORBIDDEN, ac.i18n("error_missing_perm", node.getUuid())));
						}
					});

				} else {
					throw new EntityNotFoundException(ac.i18n("node_not_found_for_path", path));
				}
			}
		} , arh -> {
			if (arh.failed()) {
				ac.fail(arh.cause());
			}
			/* TODO copy this to all other handlers. We need to catch async errors as well elsewhere */
			if (arh.succeeded()) {
				Node node = arh.result();
				node.transformToRest(ac, th -> {
					if (hasSucceeded(ac, th)) {
						ac.send(JsonUtil.toJson(th.result()));
					}
				});
			}
		});
	}

}
