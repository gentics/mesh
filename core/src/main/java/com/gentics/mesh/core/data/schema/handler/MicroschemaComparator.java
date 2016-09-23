package com.gentics.mesh.core.data.schema.handler;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

@Singleton
public class MicroschemaComparator extends AbstractFieldSchemaContainerComparator<Microschema> {

	@Inject
	public MicroschemaComparator() {
	}

	@Override
	public List<SchemaChangeModel> diff(Microschema containerA, Microschema containerB) {
		return super.diff(containerA, containerB, Microschema.class);
	}

}
