package com.gentics.mesh.core.verticle.project;

import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.FileUpload;

import java.util.Set;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.json.JsonUtil;

@Component
@Scope("singleton")
@SpringVerticle
public class ProjectBinaryVerticle extends AbstractProjectRestVerticle {

	private static final Logger log = LoggerFactory.getLogger(ProjectBinaryVerticle.class);

	public ProjectBinaryVerticle() {
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
			Project project = getProject(rc);
			loadObject(rc, "uuid", UPDATE_PERM, project.getNodeRoot(), rh -> {
				Node node = rh.result();
				Set<FileUpload> fileUploads = rc.fileUploads();
				for (FileUpload ul : fileUploads) {
					System.out.println(ul.fileName());
				}
				rc.response().setStatusCode(200).end(JsonUtil.toJson(new GenericMessageResponse(i18n.get(rc, "binary_field_updated", node.getUuid()))));
			});
		});
	}

}