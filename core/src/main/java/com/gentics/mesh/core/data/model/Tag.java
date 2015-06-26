package com.gentics.mesh.core.data.model;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_TAGFAMILY_ROOT;

import java.util.List;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.generic.GenericFieldContainerNode;
import com.gentics.mesh.core.data.model.node.MeshNode;
import com.gentics.mesh.core.data.model.root.TagFamily;
import com.gentics.mesh.core.data.service.LanguageService;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.data.service.transformation.TransformationPool;
import com.gentics.mesh.core.data.service.transformation.tag.TagTransformationTask;
import com.gentics.mesh.core.rest.tag.response.TagResponse;
import com.gentics.mesh.paging.PagingInfo;

public class Tag extends GenericFieldContainerNode {

	public static final String DEFAULT_TAG_LANGUAGE_TAG = "en";

	public List<? extends MeshNode> getNodes() {
		return in(HAS_TAG).has(MeshNode.class).toListExplicit(MeshNode.class);
	}

	public List<? extends TagFieldContainer> getFieldContainers() {
		return getFieldContainers(TagFieldContainer.class);
	}

	public TagFieldContainer getFieldContainer(Language language) {
		return getFieldContainer(language, TagFieldContainer.class);
	}

	public TagFieldContainer getOrCreateFieldContainer(Language language) {
		return getOrCreateFieldContainer(language, TagFieldContainer.class);
	}

	public String getName() {
		return getFieldContainer(LanguageService.getLanguageService().getTagDefaultLanguage()).getName();
	}

	public void setName(String name) {
		getOrCreateFieldContainer(LanguageService.getLanguageService().getTagDefaultLanguage()).setName(name);
	}

	public void removeNode(MeshNode node) {
		unlinkIn(node, HAS_TAG);
	}

	public TagResponse transformToRest(TransformationInfo info) {

		TagResponse restTag = new TagResponse();
		TagTransformationTask task = new TagTransformationTask(this, info, restTag);
		TransformationPool.getPool().invoke(task);
		return restTag;
	}

	public void setTagFamilyRoot(TagFamily root) {
		linkOut(root, HAS_TAGFAMILY_ROOT);
	}

	public TagFamily getTagFamilyRoot() {
		return out(HAS_TAGFAMILY_ROOT).has(TagFamily.class).nextOrDefaultExplicit(TagFamily.class, null);
	}

	public void delete() {
		e().removeAll();
		getVertex().remove();
	}

	public Page<MeshNode> findTaggedNodes(MeshAuthUser requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo) {
		//findTaggedNodes(userUuid, projectName, tag, languageTags, pagingInfo);
		return null;
	}

	public Page<MeshNode> findTaggedNodes(MeshAuthUser requestUser, String projectName, Tag tag, List<String> languageTags, PagingInfo pagingInfo) {
		//		String langFilter = getLanguageFilter("l");
		//		if (languageTags == null || languageTags.isEmpty()) {
		//			langFilter = "";
		//		} else {
		//			langFilter += " AND ";
		//		}
		//		String baseQuery = PERMISSION_PATTERN_ON_NODE;
		//		baseQuery += "MATCH (node)-[:ASSIGNED_TO_PROJECT]->(pr:Project) ";
		//		baseQuery += "MATCH (tag:Tag)-[:HAS_TAG]->(node)-[l:HAS_I18N_PROPERTIES]-(sp:I18NProperties) ";
		//		baseQuery += "WHERE " + langFilter + " AND " + USER_PERMISSION_FILTER + " AND " + PROJECT_FILTER;
		//
		//		String query = baseQuery + " WITH sp, node " + ORDER_BY_NAME + " RETURN DISTINCT node as n";
		//		String countQuery = baseQuery + " RETURN count(DISTINCT node) as count";
		//
		//		Map<String, Object> parameters = new HashMap<>();
		//		parameters.put("languageTags", languageTags);
		//		parameters.put("projectName", projectName);
		//		parameters.put("userUuid", userUuid);
		//		parameters.put("tag", tag);
		//		return queryService.query(query, countQuery, parameters, pagingInfo, MeshNode.class);
		return null;
	}

}
