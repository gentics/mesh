package com.gentics.mesh.core.data;

import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.rest.lang.LanguageResponse;

/**
 * Graph Domain Model interface for languages.
 */
public interface Language extends MeshCoreVertex<LanguageResponse, Language>, NamedElement<Vertex> {

	String TYPE = "language";

	TypeInfo TYPE_INFO = new TypeInfo(TYPE, null, null, null);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Return the native name of the language.
	 * 
	 * @return
	 */
	String getNativeName();

	/**
	 * Set the native name of the language.
	 * 
	 * @param languageNativeName
	 */
	void setNativeName(String languageNativeName);

	/**
	 * Return the language tag.
	 * 
	 * @return
	 */
	String getLanguageTag();

	/**
	 * Set the language tag.
	 * 
	 * @param languageTag
	 */
	void setLanguageTag(String languageTag);

}
