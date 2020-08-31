package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.data.HibBaseElement;

public interface HibSchemaChange<T> extends HibBaseElement {

	HibSchemaChange<?> getNextChange();

}
