package com.gentics.mesh.core.rest.micronode;

import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;

public class NullMicronodeResponse extends MicronodeResponse {
	@Override
	public FieldMap getFields() {
		return null;
	}

	@Override
	public MicroschemaReference getMicroschema() {
		return null;
	}

	@Override
	public String getUuid() {
		return null;
	}
}
