package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAG;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.root.TagRoot;

public class TagRootImpl extends AbstractRootVertex<Tag> implements TagRoot {

	@Override
	protected Class<? extends Tag> getPersistanceClass() {
		return TagImpl.class;
	}

	@Override
	protected String getRootLabel() {
		return HAS_TAG;
	}

	@Override
	public void addTag(Tag tag) {
		addItem(tag);
	}

	@Override
	public void removeTag(Tag tag) {
		removeTag(tag);
	}

//	@Override
//	public Page<? extends Tag> findProjectTags(MeshAuthUser requestUser, String projectName, PagingInfo pagingInfo)
//			throws InvalidArgumentException {
//
//		VertexTraversal<?, ?, ?> traversal = requestUser.getImpl().getPermTraversal(READ_PERM).has(TagImpl.class).mark().out(ASSIGNED_TO_PROJECT)
//				.has("name", projectName).back();
//		VertexTraversal<?, ?, ?> countTraversal = requestUser.getImpl().getPermTraversal(READ_PERM).has(TagImpl.class).mark()
//				.out(ASSIGNED_TO_PROJECT).has("name", projectName).back();
//		return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, TagImpl.class);
//		// String langFilter = getLanguageFilter("l");
//		// if (languageTags == null || languageTags.isEmpty()) {
//		// langFilter = "";
//		// } else {
//		// langFilter += " AND ";
//		// }
//		// String baseQuery = PERMISSION_PATTERN_ON_TAG;
//		// baseQuery += TAG_PROJECT_PATTERN;
//		// baseQuery += "WHERE " + langFilter + USER_PERMISSION_FILTER + "AND " + PROJECT_FILTER;
//		//
//		// String query = baseQuery + " WITH p, tag " + ORDER_BY_NAME + " RETURN DISTINCT tag as n";
//		// String countQuery = baseQuery + " RETURN count(DISTINCT tag) as count";
//		//
//		// Map<String, Object> parameters = new HashMap<>();
//		// parameters.put("languageTags", languageTags);
//		// parameters.put("projectName", projectName);
//		// parameters.put("userUuid", userUuid);
//		// return queryService.query(query, countQuery, parameters, pagingInfo, Tag.class);
//	}

	// static String PERMISSION_PATTERN_ON_TAG =
	// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(tag:Tag)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) ";
	// static String PERMISSION_PATTERN_ON_NODE =
	// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(node:MeshNode)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) ";
	// static String TAG_PROJECT_PATTERN = "MATCH (tag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) ";
	// static String USER_PERMISSION_FILTER = " requestUser.uuid = {userUuid} AND perm.`permissions-read` = true ";
	// static String PROJECT_FILTER = "pr.name = {projectName} ";
	// static String ROOT_TAG_FILTER = "id(rootTag) = {rootTagId} ";
	// static String ORDER_BY_NAME = "ORDER BY p.`properties-name` desc";

	// public static String getLanguageFilter(String field) {
	// String filter = " " + field + ".languageTag IN {languageTags} ";
	// return filter;
	// }
	
	@Override
	public Tag findByName(String name) {
		return out(getRootLabel()).has(getPersistanceClass()).mark().out(HAS_FIELD_CONTAINER).has("name", name).back().nextOrDefaultExplicit(TagImpl.class, null);
	}

	@Override
	public Tag findByName(String projectName, String name) {
		return out(getRootLabel()).has(getPersistanceClass()).mark().out(HAS_FIELD_CONTAINER).has("name", name).back().mark().out(ASSIGNED_TO_PROJECT).has(ProjectImpl.class)
				.has("name", projectName).back().nextOrDefaultExplicit(TagImpl.class, null);
	}

}
