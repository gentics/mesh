package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.json.JsonUtil.toJson;
import static com.gentics.mesh.util.RoutingContextHelper.getPagingInfo;
import static com.gentics.mesh.util.RoutingContextHelper.getSelectedLanguageTags;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Route;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
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
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.core.verticle.handler.NodeListHandler;
import com.gentics.mesh.core.verticle.handler.TagListHandler;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.BlueprintTransaction;
import com.gentics.mesh.util.RestModelPagingHelper;

/**
 * The tag verticle provides rest endpoints which allow manipulation and handling of tag related objects.
 * 
 * @author johannes2
 *
 */
@Component
@Scope("singleton")
@SpringVerticle
public class TagVerticle extends AbstractProjectRestVerticle {

	private static final Logger log = LoggerFactory.getLogger(TagVerticle.class);

	@Autowired
	private TagListHandler tagListHandler;

	@Autowired
	private NodeListHandler nodeListHandler;

	public TagVerticle() {
		super("tags");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();

		addTaggedNodesHandler();
	}

	private void addTaggedNodesHandler() {
		Route getRoute = route("/:uuid/nodes").method(GET).produces(APPLICATION_JSON);
		getRoute.handler(rc -> {
			MeshAuthUser requestUser = getUser(rc);

			nodeListHandler.handleListByTag(rc, (projectName, tag, languageTags, pagingInfo) -> {
				return tag.findTaggedNodes(requestUser, projectName, languageTags, pagingInfo);
			});
		});
	}

	// TODO fetch project specific tag
	// TODO update other fields as well?
	// TODO Update user information
	// TODO use schema and only handle those i18n properties that were specified within the schema.
	private void addUpdateHandler() {
		Route route = route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			MeshAuthUser requestUser = getUser(rc);

			rcs.loadObject(rc, "uuid", projectName, UPDATE_PERM, TagImpl.class, (AsyncResult<Tag> rh) -> {
				Tag tag = rh.result();

				TagUpdateRequest requestModel = fromJson(rc, TagUpdateRequest.class);

				if (StringUtils.isEmpty(requestModel.getFields().getName())) {
					rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "tag_name_not_set")));
					return;
				}
				try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
					tag.setName(requestModel.getFields().getName());
					tx.success();
				}
				TransformationInfo info = new TransformationInfo(requestUser, null, rc);

				rc.response().setStatusCode(200).end(toJson(tag.transformToRest(info)));
			});

		});

	}

	// TODO load project specific root tag
	// TODO handle creator
	// TODO load schema and set the reference to the tag
	// newTag.setSchemaName(request.getSchemaName());
	// TODO maybe projects should not be a set?
	private void addCreateHandler() {
		Route route = route("/").method(POST).consumes(APPLICATION_JSON).produces(APPLICATION_JSON);
		route.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			MeshAuthUser requestUser = getUser(rc);
			List<String> languageTags = getSelectedLanguageTags(rc);

			Future<Tag> tagCreated = Future.future();
			TagCreateRequest requestModel = fromJson(rc, TagCreateRequest.class);

			if (StringUtils.isEmpty(requestModel.getFields().getName())) {
				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "tag_name_not_set")));
				return;
			}

			//TODO check tag family reference for null

			rcs.loadObjectByUuid(rc, requestModel.getTagFamilyReference().getUuid(), CREATE_PERM, TagFamily.class, (AsyncResult<TagFamily> rh) -> {
				TagFamily tagFamily = rh.result();
				Tag newTag = tagFamily.create(requestModel.getFields().getName());
				Project project = boot.projectRoot().findByName(projectName);
				newTag.addProject(project);
				tagCreated.complete(newTag);
			}, trh -> {
				Tag newTag = tagCreated.result();
				TransformationInfo info = new TransformationInfo(requestUser, languageTags, rc);
				rc.response().setStatusCode(200).end(toJson(newTag.transformToRest(info)));
			});

		});
	}

	// TODO filtering, sorting
	private void addReadHandler() {
		Route route = route("/:uuid").method(GET).produces(APPLICATION_JSON);
		route.handler(rc -> {
			MeshAuthUser requestUser = getUser(rc);
			List<String> languageTags = getSelectedLanguageTags(rc);

			rcs.loadObject(rc, "uuid", READ_PERM, TagImpl.class, (AsyncResult<Tag> trh) -> {
				Tag tag = trh.result();
				TransformationInfo info = new TransformationInfo(requestUser, languageTags, rc);

				rc.response().setStatusCode(200).end(toJson(tag.transformToRest(info)));
			});
		});

		Route readAllRoute = route().method(GET).produces(APPLICATION_JSON);
		readAllRoute.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			MeshAuthUser requestUser = getUser(rc);

			List<String> languageTags = getSelectedLanguageTags(rc);
			vertx.executeBlocking((Future<TagListResponse> bcr) -> {
				TagListResponse listResponse = new TagListResponse();
				PagingInfo pagingInfo = getPagingInfo(rc);
				Page<? extends Tag> tagPage;
				try {
					tagPage = boot.tagRoot().findProjectTags(requestUser, projectName, languageTags, pagingInfo);
					for (Tag tag : tagPage) {
						TransformationInfo info = new TransformationInfo(requestUser, languageTags, rc);

						listResponse.getData().add(tag.transformToRest(info));
					}
					RestModelPagingHelper.setPaging(listResponse, tagPage, pagingInfo);
					bcr.complete(listResponse);
				} catch (Exception e) {
					bcr.fail(e);
				}
			}, arh -> {
				if (arh.failed()) {
					rc.fail(arh.cause());
					return;
				}
				TagListResponse listResponse = arh.result();
				rc.response().setStatusCode(200).end(toJson(listResponse));
			});

		});

	}

	// TODO filter by projectName
	private void addDeleteHandler() {
		Route route = route("/:uuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			String projectName = rcs.getProjectName(rc);
			rcs.loadObject(rc, "uuid", projectName, DELETE_PERM, TagImpl.class, (AsyncResult<Tag> rh) -> {
				Tag tag = rh.result();
				//				DelegatingFramedThreadedTransactionalGraph dfttg = (DelegatingFramedThreadedTransactionalGraph)tag.getGraph();
				//				dfttg.newTransaction();
				//				tag = tagService.findByUUID(tag.getUuid());
					try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
						tag.delete();
						tx.success();
					}
				}, trh -> {
					if (trh.failed()) {
						rc.fail(trh.cause());
					}
					String uuid = rc.request().params().get("uuid");
					rc.response().setStatusCode(200).end(JsonUtil.toJson(new GenericMessageResponse(i18n.get(rc, "tag_deleted", uuid))));
				});
		});
	}

}
