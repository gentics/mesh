package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * A persisting extension to {@link LanguageDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingLanguageDao extends LanguageDao, PersistingDaoGlobal<HibLanguage> {

	default HibLanguage create(String languageName, String languageTag, String uuid) {
		HibLanguage language = createPersisted(uuid);
		language.setName(languageName);
		language.setLanguageTag(languageTag);
		return mergeIntoPersisted(language);
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
