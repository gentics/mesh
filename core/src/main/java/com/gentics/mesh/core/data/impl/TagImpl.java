package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.TagFieldContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.GenericFieldContainerNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.util.BlueprintTransaction;

public class TagImpl extends GenericFieldContainerNode<TagResponse> implements Tag {

	public static final String DEFAULT_TAG_LANGUAGE_TAG = "en";

	@Override
	public String getType() {
		return Tag.TYPE;
	}

	public List<? extends Node> getNodes() {
		return in(HAS_TAG).has(NodeImpl.class).toListExplicit(NodeImpl.class);
	}

	public List<? extends TagFieldContainer> getFieldContainers() {
		return out(HAS_FIELD_CONTAINER).has(TagFieldContainerImpl.class).toListExplicit(TagFieldContainerImpl.class);
	}

	public TagFieldContainer getFieldContainer(Language language) {
		return getFieldContainer(language, TagFieldContainerImpl.class);
	}

	public TagFieldContainer getOrCreateFieldContainer(Language language) {
		return getOrCreateFieldContainer(language, TagFieldContainerImpl.class);
	}

	public String getName() {
		return getFieldContainer(BootstrapInitializer.getBoot().languageRoot().getTagDefaultLanguage()).getName();
	}

	public void setName(String name) {
		getOrCreateFieldContainer(BootstrapInitializer.getBoot().languageRoot().getTagDefaultLanguage()).setName(name);
	}

	public void removeNode(Node node) {
		unlinkIn(node.getImpl(), HAS_TAG);
	}

	@Override
	public Tag transformToRest(RoutingContext rc, Handler<AsyncResult<TagResponse>> resultHandler) {
		Vertx vertx = Mesh.vertx();
		//		vertx.executeBlocking(bc -> {
		TagResponse restTag = new TagResponse();

		try (BlueprintTransaction tx = new BlueprintTransaction(MeshSpringConfiguration.getMeshSpringConfiguration()
				.getFramedThreadedTransactionalGraph())) {
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
			tx.success();
		}

		// if (currentDepth < info.getMaxDepth()) {
		// }
		// if (info.isIncludeTags()) {
		// TagTraversalConsumer tagConsumer = new TagTraversalConsumer(info, currentDepth, restTag, tasks);
		// tag.getTags().parallelStream().forEachOrdered(tagConsumer);
		// } else {
		// restTag.setTags(null);
		// }
		//
		// if (info.isIncludeContents()) {
		// ContentTraversalConsumer contentConsumer = new ContentTraversalConsumer(info, currentDepth, restTag, tasks);
		// tag.getContents().parallelStream().forEachOrdered(contentConsumer);
		// } else {
		// restTag.setContents(null);
		// }
		//
		// if (info.isIncludeChildTags()) {
		// TagTraversalConsumer tagConsumer = new TagTraversalConsumer(info, currentDepth, restTag, tasks);
		// tag.getChildTags().parallelStream().forEachOrdered(tagConsumer);
		// } else {
		// restTag.setChildTags(null);
		// }

		//				bc.complete(restTag);
		resultHandler.handle(Future.succeededFuture(restTag));

		//			}, resultHandler);
		return this;
	}

	public void setTagFamilyRoot(TagFamily root) {
		linkOut(root.getImpl(), HAS_TAGFAMILY_ROOT);
	}

	public TagFamily getTagFamily() {
		return in(HAS_TAG).has(TagFamilyImpl.class).nextOrDefaultExplicit(TagFamilyImpl.class, null);
	}

	public void delete() {
		e().removeAll();
		getVertex().remove();
	}

	@Override
	public Page<Node> findTaggedNodes(MeshAuthUser requestUser, List<String> languageTags, PagingInfo pagingInfo) {
		// String langFilter = getLanguageFilter("l");
		// if (languageTags == null || languageTags.isEmpty()) {
		// langFilter = "";
		// } else {
		// langFilter += " AND ";
		// }
		// String baseQuery = PERMISSION_PATTERN_ON_NODE;
		// baseQuery += "MATCH (node)-[:ASSIGNED_TO_PROJECT]->(pr:Project) ";
		// baseQuery += "MATCH (tag:Tag)-[:HAS_TAG]->(node)-[l:HAS_I18N_PROPERTIES]-(sp:I18NProperties) ";
		// baseQuery += "WHERE " + langFilter + " AND " + USER_PERMISSION_FILTER + " AND " + PROJECT_FILTER;
		//
		// String query = baseQuery + " WITH sp, node " + ORDER_BY_NAME + " RETURN DISTINCT node as n";
		// String countQuery = baseQuery + " RETURN count(DISTINCT node) as count";
		//
		// Map<String, Object> parameters = new HashMap<>();
		// parameters.put("languageTags", languageTags);
		// parameters.put("projectName", projectName);
		// parameters.put("userUuid", userUuid);
		// parameters.put("tag", tag);
		// return queryService.query(query, countQuery, parameters, pagingInfo, MeshNode.class);
		return null;
	}

	@Override
	public TagReference tansformToTagReference() {
		TagReference reference = new TagReference();
		reference.setUuid(getUuid());
		reference.setName(getName());
		return reference;
	}

	@Override
	public TagImpl getImpl() {
		return this;
	}

}
