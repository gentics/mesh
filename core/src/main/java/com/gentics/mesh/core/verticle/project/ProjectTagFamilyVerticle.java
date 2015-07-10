package com.gentics.mesh.core.verticle.project;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.json.JsonUtil.toJson;
import static com.gentics.mesh.util.RoutingContextHelper.getPagingInfo;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
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
import com.gentics.mesh.core.data.impl.TagFamilyImpl;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.error.EntityNotFoundException;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.BlueprintTransaction;
import com.gentics.mesh.util.RestModelPagingHelper;

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
		addReadHandler();
		addCreateHandler();
		addUpdateHandler();
		addDeleteHandler();

		addReadTagsHandler();
	}

	private void addReadTagsHandler() {
		Route route = route("/:tagFamilyUuid/tags").method(GET).produces(APPLICATION_JSON);
		route.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			MeshAuthUser requestUser = getUser(rc);
			PagingInfo pagingInfo = getPagingInfo(rc);

			//TODO this is not checking for the project name and project relationship. We _need_ to fix this!
			rcs.loadObject(rc, "uuid", READ_PERM, TagFamilyImpl.class, (AsyncResult<TagFamily> rh) -> {
				TagFamily tagFamily = rh.result();
				TagListResponse listResponse = new TagListResponse();
				try {
					Page<? extends Tag> tagPage = tagFamily.getTags(requestUser, pagingInfo);
					for (Tag tag : tagPage) {
						TransformationInfo info = new TransformationInfo(requestUser, null, rc);
						listResponse.getData().add(tag.transformToRest(info));
					}
					RestModelPagingHelper.setPaging(listResponse, tagPage, pagingInfo);
					rc.response().setStatusCode(200).end(toJson(listResponse));
				} catch (Exception e) {
					rc.fail(e);
				}
			});

		});
	}

	private void addDeleteHandler() {
		Route deleteRoute = route("/:uuid").method(DELETE).produces(APPLICATION_JSON);
		deleteRoute.handler(rc -> {
			rcs.loadObject(rc, "uuid", DELETE_PERM, TagFamilyImpl.class, (AsyncResult<TagFamily> rh) -> {
				TagFamily tagFamily = rh.result();
				tagFamily.delete();
				String uuid = rc.request().params().get("uuid");
				rc.response().setStatusCode(200).end(JsonUtil.toJson(new GenericMessageResponse(i18n.get(rc, "tagfamily_deleted", uuid))));
			});

		});
	}

	private void addReadHandler() {
		Route readRoute = route("/:uuid").method(GET).produces(APPLICATION_JSON);
		readRoute.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			MeshAuthUser requestUser = getUser(rc);

			rcs.loadObject(rc, "uuid", READ_PERM, TagFamilyImpl.class, (AsyncResult<TagFamily> rh) -> {
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
			String projectName = rcs.getProjectName(rc);
			MeshAuthUser requestUser = getUser(rc);
			vertx.executeBlocking((Future<TagFamilyListResponse> bcr) -> {
				TagFamilyListResponse listResponse = new TagFamilyListResponse();
				PagingInfo pagingInfo = getPagingInfo(rc);
				Page<? extends TagFamily> tagfamilyPage;
				try {
					tagfamilyPage = boot.projectRoot().findByName(projectName).getTagFamilyRoot().findAll(requestUser, pagingInfo);
					for (TagFamily tagFamily : tagfamilyPage) {
						listResponse.getData().add(tagFamily.transformToRest(requestUser));
					}
					RestModelPagingHelper.setPaging(listResponse, tagfamilyPage, pagingInfo);
					bcr.complete(listResponse);
				} catch (Exception e) {
					bcr.fail(e);
				}
			}, arh -> {
				if (arh.failed()) {
					rc.fail(arh.cause());
					return;
				}
				TagFamilyListResponse listResponse = arh.result();
				rc.response().setStatusCode(200).end(toJson(listResponse));
			});

		});

	}

	private void addCreateHandler() {
		Route createRoute = route().method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		createRoute.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			MeshAuthUser requestUser = getUser(rc);
			TagFamilyCreateRequest requestModel = fromJson(rc, TagFamilyCreateRequest.class);

			if (StringUtils.isEmpty(requestModel.getName())) {
				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "tagfamily_name_not_set")));
				return;
			}
			Project project = boot.projectRoot().findByName(projectName);
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
				rc.response().end(JsonUtil.toJson(tagFamily.transformToRest(requestUser)));
			} else {
				rc.fail(new InvalidPermissionException(i18n.get(rc, "error_missing_perm", root.getUuid())));
			}
		});
	}

	private void addUpdateHandler() {
		Route updateRoute = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		updateRoute.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			MeshAuthUser requestUser = getUser(rc);
			TagFamilyUpdateRequest requestModel = fromJson(rc, TagFamilyUpdateRequest.class);

			if (StringUtils.isEmpty(requestModel.getName())) {
				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "tagfamily_name_not_set")));
				return;
			}
			String uuid = rc.request().params().get("uuid");
			Project project = boot.projectRoot().findByName(projectName);
			TagFamilyRoot root = project.getTagFamilyRoot();
			TagFamily tagFamily = root.findByUuid(uuid);
			if (tagFamily == null) {
				rc.fail(new EntityNotFoundException(i18n.get(rc, "object_not_found_for_name", requestModel.getName())));
				return;
			}
			if (requestUser.hasPermission(tagFamily, UPDATE_PERM)) {
				tagFamily.setName(requestModel.getName());
			}
			rc.response().end(JsonUtil.toJson(tagFamily.transformToRest(requestUser)));

		});
	}
}
