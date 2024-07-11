package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;

import java.util.Collections;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A persisting extension to {@link LanguageDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingLanguageDao extends LanguageDao, PersistingDaoGlobal<Language>, PersistingNamedEntityDao<Language> {

	static final Logger log = LoggerFactory.getLogger(PersistingLanguageDao.class);

	default Language create(String languageName, String languageTag, String uuid) {
		Language language = createPersisted(uuid, l -> {
			l.setName(languageName);
			l.setLanguageTag(languageTag);
		});
		uncacheSync(mergeIntoPersisted(language));
		return language;
	}

	@Override
	default void assign(Language language, Project project, EventQueueBatch batch, boolean throwOnExisting) {
		project.getLanguages().stream()
			.filter(l -> l.getLanguageTag().equals(language.getLanguageTag()))
			.findAny()
			.ifPresentOrElse(existing -> {
					if (throwOnExisting) {
						throw error(BAD_REQUEST, "error_language_already_assigned", language.getLanguageTag(), project.getName());
					} else {
						log.debug("Language [{}] is already assigned to the project [{}]", language.getLanguageTag(), project.getName());
					}
				}, () -> {
					project.addLanguage(language);
				});
	}

	@Override
	default void unassign(Language language, Project project, EventQueueBatch batch, boolean throwOnInexisting) {
		project.getLanguages().stream()
			.filter(l -> l.getLanguageTag().equals(language.getLanguageTag()))
			.findAny()
			.ifPresentOrElse(existing -> {
					CommonTx.get().nodeDao().findUsedLanguages(project, Collections.singletonList(language.getLanguageTag()), true).stream().findAny().ifPresentOrElse(existingContent -> {
						throw error(CONFLICT, "error_language_still_in_use", language.getLanguageTag(), project.getName());
					}, () -> {
						project.removeLanguage(language);
					});
				}, () -> {
					if (throwOnInexisting) {
						throw error(BAD_REQUEST, "error_language_not_assigned", language.getLanguageTag(), project.getName());
					} else {
						log.debug("Language [{}] is not assigned to the project [{}]", language.getLanguageTag(), project.getName());
					}
				});
	}

	@Override
	default LanguageResponse transformToRestSync(Language element, InternalActionContext ac, int level,
			String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		LanguageResponse model = new LanguageResponse();
		if (fields.has("uuid")) {
			model.setUuid(element.getUuid());

			// Performance shortcut to return now and ignore the other checks
			if (fields.size() == 1) {
				return model;
			}
		}
		if (fields.has("name")) {
			model.setName(element.getName());

			// Performance shortcut to return now and ignore the other checks
			if (fields.size() == 1) {
				return model;
			}
		}
		if (fields.has("languageTag")) {
			model.setLanguageTag(element.getLanguageTag());

			// Performance shortcut to return now and ignore the other checks
			if (fields.size() == 1) {
				return model;
			}
		}
		if (fields.has("nativeName")) {
			model.setNativeName(element.getNativeName());
		}
		return model;
	}

	@Override
	default Language create(String languageName, String languageTag) {
		return create(languageName, languageTag, null);
	}

	@Override
	default boolean update(Language element, InternalActionContext ac, EventQueueBatch batch) {
		throw error(METHOD_NOT_ALLOWED, "error_language_update_forbidden");
	}

	@Override
	default void delete(Language element, BulkActionContext bac) {
		throw error(METHOD_NOT_ALLOWED, "error_language_deletion_forbidden");
	}
}
