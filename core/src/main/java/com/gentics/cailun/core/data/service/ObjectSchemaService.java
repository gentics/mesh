package com.gentics.cailun.core.data.service;

import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.service.generic.GenericNodeService;
import com.gentics.cailun.core.rest.response.RestObjectSchema;

public interface ObjectSchemaService extends GenericNodeService<ObjectSchema> {

	RestObjectSchema getReponseObject(ObjectSchema projectSchema);

	void deleteByName(String projectName, String schemaName);

	void deleteByUUID(String uuid);

}
