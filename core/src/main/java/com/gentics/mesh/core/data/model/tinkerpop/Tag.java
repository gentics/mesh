package com.gentics.mesh.core.data.model.tinkerpop;

import java.util.List;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.generic.GenericPropertyContainer;
import com.gentics.mesh.core.data.model.relationship.MeshRelationships;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.data.service.transformation.TransformationPool;
import com.gentics.mesh.core.data.service.transformation.tag.TagTransformationTask;
import com.gentics.mesh.core.rest.tag.response.TagResponse;
import com.gentics.mesh.paging.PagingInfo;

public class Tag extends GenericPropertyContainer {

	public List<? extends MeshNode> getNodes() {
		return in(MeshRelationships.HAS_TAG).toList(MeshNode.class);
	}

	public void removeNode(MeshNode node) {
		unlinkIn(node, MeshRelationships.HAS_TAG);
	}

	public TagResponse transformToRest(TransformationInfo info) {

		TagResponse restTag = new TagResponse();
		TagTransformationTask task = new TagTransformationTask(this, info, restTag);
		TransformationPool.getPool().invoke(task);
		return restTag;
	}

	public void delete() {
		//TODO handle edges?
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
