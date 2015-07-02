package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.data.service.LanguageService.getLanguageService;

import java.util.List;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.TagFieldContainer;
import com.gentics.mesh.core.data.generic.GenericFieldContainerNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.data.service.transformation.TransformationPool;
import com.gentics.mesh.core.data.service.transformation.tag.TagTransformationTask;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.paging.PagingInfo;

public class TagImpl extends GenericFieldContainerNode implements Tag {

	public static final String DEFAULT_TAG_LANGUAGE_TAG = "en";

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
		return getFieldContainer(getLanguageService().getTagDefaultLanguage()).getName();
	}

	public void setName(String name) {
		getOrCreateFieldContainer(getLanguageService().getTagDefaultLanguage()).setName(name);
	}

	public void removeNode(Node node) {
		unlinkIn(node.getImpl(), HAS_TAG);
	}

	public TagResponse transformToRest(TransformationInfo info) {

		TagResponse restTag = new TagResponse();
		TagTransformationTask task = new TagTransformationTask(this, info, restTag);
		TransformationPool.getPool().invoke(task);
		return restTag;
	}

	public void setTagFamilyRoot(TagFamily root) {
		linkOut(root.getImpl(), HAS_TAGFAMILY_ROOT);
	}

	public TagFamily getTagFamilyRoot() {
		return out(HAS_TAGFAMILY_ROOT).has(TagFamilyImpl.class).nextOrDefaultExplicit(TagFamilyImpl.class, null);
	}

	public void delete() {
		e().removeAll();
		getVertex().remove();
	}

	public Page<Node> findTaggedNodes(MeshAuthUser requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo) {
		// findTaggedNodes(userUuid, projectName, tag, languageTags, pagingInfo);
		return null;
	}

	public Page<Node> findTaggedNodes(MeshAuthUser requestUser, String projectName, TagImpl tag, List<String> languageTags, PagingInfo pagingInfo) {
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
	public TagReference tansformToTagReference(TransformationInfo info) {
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
