package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static io.vertx.core.http.HttpMethod.*;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;

@Component
@Scope("singleton")
@SpringVerticle
public class MicroschemaVerticle extends AbstractCoreApiVerticle {

	protected MicroschemaVerticle() {
		super("microschemas");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addProjectHandlers();

		addCreateHandler();
		addReadHandlers();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addReadHandlers() {
		route("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			if (StringUtils.isEmpty(uuid)) {
				rc.next();
			} else {
				loadTransformAndReturn(rc, "uuid", READ_PERM, boot.microschemaContainerRoot());
			}
		});

		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			loadTransformAndResponde(rc, boot.microschemaContainerRoot(), new MicroschemaListResponse());
		});
	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			delete(rc, "uuid", "group_deleted", boot.microschemaContainerRoot());
		});
	}

	private void addUpdateHandler() {
		// TODO Auto-generated method stub

	}

	private void addCreateHandler() {
		// TODO Auto-generated method stub

	}

	private void addProjectHandlers() {
		// TODO Auto-generated method stub

	}

}
