package com.gentics.cailun.core.data.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.I18NProperties;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.data.service.generic.GenericTagServiceImpl;
import com.gentics.cailun.core.repository.TagRepository;
import com.gentics.cailun.core.rest.tag.response.TagResponse;

@Component
@Transactional
public class TagServiceImpl extends GenericTagServiceImpl<Tag, GenericFile> implements TagService {

	private static final Logger log = LoggerFactory.getLogger(TagServiceImpl.class);

	@Autowired
	private TagRepository tagRepository;

	@Autowired
	private LanguageService languageService;

	@Autowired
	private UserService userService;

	@Override
	public Tag findByProjectPath(String projectName, String path) {
		// TODO find the tag by traversing the path
		return null;
	}

	@Override
	public TagResponse transformToRest(Tag tag, List<String> languageTags) {
		TagResponse response = new TagResponse();

		for (String languageTag : languageTags) {
			Language language = languageService.findByLanguageTag(languageTag);
			if (language == null) {
				// TODO should we just omit the language or abort?
				log.error("No language found for language tag {" + languageTag + "}. Skipping lanuage.");
				continue;
			}
			// TODO tags can also be dynamically enhanced. Maybe we should check the schema here? This would be costly. Currently we are just returning all
			// found i18n properties for the language.

			// Add all i18n properties for the selected language to the response
			I18NProperties i18nProperties = tag.getI18NProperties(language);
			if (i18nProperties != null) {
				for (String key : i18nProperties.getProperties().getPropertyKeys()) {
					response.addProperty(languageTag, key, i18nProperties.getProperty(key));
				}
			} else {
				log.error("Could not find any i18n properties for language {" + languageTag + "}. Skipping language.");
				continue;
			}
		}
		response.setUuid(tag.getUuid());

		// TODO handle files and subtags:
		// tag.getTags()
		// tag.getFiles()
		response.setCreator(userService.transformToRest(tag.getCreator()));
		// TODO handle properties for the type of tag
		return response;

	}
}
