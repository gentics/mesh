package com.gentics.cailun.core.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.generic.GenericPropertyContainer;
import com.gentics.cailun.core.repository.generic.GenericPropertyContainerRepository;

public interface TagRepository extends GenericPropertyContainerRepository<Tag> {

	// TODO filter by name?
	@Query(value = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(tag:Tag)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
			+ "MATCH (tag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
			+ "WHERE l.languageTag IN {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
			+ "WITH p, tag "
			+ "ORDER BY p.`properties-name` desc " + "RETURN DISTINCT tag",

	countQuery = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(tag:Tag)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
			+ "MATCH (tag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
			+ "WHERE l.languageTag IN {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
			+ "RETURN count(DISTINCT tag)")
	public Page<Tag> findAll(String userUuid, String projectName, List<String> languageTags, Pageable pageable);

	@Query(

	value = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(tag:Tag)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
			+ "MATCH (tag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
			+ "WHERE requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
			+ "WITH p, tag "
			+ "ORDER BY p.`properties-name` desc " + "RETURN DISTINCT tag",

	countQuery = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(tag:Tag)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
			+ "MATCH (tag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
			+ "WHERE requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} " + "RETURN count(DISTINCT tag)")
	public Page<Tag> findAll(String userUuid, String projectName, Pageable pageable);

	@Query(

	value = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(rootTag:Tag)"
			+ "MATCH (rootTag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
			+ "MATCH (rootTag)-[:HAS_TAG]->(subTag:Tag)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
			+ "WHERE l.languageTag IN {3} AND id(rootTag) = {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
			+ "WITH p, subTag " + "ORDER BY p.`properties-name` desc " + "RETURN DISTINCT subTag",

	countQuery = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(rooTag:Tag)"
			+ "MATCH (rootTag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
			+ "MATCH (rootTag)-[:HAS_TAG]->(subTag:Tag)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
			+ "WHERE l.languageTag IN {3} AND id(rootTag) = {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
			+ "RETURN count(DISTINCT subTag)")
	public Page<Tag> findAllTags(String userUuid, String projectName, Tag rootTag, List<String> languageTags, Pageable pr);

	@Query(

	value = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(rootTag:Tag)"
			+ "MATCH (rootTag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
			+ "WHERE id(rootTag) = {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
			+ "MATCH (rootTag)-[:HAS_TAG]->(subTag:Tag)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties)  " + "WITH p, subTag "
			+ "ORDER BY p.`properties-name` desc " + "RETURN DISTINCT subTag",

	countQuery = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(rooTag:Tag)"
			+ "MATCH (rootTag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
			+ "MATCH (rootTag)-[:HAS_TAG]->(subTag:Tag)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties)  "
			+ "WHERE id(rootTag) = {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
			+ "RETURN count(DISTINCT subTag)")
	public Page<Tag> findAllTags(String userUuid, String projectName, Tag rootTag, Pageable pr);

	@Query(

	value = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(rootTag:Tag)"
			+ "MATCH (rootTag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
			+ "WHERE id(rootTag) = {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
			+ "MATCH (rootTag)-[:HAS_CONTENT]->(subContent:Content)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties)  " + "WITH p, subContent "
			+ "ORDER BY p.`properties-name` desc " + "RETURN DISTINCT subContent",

	countQuery = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(rooTag:Tag)"
			+ "MATCH (rootTag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
			+ "MATCH (rootTag)-[:HAS_CONTENT]->(subContent:Content)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties)  "
			+ "WHERE id(rootTag) = {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
			+ "RETURN count(DISTINCT subContent)")
	public Page<Content> findAllVisibleContents(String userUuid, String projectName, Tag rootTag, Pageable pr);

	@Query(

	value = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(rootTag:Tag)"
			+ "MATCH (rootTag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
			+ "MATCH (rootTag)-[:HAS_CONTENT]->(subContent:Content)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
			+ "WHERE l.languageTag IN {3} AND id(rootTag) = {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
			+ "WITH p, subContent " + "ORDER BY p.`properties-name` desc " + "RETURN DISTINCT subContent",

	countQuery = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(rooTag:Tag)"
			+ "MATCH (rootTag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
			+ "MATCH (rootTag)-[:HAS_CONTENT]->(subContent:Content)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
			+ "WHERE l.languageTag IN {3} AND id(rootTag) = {2} AND requestUser.uuid = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
			+ "RETURN count(DISTINCT subContent)")
	public Page<Content> findAllVisibleContents(String userUuid, String projectName, Tag rootTag, List<String> languageTags, Pageable pr);

	@Query()
	public Page<? extends GenericPropertyContainer> findAllVisibleChildNodes(String userUuid, String projectName, Tag rootTag,
			List<String> languageTags, Pageable pr);

	@Query()
	public Page<? extends GenericPropertyContainer> findAllVisibleChildNodes(String userUuid, String projectName, Tag rootTag, Pageable pr);

}
