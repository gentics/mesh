package com.gentics.mesh.core.data.service;

import org.springframework.data.domain.Page;
import org.springframework.data.neo4j.conversion.Result;

import com.gentics.mesh.core.data.model.ObjectSchema;
import com.gentics.mesh.core.data.model.auth.User;
import com.gentics.mesh.core.data.service.generic.GenericNodeService;
import com.gentics.mesh.core.rest.schema.response.ObjectSchemaResponse;
import com.gentics.mesh.paging.PagingInfo;

public interface ObjectSchemaService extends GenericNodeService<ObjectSchema> {

	ObjectSchemaResponse transformToRest(ObjectSchema schema);

	void deleteByUUID(String uuid);

	public Result<ObjectSchema> findAll();

	Page<ObjectSchema> findAllVisible(User requestUser, PagingInfo pagingInfo);

	ObjectSchema findByName(String name);

}
