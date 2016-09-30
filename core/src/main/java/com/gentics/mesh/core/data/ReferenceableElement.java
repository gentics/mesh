package com.gentics.mesh.core.data;

/**
 * A referenceable element is an graph element which can be transformed to a rest reference model.
 * 
 * @param <TR>
 */
public interface ReferenceableElement<TR> extends NamedElement {

	/**
	 * Transform the element to a rest model that represents a references.
	 */
	TR transformToReference();
}
