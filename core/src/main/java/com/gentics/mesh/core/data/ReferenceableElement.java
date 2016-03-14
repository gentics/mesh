package com.gentics.mesh.core.data;

import com.gentics.mesh.core.rest.common.NameUuidReference;

/**
 * A referenceable element is an graph element which can be transformed to a rest reference model.
 * 
 * @param
 * 			<TR>
 */
public interface ReferenceableElement<TR extends NameUuidReference<TR>> extends NamedElement {

	TR createEmptyReferenceModel();

	/**
	 * Transform the element to a rest model that represents a references.
	 */
	default TR transformToReference() {
		TR reference = createEmptyReferenceModel();
		reference.setName(getName());
		reference.setUuid(getUuid());
		return reference;
	}
}
