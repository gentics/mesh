package com.gentics.cailun.core.rest.service.generic;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.repository.GlobalI18NValueRepository;
import com.gentics.cailun.core.repository.generic.GlobalGenericNodeRepository;
import com.gentics.cailun.core.rest.model.I18NProperties;
import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.model.generic.GenericNode;
import com.gentics.cailun.core.rest.model.relationship.Translated;

@Component
@Transactional
public class GenericNodeServiceImpl<T extends GenericNode> implements GenericNodeService<T> {

	@Autowired
	GlobalI18NValueRepository i18nPropertyRepository;

	@Autowired
	@Qualifier("globalGenericNodeRepository")
	GlobalGenericNodeRepository<T> nodeRepository;

	/**
	 * Adds or updates the i18n value for the given language and key with the given value.
	 * 
	 * @param language
	 *            Language for the i18n value
	 * @param key
	 *            Key of the value
	 * @param value
	 *            The i18n text value
	 */
	public void setProperty(T node, Language language, String key, String value) {

		if (node == null || StringUtils.isEmpty(key) || language == null) {
			// TODO exception? boolean return?
			return;
		}

		I18NProperties i18nProperties = node.getI18NProperties(language);
		if (i18nProperties == null) {
			i18nProperties = new I18NProperties(language);
			i18nProperties.addProperty(key, value);
			i18nProperties = i18nPropertyRepository.save(i18nProperties);
			node.getI18nTranslations().add(new Translated(node, i18nProperties, language));
		} else {
			i18nProperties.addProperty(key, value);
		}

	}

	public void setName(T node, Language language, String name) {
		setProperty(node, language, GenericNode.NAME_KEYWORD, name);
	}

	@Override
	public T save(T node) {
		return nodeRepository.save(node);
	}

	@Override
	public void delete(T node) {
		nodeRepository.delete(node);
	}

	@Override
	public T findOne(Long id) {
		return nodeRepository.findOne(id);
	}

	@Override
	public void save(List<T> nodes) {
		this.nodeRepository.save(nodes);
	}

	@Override
	public Result<T> findAll() {
		return nodeRepository.findAll();
	}

}
