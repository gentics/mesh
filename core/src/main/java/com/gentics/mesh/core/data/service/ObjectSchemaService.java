package com.gentics.mesh.core.data.service;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.Result;
import com.gentics.mesh.core.data.model.root.ObjectSchemaRoot;
import com.gentics.mesh.core.data.model.schema.propertytypes.BasicPropertyTypeSchema;
import com.gentics.mesh.core.data.model.schema.propertytypes.MicroPropertyTypeSchema;
import com.gentics.mesh.core.data.model.schema.propertytypes.PropertyType;
import com.gentics.mesh.core.data.model.tinkerpop.ObjectSchema;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.gentics.mesh.core.data.service.generic.GenericNodeService;
import com.gentics.mesh.core.rest.schema.response.ObjectSchemaResponse;
import com.gentics.mesh.paging.PagingInfo;

public interface ObjectSchemaService extends GenericNodeService<ObjectSchema> {

	ObjectSchemaResponse transformToRest(ObjectSchema schema);

	void deleteByUUID(String uuid);

	public Result<ObjectSchema> findAll();

	Page<ObjectSchema> findAllVisible(User requestUser, PagingInfo pagingInfo);

	ObjectSchema findByName(String name);

	ObjectSchema create(String name);

	ObjectSchemaRoot createRoot();

	ObjectSchemaRoot findRoot();

	BasicPropertyTypeSchema create(String nameKeyword, PropertyType i18nString);

	MicroPropertyTypeSchema createMicroPropertyTypeSchema(String key);

	BasicPropertyTypeSchema createBasicPropertyTypeSchema(String key, PropertyType type);

	BasicPropertyTypeSchema createListPropertyTypeSchema(String key);

}
