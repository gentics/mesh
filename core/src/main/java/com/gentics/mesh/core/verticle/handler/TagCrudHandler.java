package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.search.SearchQueue.SEARCH_QUEUE_ENTRY_ADDRESS;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.util.VerticleHelper.fail;
import static com.gentics.mesh.util.VerticleHelper.getPagingInfo;
import static com.gentics.mesh.util.VerticleHelper.getSelectedLanguageTags;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.loadObjectByUuid;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

@Component
public class TagCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleCreate(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {

			Project project = getProject(rc);
			Future<Tag> tagCreated = Future.future();
			TagCreateRequest requestModel = fromJson(rc, TagCreateRequest.class);
			String tagName = requestModel.getFields().getName();
			if (StringUtils.isEmpty(tagName)) {
				fail(rc, "tag_name_not_set");
			} else {
				TagFamilyReference reference = requestModel.getTagFamilyReference();
				if (reference == null || isEmpty(reference.getUuid())) {
					fail(rc, "tag_tagfamily_reference_not_set");
				} else {
					loadObjectByUuid(rc, requestModel.getTagFamilyReference().getUuid(), CREATE_PERM, project.getTagFamilyRoot(), rh -> {
						if (hasSucceeded(rc, rh)) {
							//							try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
							TagFamily tagFamily = rh.result();
							if (tagFamily.findTagByName(tagName) != null) {
								rc.fail(new HttpStatusCodeErrorException(CONFLICT,
										i18n.get(rc, "tag_create_tag_with_same_name_already_exists", tagName, tagFamily.getName())));
								return;
							}
							Tag newTag = tagFamily.create(requestModel.getFields().getName(), project, getUser(rc));
							getUser(rc).addCRUDPermissionOnRole(project.getTagFamilyRoot(), CREATE_PERM, newTag);
							project.getTagRoot().addTag(newTag);
							tagCreated.complete(newTag);
							searchQueue().put(newTag.getUuid(), Tag.TYPE, SearchQueueEntryAction.CREATE_ACTION);
							vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, null);
							transformAndResponde(rc, newTag);
							//							}
						}
					});
				}
			}
		}
	}

	@Override
	public void handleDelete(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			delete(rc, "uuid", "tag_deleted", getProject(rc).getTagRoot());
		}
	}

	@Override
	public void handleUpdate(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			Project project = getProject(rc);
			loadObject(rc, "uuid", UPDATE_PERM, project.getTagRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					Tag tag = rh.result();

					TagUpdateRequest requestModel = fromJson(rc, TagUpdateRequest.class);

					TagFamilyReference reference = requestModel.getTagFamilyReference();
					boolean updateTagFamily = false;
					if (reference != null) {
						// Check whether a uuid was specified and whether the tag family changed
						if (!isEmpty(reference.getUuid())) {
							if (!tag.getTagFamily().getUuid().equals(reference.getUuid())) {
								updateTagFamily = true;
							}
						}
					}

					String newTagName = requestModel.getFields().getName();
					if (StringUtils.isEmpty(newTagName)) {
						fail(rc, "tag_name_not_set");
					} else {
						TagFamily tagFamily = tag.getTagFamily();
						Tag foundTagWithSameName = tagFamily.findTagByName(newTagName);
						if (foundTagWithSameName != null && !foundTagWithSameName.getUuid().equals(tag.getUuid())) {
							rc.fail(new HttpStatusCodeErrorException(CONFLICT,
									i18n.get(rc, "tag_create_tag_with_same_name_already_exists", newTagName, tagFamily.getName())));
							return;
						}
						tag.setEditor(getUser(rc));
						tag.setLastEditedTimestamp(System.currentTimeMillis());
						// try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
						tag.setName(requestModel.getFields().getName());
						if (updateTagFamily) {
							// TODO update the tagfamily
						}
						searchQueue().put(tag.getUuid(), Tag.TYPE, SearchQueueEntryAction.UPDATE_ACTION);
						vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, null);
						// tx.success();
						// }
						transformAndResponde(rc, tag);
					}
				}
			});
		}
	}

	@Override
	public void handleRead(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			Project project = getProject(rc);
			loadTransformAndResponde(rc, "uuid", READ_PERM, project.getTagRoot());
		}
	}

	@Override
	public void handleReadList(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			Project project = getProject(rc);
			loadTransformAndResponde(rc, project.getTagRoot(), new TagListResponse());
		}
	}

	public void handleTaggedNodesList(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			Project project = getProject(rc);
			loadObject(rc, "uuid", READ_PERM, project.getTagRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					Tag tag = rh.result();
					Page<? extends Node> page;
					try {
						page = tag.findTaggedNodes(getUser(rc), getSelectedLanguageTags(rc), getPagingInfo(rc));
						transformAndResponde(rc, page, new NodeListResponse());
					} catch (Exception e) {
						//TODO i18n - exception handling
						fail(rc, "Could not load nodes");
					}
				}
			});
		}
	}

}
