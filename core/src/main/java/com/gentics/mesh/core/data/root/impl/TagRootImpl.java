package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;

import java.util.List;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

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
	public Page<? extends Tag> findProjectTags(MeshAuthUser requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo)
			throws InvalidArgumentException {

		VertexTraversal<?, ?, ?> traversal = requestUser.getImpl().getPermTraversal(READ_PERM).has(TagImpl.class).mark().out(ASSIGNED_TO_PROJECT)
				.has("name", projectName).back();
		VertexTraversal<?, ?, ?> countTraversal = requestUser.getImpl().getPermTraversal(READ_PERM).has(TagImpl.class).mark()
				.out(ASSIGNED_TO_PROJECT).has("name", projectName).back();
		return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, TagImpl.class);
		// String langFilter = getLanguageFilter("l");
		// if (languageTags == null || languageTags.isEmpty()) {
		// langFilter = "";
		// } else {
		// langFilter += " AND ";
		// }
		// String baseQuery = PERMISSION_PATTERN_ON_TAG;
		// baseQuery += TAG_PROJECT_PATTERN;
		// baseQuery += "WHERE " + langFilter + USER_PERMISSION_FILTER + "AND " + PROJECT_FILTER;
		//
		// String query = baseQuery + " WITH p, tag " + ORDER_BY_NAME + " RETURN DISTINCT tag as n";
		// String countQuery = baseQuery + " RETURN count(DISTINCT tag) as count";
		//
		// Map<String, Object> parameters = new HashMap<>();
		// parameters.put("languageTags", languageTags);
		// parameters.put("projectName", projectName);
		// parameters.put("userUuid", userUuid);
		// return queryService.query(query, countQuery, parameters, pagingInfo, Tag.class);
	}

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
	public Tag findByName(String projectName, String name) {
		// TODO filter by i18n properties, projectname
		return findByNameAndProject(projectName, name);
	}



}
