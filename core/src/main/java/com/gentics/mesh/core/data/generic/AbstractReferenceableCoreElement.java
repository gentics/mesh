package com.gentics.mesh.core.data.generic;

import com.gentics.mesh.core.data.NamedVertex;
import com.gentics.mesh.core.data.ReferenceableElement;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.handler.InternalActionContext;

public abstract class AbstractReferenceableCoreElement<T extends RestModel, TR extends NameUuidReference<TR>> extends AbstractCoreElement<T>
		implements  ReferenceableElement<TR>, NamedVertex {

	abstract protected TR createEmptyReferenceModel();

	@Override
	public TR transformToReference(InternalActionContext ac) {
		TR reference = createEmptyReferenceModel();
		reference.setName(getName());
		reference.setUuid(getUuid());
		return reference;
	}

}
