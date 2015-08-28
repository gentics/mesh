package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.util.VerticleHelper.fail;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.IndexedVertex;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.TagFieldContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.GenericFieldContainerNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class TagImpl extends GenericFieldContainerNode<TagResponse>implements Tag, IndexedVertex {

	private static final Logger log = LoggerFactory.getLogger(TagImpl.class);

	public static final String DEFAULT_TAG_LANGUAGE_TAG = "en";

	@Override
	public String getType() {
		return Tag.TYPE;
	}

	@Override
	public List<? extends Node> getNodes() {
		return in(HAS_TAG).has(NodeImpl.class).toListExplicit(NodeImpl.class);
	}

	@Override
	public List<? extends TagFieldContainer> getFieldContainers() {
		return out(HAS_FIELD_CONTAINER).has(TagFieldContainerImpl.class).toListExplicit(TagFieldContainerImpl.class);
	}

	@Override
	public TagFieldContainer getFieldContainer(Language language) {
		return getFieldContainer(language, TagFieldContainerImpl.class);
	}

	@Override
	public TagFieldContainer getOrCreateFieldContainer(Language language) {
		return getOrCreateFieldContainer(language, TagFieldContainerImpl.class);
	}

	@Override
	public String getName() {
		return getFieldContainer(BootstrapInitializer.getBoot().languageRoot().getTagDefaultLanguage()).getName();
	}

	@Override
	public void setName(String name) {
		getOrCreateFieldContainer(BootstrapInitializer.getBoot().languageRoot().getTagDefaultLanguage()).setName(name);
	}

	@Override
	public void removeNode(Node node) {
		unlinkIn(node.getImpl(), HAS_TAG);
	}

	@Override
	public Tag transformToRest(RoutingContext rc, Handler<AsyncResult<TagResponse>> resultHandler) {
		TagResponse restTag = new TagResponse();

		restTag.setPermissions(getUser(rc).getPermissionNames(this));
		restTag.setUuid(getUuid());

		TagFamily tagFamily = getTagFamily();

		if (tagFamily != null) {
			TagFamilyReference tagFamilyReference = new TagFamilyReference();
			tagFamilyReference.setName(tagFamily.getName());
			tagFamilyReference.setUuid(tagFamily.getUuid());
			restTag.setTagFamilyReference(tagFamilyReference);
		}

		User creator = getCreator();
		if (creator != null) {
			restTag.setCreator(creator.transformToUserReference());
		}

		User editor = getEditor();
		if (editor != null) {
			restTag.setEditor(editor.transformToUserReference());
		}

		restTag.getFields().setName(getName());

		resultHandler.handle(Future.succeededFuture(restTag));

		return this;
	}

	@Override
	public void setTagFamily(TagFamily tagFamily) {
		linkOut(tagFamily.getImpl(), HAS_TAGFAMILY_ROOT);
	}

	@Override
	public TagFamily getTagFamily() {
		return in(HAS_TAG).has(TagFamilyImpl.class).nextOrDefaultExplicit(TagFamilyImpl.class, null);
	}

	@Override
	public void delete() {
		if (log.isDebugEnabled()) {
			log.debug("Deleting tag {" + getName() + "}");
		}
		addDeleteFromIndexActions();
		getVertex().remove();
	}

	@Override
	public void addDeleteFromIndexActions() {
		SearchQueue queue = BootstrapInitializer.getBoot().meshRoot().getSearchQueue();
		//TODO use a dedicated uuid or timestamp for batched to avoid collisions
		SearchQueueBatch batch = queue.createBatch(getUuid());

		batch.addEntry(this, SearchQueueEntryAction.DELETE_ACTION);
		for (Node node : getNodes()) {
			batch.addEntry(node, SearchQueueEntryAction.UPDATE_ACTION);
		}
		batch.addEntry(getTagFamily(), SearchQueueEntryAction.UPDATE_ACTION);
	}

	@Override
	public void addUpdateIndexActions() {
		SearchQueue queue = BootstrapInitializer.getBoot().meshRoot().getSearchQueue();
		//TODO use a dedicated uuid or timestamp for batched to avoid collisions
		SearchQueueBatch batch = queue.createBatch(getUuid());

		batch.addEntry(this, SearchQueueEntryAction.UPDATE_ACTION);
		for (Node node : getNodes()) {
			batch.addEntry(node, SearchQueueEntryAction.UPDATE_ACTION);
		}
		batch.addEntry(getTagFamily(), SearchQueueEntryAction.UPDATE_ACTION);
	}

	@Override
	public Page<? extends Node> findTaggedNodes(MeshAuthUser requestUser, List<String> languageTags, PagingInfo pagingInfo)
			throws InvalidArgumentException {

		VertexTraversal<?, ?, ?> traversal = in(HAS_TAG).has(NodeImpl.class).mark().in(READ_PERM.label()).out(HAS_ROLE).in(HAS_USER)
				.retain(requestUser.getImpl()).back();
		VertexTraversal<?, ?, ?> countTraversal = in(HAS_TAG).has(NodeImpl.class).mark().in(READ_PERM.label()).out(HAS_ROLE).in(HAS_USER)
				.retain(requestUser.getImpl()).back();
		Page<? extends Node> nodePage = TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, NodeImpl.class);
		return nodePage;
	}

	@Override
	public TagReference tansformToTagReference() {
		TagReference reference = new TagReference();
		reference.setUuid(getUuid());
		reference.setName(getName());
		return reference;
	}

	@Override
	public void update(RoutingContext rc) {
		I18NService i18n = I18NService.getI18n();
		Database db = MeshSpringConfiguration.getMeshSpringConfiguration().database();

		TagUpdateRequest requestModel = fromJson(rc, TagUpdateRequest.class);
		TagFamilyReference reference = requestModel.getTagFamilyReference();
		try (Trx txUpdate = db.trx()) {
			boolean updateTagFamily = false;
			if (reference != null) {
				// Check whether a uuid was specified and whether the tag family changed
				if (!isEmpty(reference.getUuid())) {
					if (!getTagFamily().getUuid().equals(reference.getUuid())) {
						updateTagFamily = true;
					}
				}
			}

			String newTagName = requestModel.getFields().getName();
			if (isEmpty(newTagName)) {
				fail(rc, "tag_name_not_set");
				txUpdate.failure();
				return;
			} else {
				TagFamily tagFamily = getTagFamily();
				Tag foundTagWithSameName = tagFamily.findTagByName(newTagName);
				if (foundTagWithSameName != null && !foundTagWithSameName.getUuid().equals(getUuid())) {
					rc.fail(new HttpStatusCodeErrorException(CONFLICT,
							i18n.get(rc, "tag_create_tag_with_same_name_already_exists", newTagName, tagFamily.getName())));
					txUpdate.failure();
					return;
				}
				setEditor(getUser(rc));
				setLastEditedTimestamp(System.currentTimeMillis());
				setName(requestModel.getFields().getName());
				if (updateTagFamily) {
					// TODO update the tagfamily
				}
			}
			addUpdateIndexActions();
			txUpdate.success();
		}
	}

}
