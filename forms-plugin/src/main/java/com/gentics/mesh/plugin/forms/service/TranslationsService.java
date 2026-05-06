package com.gentics.mesh.plugin.forms.service;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.parameter.client.GenericParametersImpl;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.dagger.UserClient;
import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.model.TranslationLanguageModel;
import com.gentics.mesh.plugin.forms.model.TranslationsModel;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Single;

/**
 * Service for handling translations
 */
@Singleton
public class TranslationsService {
	private static final Logger log = LoggerFactory.getLogger(TranslationsService.class);
	

	private final MeshRestClient userClient;
	private final MeshRestClient adminClient;
	private final StatusService statusService;
	private final FormService formService;
	private final ObjectMapper mapper;

	@Inject
	public TranslationsService(
			@UserClient @Nullable MeshRestClient userClient,
			@AdminClient @Nullable MeshRestClient adminClient,
			StatusService statusService,
			FormService formService,
			ObjectMapper mapper) {
		this.userClient = userClient;
		this.adminClient = adminClient;
		this.statusService = statusService;
		this.formService = formService;
		this.mapper = mapper;
	}

	/**
	 * Get the full translations model for the given project.
	 *
	 * @param projectName project name
	 * @return single translations model
	 */
	public Single<TranslationsModel> getTranslations(String projectName) {
		return statusService.getRootNode(projectName, adminClient).flatMap(response -> {
			FieldMap fields = response.getFields();
			if (fields.hasField("translations") && fields.getStringField("translations") != null) {
				String json = fields.getStringField("translations").getString();
				if (json != null && !json.isEmpty()) {
					try {
						return Single.just(mapper.readValue(json, TranslationsModel.class));
					} catch (Exception e) {
						return Single.error(e);
					}
				}
			}
			return Single.just(new TranslationsModel());
		});
	}

	/**
	 * Get translations for a specific form and language.
	 *
	 * <p>Loads the form to determine its formType. If the form is not found,
	 * falls back to the default translations for the given language. If the form
	 * is found, the formType is used to look up the matching type translations;
	 * if no type entry exists, default translations are returned.</p>
	 *
	 * @param projectName project name
	 * @param formUuid    UUID of the form node
	 * @param lang        language code (e.g. "de", "en")
	 * @return single map of translation key → translated string
	 */
	public Single<Map<String, String>> getTranslations(String projectName, String formUuid, String lang) {
		return formService.loadFormAsAdmin(projectName, formUuid)
			.flatMap(formJson -> {
				try {
					Form form = formService.toForm(formJson.encode(), formUuid, null);
					String formType = form.getType();
					return getTranslations(projectName).map(model -> translationsForType(model, formType, lang));
				} catch (Exception e) {
					log.error("Error while parsing form", e);
					return getTranslations(projectName).map(model -> translationsForDefault(model, lang));
				}
			})
			.onErrorResumeNext(t ->
				getTranslations(projectName).map(model -> translationsForDefault(model, lang))
			);
	}

	/**
	 * Set the translations for the given project.
	 *
	 * @param projectName project name
	 * @param model the translations to store
	 * @return single translations model (as stored)
	 */
	public Single<TranslationsModel> setTranslations(String projectName, TranslationsModel model) {
		try {
			String json = mapper.writeValueAsString(model);
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setVersion("draft");
			update.setLanguage("en");
			update.getFields().put("translations", new StringFieldImpl().setString(json));
			return statusService.getRootNode(projectName, adminClient).flatMap(rootNode -> {				
				FieldMap fields = rootNode.getFields();
				if (fields.hasField("translations") && fields.getStringField("translations") != null) {
					String savedJson = fields.getStringField("translations").getString();
					if (savedJson != null && !savedJson.isEmpty()) {
						if(savedJson.equals(json)) {
							//nothing changed, no save necessary
							return Single.just(model);
						}
					}
				}
				return userClient.updateNode(projectName, rootNode.getUuid(), update,
						new GenericParametersImpl().setETag(false).setFields("fields", "uuid")).toSingle()
					.flatMap(response -> userClient.publishNode(projectName, response.getUuid()).toSingle()
						.map(r -> model));
			});
		} catch (Exception e) {
			return Single.error(e);
		}
	}

	private Map<String, String> translationsForType(TranslationsModel model, String formType, String lang) {
		if (formType != null && model.types != null) {
			TranslationLanguageModel typeModel = model.types.get(formType);
			if (typeModel != null) {
				Map<String, String> result = translationsForLang(typeModel, lang);
				if (!result.isEmpty()) {
					return result;
				}
			}
		}
		return translationsForDefault(model, lang);
	}

	private Map<String, String> translationsForDefault(TranslationsModel model, String lang) {
		TranslationLanguageModel defaultModel = model.defaultTranslations;
		if (defaultModel == null) {
			return Collections.emptyMap();
		}
		return translationsForLang(defaultModel, lang);
	}

	private Map<String, String> translationsForLang(TranslationLanguageModel langModel, String lang) {
		return langModel.getOrDefault(lang, Collections.emptyMap());
	}
}
