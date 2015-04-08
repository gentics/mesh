package com.gentics.cailun.core.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.repository.generic.GenericContentRepository;

public interface ContentRepository extends GenericContentRepository<Content> {

	@Query(
			value="MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(content:Content)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
					+ "MATCH (content)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
					+ "WHERE l.languageTag IN {2} AND id(requestUser) = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
					+ "WITH p, content "
					+ "ORDER by p.`properties-name` desc "
					+ "RETURN DISTINCT content",
			countQuery="MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(content:Content)-[l:HAS_I18N_PROPERTIES]-(p:I18NProperties) "
					+ "MATCH (content)-[:ASSIGNED_TO_PROJECT]->(pr:Project) "
					+ "WHERE l.languageTag IN {2} AND id(requestUser) = {0} AND perm.`permissions-read` = true AND pr.name = {1} "
					+ "RETURN count(DISTINCT content)"
			
		)
	Page<Content> findAll(User requestUser, String projectName, List<String> languageTags,Pageable pageable);
}
