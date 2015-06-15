package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.util.JsonUtils.toJson;
import io.vertx.core.AsyncResult;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.apex.FileUpload;

import java.util.Set;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.rest.common.response.GenericMessageResponse;

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
			String projectName = rcs.getProjectName(rc);
			rcs.loadObject(rc, "uuid", projectName, PermissionType.UPDATE, (AsyncResult<MeshNode> rh) -> {
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