package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Collections;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.event.EventQueueBatch;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * A persisting extension to {@link LanguageDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingLanguageDao extends LanguageDao, PersistingDaoGlobal<HibLanguage>, PersistingNamedEntityDao<HibLanguage> {

	static final Logger log = LoggerFactory.getLogger(PersistingLanguageDao.class);

	default HibLanguage create(String languageName, String languageTag, String uuid) {
		HibLanguage language = createPersisted(uuid, l -> {
			l.setName(languageName);
			l.setLanguageTag(languageTag);
		});
		uncacheSync(mergeIntoPersisted(language));
		return language;
	}

	@Override
	default void assign(HibLanguage language, HibProject project, EventQueueBatch batch, boolean throwOnExisting) {
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
	default void unassign(HibLanguage language, HibProject project, EventQueueBatch batch, boolean throwOnInexisting) {
		project.getLanguages().stream()
			.filter(l -> l.getLanguageTag().equals(language.getLanguageTag()))
			.findAny()
			.ifPresentOrElse(existing -> {
					CommonTx.get().nodeDao().findUsedLanguages(project, Collections.singletonList(language.getLanguageTag()), true).stream().findAny().ifPresentOrElse(existingContent -> {
						throw error(BAD_REQUEST, "error_language_still_in_use", language.getLanguageTag(), project.getName());
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
	default LanguageResponse transformToRestSync(HibLanguage element, InternalActionContext ac, int level,
			String... languageTags) {
		LanguageResponse model = new LanguageResponse();
		model.setUuid(element.getUuid());
		model.setLanguageTag(element.getLanguageTag());
		model.setName(element.getName());
		model.setNativeName(element.getNativeName());
		return model;
	}

	@Override
	default HibLanguage create(String languageName, String languageTag) {
		return create(languageName, languageTag, null);
	}

	@Override
	default boolean update(HibLanguage element, InternalActionContext ac, EventQueueBatch batch) {
		throw error(BAD_REQUEST, "error_language_update_forbidden");
	}

	@Override
	default void delete(HibLanguage element, BulkActionContext bac) {
		throw error(BAD_REQUEST, "error_language_deletion_forbidden");
	}
}
