package com.gentics.cailun.core.data.service;

import org.springframework.data.domain.Page;
import org.springframework.data.neo4j.conversion.Result;

import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.generic.GenericNodeService;
import com.gentics.cailun.core.rest.schema.response.ObjectSchemaResponse;
import com.gentics.cailun.path.PagingInfo;

public interface ObjectSchemaService extends GenericNodeService<ObjectSchema> {

	ObjectSchemaResponse transformToRest(ObjectSchema schema);

	void deleteByUUID(String uuid);

	public Result<ObjectSchema> findAll();

	Page<ObjectSchema> findAllVisible(User requestUser, PagingInfo pagingInfo);

	ObjectSchema findByName(String name);

}
