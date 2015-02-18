package com.gentics.cailun.core.rest.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.repository.GlobalLanguageRepository;
import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.service.generic.GenericNodeServiceImpl;

@Component
@Transactional
public class LanguageServiceImpl extends GenericNodeServiceImpl<Language> implements LanguageService {
	
	@Autowired
	private GlobalLanguageRepository languageRepository;

	@Override
	public Language save(Language language) {
		if(StringUtils.isEmpty(language.getLanguageTag())|| StringUtils.isEmpty(language.getName())) {
			//TODO throw exception?
		}
		return languageRepository.save(language);
	}
	
}
