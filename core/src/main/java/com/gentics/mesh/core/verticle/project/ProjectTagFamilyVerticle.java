package com.gentics.mesh.core.verticle.project;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.util.RoutingContextHelper.getPagingInfo;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.delete;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.ext.web.Route;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractProjectRestVerticle;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.util.BlueprintTransaction;

@Component
@Scope("singleton")
@SpringVerticle
public class ProjectTagFamilyVerticle extends AbstractProjectRestVerticle {

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
			delete(rc, "uuid", "tagfamily_deleted", getProject(rc).getTagFamilyRoot());
		});
	}

	private void addReadHandler() {
		Route readRoute = route("/:uuid").method(GET).produces(APPLICATION_JSON);
		readRoute.handler(rc -> {
			loadTransformAndResponde(rc, "uuid", READ_PERM, getProject(rc).getTagFamilyRoot());
		});

		Route readAllRoute = route().method(GET).produces(APPLICATION_JSON);
		readAllRoute.handler(rc -> {
			Project project = getProject(rc);
			loadTransformAndResponde(rc, project.getTagFamilyRoot(), new TagFamilyListResponse());
		});

	}

	private void addCreateHandler() {
		Route createRoute = route().method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		createRoute.handler(rc -> {
			Project project = getProject(rc);
			MeshAuthUser requestUser = getUser(rc);
			TagFamilyCreateRequest requestModel = fromJson(rc, TagFamilyCreateRequest.class);

			if (StringUtils.isEmpty(requestModel.getName())) {
				rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "tagfamily_name_not_set")));
			} else {
				TagFamilyRoot root = project.getTagFamilyRoot();
				/* TODO check for null */
				if (requestUser.hasPermission(root, CREATE_PERM)) {
					TagFamily tagFamily = null;
					try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
						tagFamily = root.create(requestModel.getName());
						root.addTagFamily(tagFamily);
						requestUser.addCRUDPermissionOnRole(root, CREATE_PERM, tagFamily);
						tx.success();
					}
					transformAndResponde(rc, tagFamily);
				} else {
					rc.fail(new InvalidPermissionException(i18n.get(rc, "error_missing_perm", root.getUuid())));
				}
			}
		});
	}

	private void addUpdateHandler() {
		Route updateRoute = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		updateRoute.handler(rc -> {
			Project project = getProject(rc);
			TagFamilyUpdateRequest requestModel = fromJson(rc, TagFamilyUpdateRequest.class);

			if (StringUtils.isEmpty(requestModel.getName())) {
				rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "tagfamily_name_not_set")));
			} else {
				loadObject(rc, "uuid", UPDATE_PERM, project.getTagFamilyRoot(), rh -> {
					if (hasSucceeded(rc, rh)) {
						try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
							TagFamily tagFamily = rh.result();
							tagFamily.setName(requestModel.getName());
							tx.success();
							transformAndResponde(rc, tagFamily);
						}
					}
				});
			}

		});
	}
}
