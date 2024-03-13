package com.gentics.mesh.core.data.schema.handler;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.NO_INDEX_KEY;

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
		List<SchemaChangeModel> changes = super.diff(containerA, containerB, MicroschemaModel.class);

		// .noIndex
		compareAndAddSchemaProperty(changes, NO_INDEX_KEY, containerA.getNoIndex(), containerB.getNoIndex(), MicroschemaModel.class);

		return changes;
	}

}
