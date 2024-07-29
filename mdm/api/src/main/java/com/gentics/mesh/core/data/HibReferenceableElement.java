package com.gentics.mesh.core.data;

/**
 * A referenceable element is an graph element which can be transformed to a rest reference model.
 * 
 * @param <TR>
 */
public interface HibReferenceableElement<TR> extends HibNamedElement {

	/**
	 * Transform the element to a rest model that represents a references.
	 */
	TR transformToReference();
}
