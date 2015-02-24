package com.gentics.cailun.core.repository;

import org.springframework.data.neo4j.annotation.Query;

import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.repository.generic.GenericNodeRepository;

public interface ObjectSchemaRepository extends GenericNodeRepository<ObjectSchema> {

	//@Query("MATCH (project:Project)-[:ASSIGNED_TO_PROJECT]-(n:ObjectSchema) WHERE n.name = {1} AND project.name = {0} RETURN n")
	//TODO fix query - somehow the project relationship is not matching
	@Query("MATCH (n:ObjectSchema) WHERE n.name = {1} RETURN n")
	ObjectSchema findByName(String projectName, String name);

}
