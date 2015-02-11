package com.gentics.cailun.core.repository;

import org.springframework.data.neo4j.annotation.Query;

import com.gentics.cailun.core.rest.model.Content;
import com.gentics.cailun.core.rest.model.File;

public interface FileRepository<T extends File> extends CaiLunNodeRepository<T> {
	@Query("MATCH (content:Content) WHERE content.uuid = {0} return content")
	public Content findByUUID(String uuid);

	@Query("MATCH (n:Content {uuid: {0}} DELETE n")
	public void delete(String uuid);
}
