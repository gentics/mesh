package com.gentics.mesh.core.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import com.gentics.mesh.core.data.model.Content;
import com.gentics.mesh.core.data.model.Tag;
import com.gentics.mesh.core.repository.action.TagActions;
import com.gentics.mesh.paging.MeshPageRequest;
import com.gentics.mesh.paging.PagingInfo;

public class TagRepositoryImpl implements TagActions {

	static String PERMISSION_PATTERN = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(tag:Tag)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) ";
	static String USER_PERMISSION_PROJECT_FILTER = " requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} ";

	@Autowired
	Neo4jTemplate neo4jTemplate;

	public Page<Tag> findProjectTags(String userUuid, String projectName, List<String> languageTags, PagingInfo pageingInfo) {
		String query = "MATCH (n:Tag) return n";
		Result<Map<String, Object>> result = neo4jTemplate.query(query, Collections.emptyMap());
		List<Tag> tags = new ArrayList<>();
		for (Map<String, Object> r : result.slice(pageingInfo.getPage() - 1, pageingInfo.getPage())) {
			Tag tag = (Tag) neo4jTemplate.getDefaultConverter().convert(r.get("n"), Tag.class);
			tags.add(tag);
		}
		//TODO add query for count
		int total = 20;
		return new PageImpl<Tag>(tags, new MeshPageRequest(pageingInfo), total);
	}

	@Override
	public Page<Tag> findTaggingTags(String userUuid, String projectName, List<String> languageTags, PagingInfo pageingInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<Tag> findTaggedTags(String userUuid, String projectName, List<String> languageTags, PagingInfo pageingInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<Tag> findChildTags(String userUuid, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pageingInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<Content> findChildContents(String userUuid, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pageingInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<Content> findTaggedContents(String userUuid, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pageingInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	// // TODO filter by name?
	// @Query(value = PERMISSION_PATTERN + "MATCH (tag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) " + "WHERE l.languageTag IN {2} AND "
	// + USER_PERMISSION_PROJECT_FILTER + "WITH p, tag " + "ORDER BY p.`properties-name` desc " + "RETURN DISTINCT tag",
	//
	// countQuery = PERMISSION_PATTERN + "MATCH (tag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
	// + "WHERE l.languageTag IN {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
	// + "RETURN count(DISTINCT tag)")
	// public Page<Tag> findAll(String userUuid, String projectName, List<String> languageTags, Pageable pageable);
	//
	// @Query(value = PERMISSION_PATTERN + "MATCH (tag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
	// + "WHERE requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} " + "WITH p, tag "
	// + "ORDER BY p.`properties-name` desc " + "RETURN DISTINCT tag",
	//
	// countQuery = PERMISSION_PATTERN + "MATCH (tag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
	// + "WHERE requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} " + "RETURN count(DISTINCT tag)")
	// public Page<Tag> findAll(String userUuid, String projectName, Pageable pageable);
	//
	// @Query(value = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(rootTag:Tag)"
	// + "MATCH (rootTag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
	// + "MATCH (rootTag)-[:HAS_TAG]->(subTag:Tag)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
	// + "WHERE l.languageTag IN {3} AND id(rootTag) = {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
	// + "WITH p, subTag " + "ORDER BY p.`properties-name` desc " + "RETURN DISTINCT subTag",
	//
	// countQuery = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(rooTag:Tag)"
	// + "MATCH (rootTag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
	// + "MATCH (rootTag)-[:HAS_TAG]->(subTag:Tag)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
	// + "WHERE l.languageTag IN {3} AND id(rootTag) = {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
	// + "RETURN count(DISTINCT subTag)")
	// public Page<Tag> findAllTags(String userUuid, String projectName, Tag rootTag, List<String> languageTags, Pageable pr);
	//
	// @Query(value = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(rootTag:Tag)"
	// + "MATCH (rootTag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
	// + "WHERE id(rootTag) = {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
	// + "MATCH (rootTag)-[:HAS_TAG]->(subTag:Tag)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties)  " + "WITH p, subTag "
	// + "ORDER BY p.`properties-name` desc " + "RETURN DISTINCT subTag",
	//
	// countQuery = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(rooTag:Tag)"
	// + "MATCH (rootTag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
	// + "MATCH (rootTag)-[:HAS_TAG]->(subTag:Tag)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties)  "
	// + "WHERE id(rootTag) = {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
	// + "RETURN count(DISTINCT subTag)")
	// public Page<Tag> findAllTags(String userUuid, String projectName, Tag rootTag, Pageable pr);
	//
	// @Query(value = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(rootTag:Tag)"
	// + "MATCH (rootTag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
	// + "WHERE id(rootTag) = {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
	// + "MATCH (rootTag)-[:HAS_CONTENT]->(subContent:Content)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties)  " + "WITH p, subContent "
	// + "ORDER BY p.`properties-name` desc " + "RETURN DISTINCT subContent",
	//
	// countQuery = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(rooTag:Tag)"
	// + "MATCH (rootTag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
	// + "MATCH (rootTag)-[:HAS_CONTENT]->(subContent:Content)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties)  "
	// + "WHERE id(rootTag) = {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
	// + "RETURN count(DISTINCT subContent)")
	// public Page<Content> findAllVisibleContents(String userUuid, String projectName, Tag rootTag, Pageable pr);
	//
	// @Query(value = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(rootTag:Tag)"
	// + "MATCH (rootTag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
	// + "MATCH (rootTag)-[:HAS_CONTENT]->(subContent:Content)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
	// + "WHERE l.languageTag IN {3} AND id(rootTag) = {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
	// + "WITH p, subContent " + "ORDER BY p.`properties-name` desc " + "RETURN DISTINCT subContent",
	//
	// countQuery = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(rooTag:Tag)"
	// + "MATCH (rootTag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
	// + "MATCH (rootTag)-[:HAS_CONTENT]->(subContent:Content)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
	// + "WHERE l.languageTag IN {3} AND id(rootTag) = {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
	// + "RETURN count(DISTINCT subContent)")
	// public Page<Content> findAllVisibleContents(String userUuid, String projectName, Tag rootTag, List<String> languageTags, Pageable pr);
	//
	// @Query(value = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(rootTag:Tag)"
	// + "MATCH (rootTag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
	// + "MATCH (rootTag)<-[:HAS_PARENT_TAG]-(childNode:GenericPropertyContainer)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
	// + "WHERE l.languageTag IN {3} AND id(rootTag) = {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
	// + "WITH p, childNode " + "ORDER BY p.`properties-name` desc " + "RETURN DISTINCT childNode",
	//
	// countQuery = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(rooTag:Tag)"
	// + "MATCH (rootTag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
	// + "MATCH (rootTag)<-[:HAS_PARENT_TAG]-(childNode:GenericPropertyContainer)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
	// + "WHERE l.languageTag IN {3} AND id(rootTag) = {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
	// + "RETURN count(DISTINCT childNode)")
	// public Page<GenericPropertyContainer> findAllVisibleChildNodes(String userUuid, String projectName, Tag rootTag, List<String> languageTags,
	// Pageable pr);
	//
	// @Query(value = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(rootTag:Tag)"
	// + "MATCH (rootTag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
	// + "WHERE id(rootTag) = {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
	// + "MATCH (rootTag)<-[:HAS_PARENT_TAG]-(childNode:GenericPropertyContainer)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) WITH p, childNode "
	// + "ORDER BY p.`properties-name` desc " + "RETURN DISTINCT childNode",
	//
	// countQuery = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(rooTag:Tag)"
	// + "MATCH (rootTag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
	// + "MATCH (rootTag)<-[:HAS_PARENT_TAG]-(childNode:GenericPropertyContainer)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties)  "
	// + "WHERE id(rootTag) = {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
	// + "RETURN count(DISTINCT childNode)")
	// public Page<GenericPropertyContainer> findAllVisibleChildNodes(String userUuid, String projectName, Tag rootTag, Pageable pr);

}
