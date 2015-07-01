package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.json.JsonUtil.toJson;
import io.vertx.core.AsyncResult;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.FileUpload;

import java.util.Set;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.node.MeshNode;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.util.RoutingContextHelper;

@Component
@Scope("singleton")
@SpringVerticle
public class BinaryVerticle extends AbstractProjectRestVerticle {

	private static final Logger log = LoggerFactory.getLogger(BinaryVerticle.class);

	public BinaryVerticle() {
		super("binaries");
	}

	@Override
	public void registerEndPoints() throws Exception {
		addFileuploadHandler();
		addFileDownloadHandler();
	}

	private void addFileDownloadHandler() {
		route().method(HttpMethod.GET).handler(rc -> {
			// rc.response().end(chunk);
			});
	}

	private void addFileuploadHandler() {
		route("/:uuid").method(HttpMethod.POST).handler(rc -> {
			MeshAuthUser user = RoutingContextHelper.getUser(rc);
			String projectName = rcs.getProjectName(rc);
			rcs.loadObject(rc, "uuid", projectName, UPDATE_PERM, MeshNode.class, (AsyncResult<MeshNode> rh) -> {
				MeshNode node = rh.result();
				Set<FileUpload> fileUploads = rc.fileUploads();
				for (FileUpload ul : fileUploads) {
					System.out.println(ul.fileName());
				}
				rc.response().setStatusCode(200).end(toJson(new GenericMessageResponse(i18n.get(rc, "binary_field_updated", node.getUuid()))));
			});
		});
	}

}