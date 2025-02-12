package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.dagger.annotations.ElementTypeKey;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Language entity implementation for Gentics Mesh.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "language")
@ElementTypeKey(ElementType.LANGUAGE)
public class HibLanguageImpl extends AbstractHibUserTrackedElement<LanguageResponse> implements HibLanguage, Serializable {

	private static final long serialVersionUID = -8944598598650779513L;

	@Column
	private String nativeName;

	@Column(unique = true, length = 8)
	private String languageTag;

	@Override
	public String getNativeName() {
		return nativeName;
	}

	@Override
	public String getLanguageTag() {
		return languageTag;
	}

	@Override
	public void setNativeName(String nativeName) {
		this.nativeName = nativeName;
	}

	@Override
	public void setLanguageTag(String languageTag) {
		this.languageTag = languageTag;
	}
}
