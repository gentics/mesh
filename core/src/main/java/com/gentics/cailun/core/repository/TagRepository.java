package com.gentics.cailun.core.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;

import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.repository.generic.GenericTagRepository;

public interface TagRepository extends GenericTagRepository<Tag, GenericFile> {

	//TODO filter by name?
	//@Query(value="MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(tag:Tag) where id(requestUser) = {0} and perm.`permissions-read` = true return tag",countQuery="MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(tag:Tag) where id(requestUser) = {0} and perm.`permissions-read` = true return count(tag)")
	@Query(
	value =   "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(tag:Tag)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
			+ "MATCH (tag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
			+ "WHERE l.languageTag IN {2} AND id(requestUser) = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
			+ "WITH p, tag "
			+ "ORDER BY p.`properties-name` desc "
			+ "RETURN DISTINCT tag",
	countQuery="MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(tag:Tag)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
					+"MATCH (tag)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
					+"WHERE l.languageTag IN {2} AND id(requestUser) = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
					+"RETURN count(DISTINCT tag)"
	)
	public Page<Tag> findAll(User requestUser, String projectName, List<String> languageTags, Pageable pageable);

}
