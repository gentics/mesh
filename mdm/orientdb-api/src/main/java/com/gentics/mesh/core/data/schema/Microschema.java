package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.search.GraphDBBucketableElement;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

/**
 * A microschema container is a graph element which stores the JSON microschema data.
 */
public interface Microschema extends
		GraphFieldSchemaContainer<MicroschemaResponse, MicroschemaVersionModel, MicroschemaReference, HibMicroschema, HibMicroschemaVersion>, HibMicroschema, GraphDBBucketableElement {


	@Override
	default MicroschemaReference transformToReference() {
		return HibMicroschema.super.transformToReference();
	}

	@Override
	default TypeInfo getTypeInfo() {
		return HibMicroschema.super.getTypeInfo();
	}
}
