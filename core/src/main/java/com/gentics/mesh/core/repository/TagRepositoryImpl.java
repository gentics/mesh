package com.gentics.mesh.core.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import com.gentics.mesh.core.data.model.MeshNode;
import com.gentics.mesh.core.data.model.Tag;
import com.gentics.mesh.core.repository.action.TagActions;
import com.gentics.mesh.neo4j.QueryService;
import com.gentics.mesh.paging.PagingInfo;

public class TagRepositoryImpl implements TagActions {

	static String PERMISSION_PATTERN_ON_TAG = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(tag:Tag)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) ";
	static String PERMISSION_PATTERN_ON_NODE = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(node:MeshNode)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) ";
	static String TAG_PROJECT_PATTERN = "MATCH (tag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) ";
	static String USER_PERMISSION_FILTER = " requestUser.uuid = {userUuid} AND perm.`permissions-read` = true ";
	static String PROJECT_FILTER = "pr.name = {projectName} ";
	static String ROOT_TAG_FILTER = "id(rootTag) = {rootTagId} ";
	static String ORDER_BY_NAME = "ORDER BY p.`properties-name` desc";

	@Autowired
	Neo4jTemplate neo4jTemplate;

	@Autowired
	private QueryService queryService;

	public static String getLanguageFilter(String field) {
		String filter = " " + field + ".languageTag IN {languageTags} ";
		return filter;
	}

	public Page<Tag> findProjectTags(String userUuid, String projectName, List<String> languageTags, PagingInfo pagingInfo) {

		String langFilter = getLanguageFilter("l");
		if (languageTags == null || languageTags.isEmpty()) {
			langFilter = "";
		} else {
			langFilter += " AND ";
		}
		String baseQuery = PERMISSION_PATTERN_ON_TAG;
		baseQuery += TAG_PROJECT_PATTERN;
		baseQuery += "WHERE " + langFilter + USER_PERMISSION_FILTER + "AND " + PROJECT_FILTER;

		String query = baseQuery + " WITH p, tag " + ORDER_BY_NAME + " RETURN DISTINCT tag as n";
		String countQuery = baseQuery + " RETURN count(DISTINCT tag) as count";

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("languageTags", languageTags);
		parameters.put("projectName", projectName);
		parameters.put("userUuid", userUuid);
		return queryService.query(query, countQuery, parameters, pagingInfo, Tag.class);
	}

	@Override
	public Page<Tag> findTags(String userUuid, String projectName, MeshNode node, List<String> languageTags, PagingInfo pagingInfo) {
		String langFilter = getLanguageFilter("l");
		if (languageTags == null || languageTags.isEmpty()) {
			langFilter = "";
		} else {
			langFilter += " AND ";
		}

		String baseQuery = PERMISSION_PATTERN_ON_TAG;
		baseQuery += TAG_PROJECT_PATTERN;
		baseQuery += "MATCH (node:MeshNode)-[:HAS_TAG]->(tag)-[l:HAS_I18N_PROPERTIES]-(sp:I18NProperties) ";
		baseQuery += "WHERE " + langFilter + USER_PERMISSION_FILTER + " AND " + PROJECT_FILTER;

		String query = baseQuery + " WITH sp, tag ORDER BY sp.`properties-name` desc RETURN DISTINCT tag as n";
		String countQuery = baseQuery + " RETURN count(DISTINCT tag) as count";

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("languageTags", languageTags);
		parameters.put("projectName", projectName);
		parameters.put("userUuid", userUuid);
		parameters.put("node", node);
		return queryService.query(query, countQuery, parameters, pagingInfo, Tag.class);
	}

	@Override
	public Page<MeshNode> findTaggedNodes(String userUuid, String projectName, Tag tag, List<String> languageTags, PagingInfo pagingInfo) {
		String langFilter = getLanguageFilter("l");
		if (languageTags == null || languageTags.isEmpty()) {
			langFilter = "";
		} else {
			langFilter += " AND ";
		}
		String baseQuery = PERMISSION_PATTERN_ON_NODE;
		baseQuery += "MATCH (node)-[:ASSIGNED_TO_PROJECT]->(pr:Project) ";
		baseQuery += "MATCH (tag:Tag)-[:HAS_TAG]->(node)-[l:HAS_I18N_PROPERTIES]-(sp:I18NProperties) ";
		baseQuery += "WHERE " + langFilter + " AND " + USER_PERMISSION_FILTER + " AND " + PROJECT_FILTER;

		String query = baseQuery + " WITH sp, node " + ORDER_BY_NAME + " RETURN DISTINCT node as n";
		String countQuery = baseQuery + " RETURN count(DISTINCT node) as count";

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("languageTags", languageTags);
		parameters.put("projectName", projectName);
		parameters.put("userUuid", userUuid);
		parameters.put("tag", tag);
		return queryService.query(query, countQuery, parameters, pagingInfo, MeshNode.class);
	}

}
