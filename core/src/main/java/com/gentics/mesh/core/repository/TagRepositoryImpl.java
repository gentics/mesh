package com.gentics.mesh.core.repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import com.gentics.mesh.core.data.model.Content;
import com.gentics.mesh.core.data.model.Tag;
import com.gentics.mesh.core.repository.action.TagActions;
import com.gentics.mesh.neo4j.QueryService;
import com.gentics.mesh.paging.PagingInfo;

public class TagRepositoryImpl implements TagActions {

	static String PERMISSION_PATTERN_ON_TAG = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(tag:Tag)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) ";
	static String PERMISSION_PATTERN_ON_CONTENT = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(content:Content)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) ";
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
	public Page<Tag> findTaggingTags(String userUuid, String projectName, Tag tag, List<String> languageTags, PagingInfo pagingInfo) {
		String langFilter = getLanguageFilter("l");
		if (languageTags == null || languageTags.isEmpty()) {
			langFilter = "";
		} else {
			langFilter += " AND ";
		}

		String baseQuery = PERMISSION_PATTERN_ON_TAG;
		baseQuery += TAG_PROJECT_PATTERN;
		baseQuery += "MATCH (rootTag:Tag)-[:HAS_TAG]->(tag)-[l:HAS_I18N_PROPERTIES]-(sp:I18NProperties) ";
		baseQuery += "WHERE " + langFilter + " AND " + USER_PERMISSION_FILTER + " AND " + PROJECT_FILTER;

		String query = baseQuery + " WITH sp, tag ORDER BY sp.`properties-name` desc RETURN DISTINCT tag as n";
		String countQuery = baseQuery + " RETURN count(DISTINCT tag) as count";

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("languageTags", languageTags);
		parameters.put("projectName", projectName);
		parameters.put("userUuid", userUuid);
		parameters.put("rootTag", tag);
		return queryService.query(query, countQuery, parameters, pagingInfo, Tag.class);
	}

	@Override
	public Page<Tag> findTaggedTags(String userUuid, String projectName, Tag tag, List<String> languageTags, PagingInfo pagingInfo) {
		String langFilter = getLanguageFilter("la");
		if (languageTags == null || languageTags.isEmpty()) {
			langFilter = "";
		} else {
			langFilter += " AND ";
		}
		String baseQuery = PERMISSION_PATTERN_ON_TAG;
		baseQuery += TAG_PROJECT_PATTERN;
		baseQuery += "MATCH (rootTag:Tag)<-[:HAS_TAG]-(tag)-[la:HAS_I18N_PROPERTIES]-(sp:I18NProperties) ";
		baseQuery += "WHERE " + langFilter + " AND " + USER_PERMISSION_FILTER + " AND " + PROJECT_FILTER;

		String query = baseQuery + " WITH sp, tag ORDER BY " + ORDER_BY_NAME + " RETURN DISTINCT tag as n";
		String countQuery = baseQuery + " RETURN count(DISTINCT tag) as count";

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("languageTags", languageTags);
		parameters.put("projectName", projectName);
		parameters.put("userUuid", userUuid);
		parameters.put("rootTag", tag);
		return queryService.query(query, countQuery, parameters, pagingInfo, Tag.class);
	}

	@Override
	public Page<Tag> findChildTags(String userUuid, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pagingInfo) {
		String langFilter = getLanguageFilter("la");
		if (languageTags == null || languageTags.isEmpty()) {
			langFilter = "";
		} else {
			langFilter += " AND ";
		}
		String baseQuery = PERMISSION_PATTERN_ON_TAG;
		baseQuery += TAG_PROJECT_PATTERN;
		baseQuery += "MATCH (rootTag:Tag)<-[:HAS_PARENT_TAG]-(tag)-[la:HAS_I18N_PROPERTIES]-(sp:I18NProperties) ";
		baseQuery += "WHERE " + langFilter + " AND " + USER_PERMISSION_FILTER + " AND " + PROJECT_FILTER + " AND " + ROOT_TAG_FILTER;

		String query = baseQuery + " WITH sp, tag " + ORDER_BY_NAME + " RETURN DISTINCT tag as n";
		String countQuery = baseQuery + " RETURN count(DISTINCT tag) as count";

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("languageTags", languageTags);
		parameters.put("projectName", projectName);
		parameters.put("userUuid", userUuid);
		parameters.put("rootTagId", rootTag.getId());
		return queryService.query(query, countQuery, parameters, pagingInfo, Tag.class);
	}

	@Override
	public Page<Content> findChildContents(String userUuid, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pagingInfo) {
		String langFilter = getLanguageFilter("l");
		if (languageTags == null || languageTags.isEmpty()) {
			langFilter = "";
		} else {
			langFilter += " AND ";
		}
		String baseQuery = PERMISSION_PATTERN_ON_CONTENT;
		baseQuery += "MATCH (content)-[:ASSIGNED_TO_PROJECT]->(pr:Project) ";
		baseQuery += "MATCH (rootTag:Tag)<-[:HAS_PARENT_TAG]-(content) ";
		baseQuery += "WHERE " + langFilter + USER_PERMISSION_FILTER + " AND " + PROJECT_FILTER + " AND " + ROOT_TAG_FILTER;

		String query = baseQuery + " WITH p, content " + ORDER_BY_NAME + " RETURN DISTINCT content as n";
		String countQuery = baseQuery + " RETURN count(DISTINCT content) as count";

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("languageTags", languageTags);
		parameters.put("projectName", projectName);
		parameters.put("userUuid", userUuid);
		parameters.put("rootTagId", rootTag.getId());
		return queryService.query(query, countQuery, parameters, pagingInfo, Content.class);
	}

	@Override
	public Page<Content> findTaggedContents(String userUuid, String projectName, Tag tag, List<String> languageTags, PagingInfo pagingInfo) {
		String langFilter = getLanguageFilter("l");
		if (languageTags == null || languageTags.isEmpty()) {
			langFilter = "";
		} else {
			langFilter += " AND ";
		}
		String baseQuery = PERMISSION_PATTERN_ON_CONTENT;
		baseQuery += "MATCH (content)-[:ASSIGNED_TO_PROJECT]->(pr:Project) ";
		baseQuery += "MATCH (rootTag:Tag)-[:HAS_PARENT_TAG]->(content)-[l:HAS_I18N_PROPERTIES]-(sp:I18NProperties) ";
		baseQuery += "WHERE " + langFilter + " AND " + USER_PERMISSION_FILTER + " AND " + PROJECT_FILTER;

		String query = baseQuery + " WITH sp, content " + ORDER_BY_NAME + " RETURN DISTINCT content as n";
		String countQuery = baseQuery + " RETURN count(DISTINCT content) as count";

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("languageTags", languageTags);
		parameters.put("projectName", projectName);
		parameters.put("userUuid", userUuid);
		parameters.put("rootTag", tag);
		return queryService.query(query, countQuery, parameters, pagingInfo, Content.class);
	}

}
