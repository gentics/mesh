package com.gentics.mesh.core.data;

public interface BasicFieldContainer extends MeshVertex {

	/**
	 * Return the language tag of the field container.
	 * 
	 * @return
	 */
	String getLanguageTag();

	/**
	 * Set the language for the field container.
	 * 
	 * @param languageTag
	 */
	void setLanguageTag(String languageTag);

}
