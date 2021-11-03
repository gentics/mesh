package com.gentics.mesh.core.data;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.rest.lang.LanguageResponse;

import static com.gentics.mesh.ElementType.LANGUAGE;

/**
 * Domain model for languages.
 */
public interface HibLanguage extends HibCoreElement<LanguageResponse>, HibNamedElement {

	TypeInfo TYPE_INFO = new TypeInfo(LANGUAGE, null, null, null);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Native name of the language.
	 * 
	 * @return
	 */
	String getNativeName();

	/**
	 * ISO 639-1 code of the language.
	 * 
	 * @return
	 */
	String getLanguageTag();

	/**
	 * Set the native name of the language.
	 * 
	 * @param languageNativeName
	 */
	void setNativeName(String languageNativeName);

	/**
	 * Set the language tag.
	 * 
	 * @param languageTag
	 */
	void setLanguageTag(String languageTag);

	@Override
	default String getAPIPath(InternalActionContext ac) {
		// Languages don't have a public location
		return null;
	}
}
