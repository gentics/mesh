package com.gentics.mesh.core.data;

import static com.gentics.mesh.ElementType.LANGUAGE;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.rest.lang.LanguageResponse;

/**
 * Graph Domain Model interface for languages.
 */
public interface Language extends MeshCoreVertex<LanguageResponse>, NamedElement, HibLanguage {

	TypeInfo TYPE_INFO = new TypeInfo(LANGUAGE, null, null, null);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}
}
