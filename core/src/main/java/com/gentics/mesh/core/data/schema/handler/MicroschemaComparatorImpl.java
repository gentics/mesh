package com.gentics.mesh.core.data.schema.handler;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

/**
 * @see MicroschemaComparator
 */
@Singleton
public class MicroschemaComparatorImpl extends AbstractFieldSchemaContainerComparator<MicroschemaModel> implements MicroschemaComparator {

	@Inject
	public MicroschemaComparatorImpl() {
	}

	@Override
	public List<SchemaChangeModel> diff(MicroschemaModel containerA, MicroschemaModel containerB) {
		return super.diff(containerA, containerB, MicroschemaModel.class);
	}

}
