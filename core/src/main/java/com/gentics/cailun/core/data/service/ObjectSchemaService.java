package com.gentics.cailun.core.data.service;

import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.service.generic.GenericNodeService;
import com.gentics.cailun.core.rest.schema.response.ObjectSchemaResponse;

public interface ObjectSchemaService extends GenericNodeService<ObjectSchema> {

	ObjectSchemaResponse transformToRest(ObjectSchema schema);

	void deleteByName(String projectName, String schemaName);

	void deleteByUUID(String uuid);

}
