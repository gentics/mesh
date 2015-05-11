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
	static String USER_PERMISSION_FILTER = " requestUser.uuid = {userUuid} AND perm.`permissions-read` = true";
	static String PROJECT_FILTER = "pr.name = {projectName}";
	static String LANGUAGE_TAG_FILTER = " l.languageTag IN {languageTags} ";

	@Autowired
	Neo4jTemplate neo4jTemplate;

	@Autowired
	private QueryService queryService;

	public Page<Tag> findProjectTags(String userUuid, String projectName, List<String> languageTags, PagingInfo pagingInfo) {

		String extraFilter = LANGUAGE_TAG_FILTER;
		if (languageTags == null || languageTags.isEmpty()) {
			extraFilter = "";
		}
		String baseQuery = PERMISSION_PATTERN_ON_TAG;
		baseQuery += "MATCH (tag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) ";
		baseQuery += "WHERE " + extraFilter + " AND " + USER_PERMISSION_FILTER + "AND " + PROJECT_FILTER;

		String query = baseQuery + " WITH p, tag ORDER BY p.`properties-name` desc RETURN DISTINCT tag";
		String countQuery = baseQuery + " RETURN count(DISTINCT tag) as count";

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("languageTags", languageTags);
		parameters.put("projectName", projectName);
		parameters.put("userUuid", userUuid);
		return queryService.query(query, countQuery, parameters, pagingInfo, Tag.class);
	}

	@Override
	public Page<Tag> findTaggingTags(String userUuid, String projectName, Tag tag, List<String> languageTags, PagingInfo pagingInfo) {
		String langFilter = LANGUAGE_TAG_FILTER;
		if (languageTags == null || languageTags.isEmpty()) {
			langFilter = "";
		}

		String baseQuery = PERMISSION_PATTERN_ON_TAG;
		baseQuery += "MATCH (tag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) ";
		baseQuery += "MATCH (rootTag:Tag)-[:HAS_TAG]->(tag)-[l:HAS_I18N_PROPERTIES]-(sp:I18NProperties) ";
		baseQuery += "WHERE " + langFilter + " AND " + USER_PERMISSION_FILTER + " AND " + PROJECT_FILTER;

		String query = baseQuery + " WITH sp, tag ORDER BY sp.`properties-name` desc RETURN DISTINCT tag";
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
		String langFilter = LANGUAGE_TAG_FILTER;
		if (languageTags == null || languageTags.isEmpty()) {
			langFilter = "";
		}
		String baseQuery = PERMISSION_PATTERN_ON_TAG;
		baseQuery += "MATCH (tag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) ";
		baseQuery += "MATCH (rootTag:Tag)<-[:HAS_TAG]-(tag)-[l:HAS_I18N_PROPERTIES]-(sp:I18NProperties) ";
		baseQuery += "WHERE " + langFilter + " AND " + USER_PERMISSION_FILTER + " AND " + PROJECT_FILTER;

		String query = baseQuery + " WITH sp, tag ORDER BY sp.`properties-name` desc RETURN DISTINCT tag";
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
		String langFilter = LANGUAGE_TAG_FILTER;
		if (languageTags == null || languageTags.isEmpty()) {
			langFilter = "";
		}
		String baseQuery = PERMISSION_PATTERN_ON_TAG;
		baseQuery += "MATCH (tag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) ";
		baseQuery += "MATCH (rootTag:Tag)-[:HAS_PARENT_TAG]->(tag)-[l:HAS_I18N_PROPERTIES]-(sp:I18NProperties) ";
		baseQuery += "WHERE " + langFilter + " AND " + USER_PERMISSION_FILTER + " AND " + PROJECT_FILTER;

		String query = baseQuery + " WITH sp, tag ORDER BY sp.`properties-name` desc RETURN DISTINCT tag";
		String countQuery = baseQuery + " RETURN count(DISTINCT tag) as count";

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("languageTags", languageTags);
		parameters.put("projectName", projectName);
		parameters.put("userUuid", userUuid);
		parameters.put("rootTag", rootTag);
		return queryService.query(query, countQuery, parameters, pagingInfo, Tag.class);
	}

	@Override
	public Page<Content> findChildContents(String userUuid, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pagingInfo) {
		String langFilter = LANGUAGE_TAG_FILTER;
		if (languageTags == null || languageTags.isEmpty()) {
			langFilter = "";
		}
		String baseQuery = PERMISSION_PATTERN_ON_CONTENT;
		baseQuery += "MATCH (content)-[:ASSIGNED_TO_PROJECT]->(pr:Project) ";
		baseQuery += "MATCH (rootTag:Tag)-[:HAS_PARENT_TAG]->(content)-[l:HAS_I18N_PROPERTIES]-(sp:I18NProperties) ";
		baseQuery += "WHERE " + langFilter + " AND " + USER_PERMISSION_FILTER + " AND " + PROJECT_FILTER;

		String query = baseQuery + " WITH sp, content ORDER BY sp.`properties-name` desc RETURN DISTINCT content";
		String countQuery = baseQuery + " RETURN count(DISTINCT content) as count";

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("languageTags", languageTags);
		parameters.put("projectName", projectName);
		parameters.put("userUuid", userUuid);
		parameters.put("rootTag", rootTag);
		return queryService.query(query, countQuery, parameters, pagingInfo, Content.class);
	}

	@Override
	public Page<Content> findTaggedContents(String userUuid, String projectName, Tag tag, List<String> languageTags, PagingInfo pagingInfo) {
		String langFilter = LANGUAGE_TAG_FILTER;
		if (languageTags == null || languageTags.isEmpty()) {
			langFilter = "";
		}
		String baseQuery = PERMISSION_PATTERN_ON_CONTENT;
		baseQuery += "MATCH (content)-[:ASSIGNED_TO_PROJECT]->(pr:Project) ";
		baseQuery += "MATCH (rootTag:Tag)-[:HAS_PARENT_TAG]->(content)-[l:HAS_I18N_PROPERTIES]-(sp:I18NProperties) ";
		baseQuery += "WHERE " + langFilter + " AND " + USER_PERMISSION_FILTER + " AND " + PROJECT_FILTER;

		String query = baseQuery + " WITH sp, content ORDER BY sp.`properties-name` desc RETURN DISTINCT content";
		String countQuery = baseQuery + " RETURN count(DISTINCT content) as count";

		Map<String, Object> parameters = new HashMap<>();
		parameters.put("languageTags", languageTags);
		parameters.put("projectName", projectName);
		parameters.put("userUuid", userUuid);
		parameters.put("rootTag", tag);
		return queryService.query(query, countQuery, parameters, pagingInfo, Content.class);
	}

}
