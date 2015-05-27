package com.gentics.mesh.core.repository;

import static com.gentics.mesh.core.repository.CypherStatements.FILTER_USER_PERM_AND_PROJECT;
import static com.gentics.mesh.core.repository.CypherStatements.MATCH_NODE_OF_PROJECT;
import static com.gentics.mesh.core.repository.CypherStatements.MATCH_PERMISSION_ON_NODE;
import static com.gentics.mesh.core.repository.CypherStatements.ORDER_BY_NAME_DESC;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;

import com.gentics.mesh.core.data.model.MeshNode;
import com.gentics.mesh.core.repository.generic.GenericPropertyContainerRepository;

public interface MeshNodeRepository extends GenericPropertyContainerRepository<MeshNode> {

	@Query(value = MATCH_PERMISSION_ON_NODE + MATCH_NODE_OF_PROJECT + "WHERE l.languageTag IN {2} AND " + FILTER_USER_PERM_AND_PROJECT + "WITH p, node "
			+ ORDER_BY_NAME_DESC + "RETURN DISTINCT node",

	countQuery = MATCH_PERMISSION_ON_NODE + MATCH_NODE_OF_PROJECT + "WHERE l.languageTag IN {2} AND " + FILTER_USER_PERM_AND_PROJECT
			+ "RETURN count(DISTINCT node)"

	)
	Page<MeshNode> findAll(String userUuid, String projectName, List<String> languageTags, Pageable pageable);

	@Query(value = MATCH_PERMISSION_ON_NODE + MATCH_NODE_OF_PROJECT + "WHERE " + FILTER_USER_PERM_AND_PROJECT + "WITH p, node " + ORDER_BY_NAME_DESC
			+ "RETURN DISTINCT node",

	countQuery = MATCH_PERMISSION_ON_NODE + MATCH_NODE_OF_PROJECT + "WHERE " + FILTER_USER_PERM_AND_PROJECT + "RETURN count(DISTINCT node)")
	Page<MeshNode> findAll(String userUuid, String projectName, Pageable pageable);

	@Query("MATCH (node:MeshNode) RETURN node")
	public List<MeshNode> findAllNodes();

	// node children

	@Query(value = MATCH_PERMISSION_ON_NODE + MATCH_NODE_OF_PROJECT + " MATCH (parentNode)-[:HAS_PARENT_NODE]->(node) " + "WHERE "
			+ FILTER_USER_PERM_AND_PROJECT + " AND id(parentNode) = {2} " + "WITH p, node " + ORDER_BY_NAME_DESC + "RETURN DISTINCT childNode",

	countQuery = MATCH_PERMISSION_ON_NODE + MATCH_NODE_OF_PROJECT + "WHERE " + FILTER_USER_PERM_AND_PROJECT + "RETURN count(DISTINCT node)"

	)
	Page<MeshNode> findChildren(String userUuid, String projectName, MeshNode parentNode, List<String> languageTags, Pageable pr);

	@Query(value = MATCH_PERMISSION_ON_NODE + MATCH_NODE_OF_PROJECT + "WHERE " + FILTER_USER_PERM_AND_PROJECT + "WITH p, node "
			+ "ORDER by p.`properties-name` desc " + "RETURN DISTINCT node",

	countQuery = MATCH_PERMISSION_ON_NODE + MATCH_NODE_OF_PROJECT + "WHERE " + FILTER_USER_PERM_AND_PROJECT + "RETURN count(DISTINCT node)")
	Page<MeshNode> findChildren(String userUuid, String projectName, MeshNode parentNode, Pageable pr);
}
