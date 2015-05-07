package com.gentics.mesh.core.data.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.data.service.generic.GenericNodeServiceImpl;
import com.gentics.mesh.core.repository.LanguageRepository;

@Component
@Transactional(readOnly = true)
public class LanguageServiceImpl extends GenericNodeServiceImpl<Language> implements LanguageService {

	@Autowired
	private LanguageRepository languageRepository;

	@Override
	public Language save(Language language) {
		if (StringUtils.isEmpty(language.getLanguageTag()) || StringUtils.isEmpty(language.getName())) {
			// TODO throw exception?
		}
		return languageRepository.save(language);
	}

	@Override
	public Language findByName(String name) {
		return languageRepository.findByName(name);
	}

	@Override
	public Language findByLanguageTag(String languageTag) {
		return languageRepository.findByLanguageTag(languageTag);
	}

}
