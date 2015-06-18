package com.gentics.mesh.core.data.model.tinkerpop;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_PARENT_NODE;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_TAG;

import java.util.List;

import org.springframework.beans.factory.annotation.Configurable;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.generic.GenericPropertyContainer;
import com.gentics.mesh.core.rest.node.response.NodeResponse;
import com.gentics.mesh.paging.PagingInfo;


public class MeshNode extends GenericPropertyContainer {

	public List<? extends Tag> getTags() {
		return out(HAS_TAG).toList(Tag.class);
	}

	public void addTag(Tag tag) {
		//addFramedEdge(HAS_TAG, tag);
		linkOut(tag, HAS_TAG);
	}

	public void removeTag(Tag tag) {
		unlinkOut(tag, HAS_TAG);
	}

	public List<? extends MeshNode> getChildren() {
		return in(HAS_PARENT_NODE).toList(MeshNode.class);
	}

	public MeshNode getParentNode() {
		return out(HAS_PARENT_NODE).nextOrDefault(MeshNode.class, null);
	}

	public void setParentNode(MeshNode parent) {
		setLinkOut(parent, HAS_PARENT_NODE);
	}

	public NodeResponse transformToRest(MeshShiroUser requestUser) {

		// TransformationInfo info = new TransformationInfo(rc);
		//
		// List<String> languageTags = rcs.getSelectedLanguageTags(rc);
		// info.setLanguageTags(languageTags);
		// info.setUserService(userService);
		// info.setLanguageService(languageService);
		// info.setTagService(tagService);
		// info.setSpringConfiguration(springConfiguration);
		// info.setContentService(this);
		// info.setI18nService(i18n);
		// NodeResponse restContent = new NodeResponse();
		// MeshNodeTransformationTask task = new MeshNodeTransformationTask(content, info, restContent);
		// pool.invoke(task);
		// return restContent;
		return null;

	}

	public Page<Tag> getTags(MeshShiroUser requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo) {
		return null;
		// public Page<Tag> findTags(String userUuid, String projectName, MeshNode node, List<String> languageTags, PagingInfo pagingInfo) {
		// // String langFilter = getLanguageFilter("l");
		// // if (languageTags == null || languageTags.isEmpty()) {
		// // langFilter = "";
		// // } else {
		// // langFilter += " AND ";
		// // }
		// //
		// // String baseQuery = PERMISSION_PATTERN_ON_TAG;
		// // baseQuery += TAG_PROJECT_PATTERN;
		// // baseQuery += "MATCH (node:MeshNode)-[:HAS_TAG]->(tag)-[l:HAS_I18N_PROPERTIES]-(sp:I18NProperties) ";
		// // baseQuery += "WHERE " + langFilter + USER_PERMISSION_FILTER + " AND " + PROJECT_FILTER;
		// //
		// // String query = baseQuery + " WITH sp, tag ORDER BY sp.`properties-name` desc RETURN DISTINCT tag as n";
		// // String countQuery = baseQuery + " RETURN count(DISTINCT tag) as count";
		// //
		// // Map<String, Object> parameters = new HashMap<>();
		// // parameters.put("languageTags", languageTags);
		// // parameters.put("projectName", projectName);
		// // parameters.put("userUuid", userUuid);
		// // parameters.put("node", node);
		// // return queryService.query(query, countQuery, parameters, pagingInfo, Tag.class);
		// return null;
		// }
	}

	public Page<MeshNode> getChildren(MeshShiroUser requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo) {

		// if (languageTags == null || languageTags.size() == 0) {
		// return findChildren(userUuid, projectName, parentNode, pr);
		// } else {
		// return findChildren(userUuid, projectName, parentNode, languageTags, pr);
		// }

		// Page<MeshNode> findChildren(String userUuid, String projectName, MeshNode parentNode, List<String> languageTags, Pageable pr) {
		// @Query(value = MATCH_PERMISSION_ON_NODE + MATCH_NODE_OF_PROJECT + " MATCH (parentNode)-[:HAS_PARENT_NODE]->(node) " + "WHERE "
		// + FILTER_USER_PERM_AND_PROJECT + " AND id(parentNode) = {2} " + "WITH p, node " + ORDER_BY_NAME_DESC + "RETURN DISTINCT childNode",
		//
		// countQuery = MATCH_PERMISSION_ON_NODE + MATCH_NODE_OF_PROJECT + " MATCH (parentNode)-[:HAS_PARENT_NODE]->(node) " + "WHERE "
		// + FILTER_USER_PERM_AND_PROJECT + " AND id(parentNode) = {2} " + "RETURN count(DISTINCT node)"
		//
		// )
		// }

		// Page<MeshNode> findChildren(String userUuid, String projectName, MeshNode parentNode, Pageable pr) {
		// @Query(value = MATCH_PERMISSION_ON_NODE + MATCH_NODE_OF_PROJECT + " MATCH (parentNode)<-[:HAS_PARENT_NODE]-(node) " + "WHERE "
		// + FILTER_USER_PERM_AND_PROJECT + " AND id(parentNode) = {2} " + "WITH p, node " + "ORDER by p.`properties-name` desc "
		// + "RETURN DISTINCT node",
		//
		// countQuery = MATCH_PERMISSION_ON_NODE + MATCH_NODE_OF_PROJECT + " MATCH (parentNode)<-[:HAS_PARENT_NODE]-(node) " + "WHERE "
		// + FILTER_USER_PERM_AND_PROJECT + " AND id(parentNode) = {2} " + "RETURN count(DISTINCT node)")
		// }
		return null;

	}

}
