package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.AsyncResult;
import io.vertx.ext.web.Route;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.impl.TagFamilyImpl;
import com.gentics.mesh.json.JsonUtil;

@Component
@Scope("singleton")
@SpringVerticle
public class TagFamilyVerticle extends AbstractProjectRestVerticle {

	public TagFamilyVerticle() {
		super("tagFamilies");
	}

	@Override
	public void registerEndPoints() throws Exception {
		addReadHandler();
		addCreateHandler();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addDeleteHandler() {
		Route deleteRoute = route("/:uuid").method(DELETE).produces(APPLICATION_JSON);
		deleteRoute.handler(rc -> {

		});
	}

	private void addReadHandler() {
		Route readRoute = route("/:uuid").method(GET).produces(APPLICATION_JSON);
		readRoute.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			MeshAuthUser requestUser = getUser(rc);

			rcs.loadObject(rc, "uuid", projectName, READ_PERM, TagFamilyImpl.class, (AsyncResult<TagFamily> rh) -> {
			}, trh -> {
				if (trh.failed()) {
					rc.fail(trh.cause());
				}
				TagFamily tagFamily = trh.result();
				rc.response().setStatusCode(200).end(JsonUtil.toJson(tagFamily.transformToRest(requestUser)));
			});

		});

		Route readAllRoute = route().method(GET).produces(APPLICATION_JSON);
		readAllRoute.handler(rc -> {

		});

	}

	private void addCreateHandler() {
		Route createRoute = route().method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		createRoute.handler(rc -> {

		});
	}

	private void addUpdateHandler() {
		Route updateRoute = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		updateRoute.handler(rc -> {

		});
	}
}
