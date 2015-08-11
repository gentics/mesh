package com.gentics.mesh.core.verticle.project;

import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.util.VerticleHelper.getPagingInfo;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.ext.web.Route;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.verticle.handler.TagFamilyCrudHandler;

@Component
@Scope("singleton")
@SpringVerticle
public class ProjectTagFamilyVerticle extends AbstractProjectRestVerticle {

	@Autowired
	private TagFamilyCrudHandler crudHandler;

	public ProjectTagFamilyVerticle() {
		super("tagFamilies");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addReadTagsHandler();
		addReadHandler();
		addCreateHandler();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addReadTagsHandler() {
		Route route = route("/:tagFamilyUuid/tags").method(GET).produces(APPLICATION_JSON);
		route.handler(rc -> {
			Project project = getProject(rc);
			MeshAuthUser requestUser = getUser(rc);
			PagingInfo pagingInfo = getPagingInfo(rc);

			// TODO this is not checking for the project name and project relationship. We _need_ to fix this!
			loadObject(rc, "tagFamilyUuid", READ_PERM, project.getTagFamilyRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					TagFamily tagFamily = rh.result();
					try {
						Page<? extends Tag> tagPage = tagFamily.getTags(requestUser, pagingInfo);
						transformAndResponde(rc, tagPage, new TagListResponse());
					} catch (Exception e) {
						rc.fail(e);
					}
				}
			});

		});
	}

	private void addDeleteHandler() {
		Route deleteRoute = route("/:uuid").method(DELETE).produces(APPLICATION_JSON);
		deleteRoute.handler(rc -> {
			crudHandler.handleDelete(rc);
		});
	}

	private void addReadHandler() {
		Route readRoute = route("/:uuid").method(GET).produces(APPLICATION_JSON);
		readRoute.handler(rc -> {
			crudHandler.handleRead(rc);
		});

		Route readAllRoute = route().method(GET).produces(APPLICATION_JSON);
		readAllRoute.handler(rc -> {
			crudHandler.handleReadList(rc);
		});

	}

	private void addCreateHandler() {
		Route createRoute = route().method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		createRoute.handler(rc -> {
			crudHandler.handleCreate(rc);
		});
	}

	private void addUpdateHandler() {
		Route updateRoute = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		updateRoute.handler(rc -> {
			crudHandler.handleUpdate(rc);
		});
	}
}
