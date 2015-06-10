package com.gentics.mesh.core.data.service;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.Result;
import com.gentics.mesh.core.data.model.root.SchemaRoot;
import com.gentics.mesh.core.data.model.schema.propertytypes.BasicPropertyType;
import com.gentics.mesh.core.data.model.schema.propertytypes.MicroPropertyType;
import com.gentics.mesh.core.data.model.schema.propertytypes.PropertyType;
import com.gentics.mesh.core.data.model.tinkerpop.Schema;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.gentics.mesh.core.data.service.generic.GenericNodeService;
import com.gentics.mesh.core.rest.schema.response.SchemaResponse;
import com.gentics.mesh.paging.PagingInfo;

public interface SchemaService extends GenericNodeService<Schema> {

	SchemaResponse transformToRest(Schema schema);

	void deleteByUUID(String uuid);

	public Result<Schema> findAll();

	Page<Schema> findAllVisible(User requestUser, PagingInfo pagingInfo);

	Schema findByName(String name);

	Schema create(String name);

	SchemaRoot createRoot();

	SchemaRoot findRoot();

	BasicPropertyType create(String nameKeyword, PropertyType i18nString);

	MicroPropertyType createMicroPropertyTypeSchema(String key);

	BasicPropertyType createBasicPropertyTypeSchema(String key, PropertyType type);

	BasicPropertyType createListPropertyTypeSchema(String key);

}
