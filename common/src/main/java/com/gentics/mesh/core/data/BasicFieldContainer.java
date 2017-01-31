package com.gentics.mesh.core.data;

public interface BasicFieldContainer extends MeshVertex {

	/**
	 * Return the language of the field container.
	 * 
	 * @return
	 */
	Language getLanguage();

	/**
	 * Set the language for the field container.
	 * 
	 * @param language
	 */
	void setLanguage(Language language);

}
