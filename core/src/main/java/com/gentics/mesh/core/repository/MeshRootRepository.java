package com.gentics.mesh.core.repository;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.repository.Repository;

import com.gentics.mesh.core.data.model.MeshRoot;

public interface MeshRootRepository extends Repository<MeshRoot, Long> {

	@Query("MATCH (n:MeshRoot) return n")
	MeshRoot findRoot();

	void save(MeshRoot rootNode);

	MeshRoot findOne(Long id);

}
