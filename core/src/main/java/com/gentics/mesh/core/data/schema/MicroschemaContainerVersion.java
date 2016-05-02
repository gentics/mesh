package com.gentics.mesh.core.data.schema;

import java.util.List;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

public interface MicroschemaContainerVersion
		extends GraphFieldSchemaContainerVersion<Microschema, MicroschemaReference, MicroschemaContainerVersion, MicroschemaContainer> {
	/**
	 * Return a list {@link NodeGraphFieldContainer} that contain at least one
	 * micronode field (or list of micronodes field) that uses this schema
	 * version for the given release
	 *
	 * @param releaseUuid
	 * @return
	 */
	List<? extends NodeGraphFieldContainer> getFieldContainers(String releaseUuid);
}
