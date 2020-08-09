package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

public interface MicroschemaDaoWrapper extends MicroschemaDao {

	MicroschemaContainer loadObjectByUuid(InternalActionContext ac, String schemaUuid, GraphPermission perm);

	MicroschemaContainer findByUuid(String uuid);

	boolean update(MicroschemaContainer microschema, InternalActionContext ac, EventQueueBatch batch);

	MicroschemaContainer create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	TransformablePage<? extends MicroschemaContainer> findAll(InternalActionContext ac, PagingParameters pagingInfo);

	MicroschemaContainer loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound);

	MicroschemaContainer findByName(String name);

	TraversalResult<? extends MicroschemaContainer> findAll();

}
