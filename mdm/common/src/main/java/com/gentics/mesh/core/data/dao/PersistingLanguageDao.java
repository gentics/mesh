package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

/**
 * A persisting extension to {@link LanguageDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingLanguageDao extends LanguageDao, PersistingDaoGlobal<HibLanguage>, PersistingNamedEntityDao<HibLanguage> {

	default HibLanguage create(String languageName, String languageTag, String uuid) {
		HibLanguage language = createPersisted(uuid, l -> {
			l.setName(languageName);
			l.setLanguageTag(languageTag);
		});
		uncacheSync(mergeIntoPersisted(language));
		return language;
	}

	@Override
	default LanguageResponse transformToRestSync(HibLanguage element, InternalActionContext ac, int level,
			String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		LanguageResponse model = new LanguageResponse();
		// TODO align fields.isEmpty() with other DAOs on a major update - this is breaking at the moment.
		if (fields.isEmpty() || fields.has("uuid")) {
			model.setUuid(element.getUuid());

			// Performance shortcut to return now and ignore the other checks
			if (fields.size() == 1) {
				return model;
			}
		}
		if (fields.isEmpty() || fields.has("name")) {
			model.setName(element.getName());

			// Performance shortcut to return now and ignore the other checks
			if (fields.size() == 1) {
				return model;
			}
		}
		if (fields.isEmpty() || fields.has("tag")) {
			model.setLanguageTag(element.getLanguageTag());

			// Performance shortcut to return now and ignore the other checks
			if (fields.size() == 1) {
				return model;
			}
		}
		if (fields.isEmpty() || fields.has("nativeName")) {
			model.setNativeName(element.getNativeName());
		}		
		return model;
	}

	@Override
	default HibLanguage create(String languageName, String languageTag) {
		return create(languageName, languageTag, null);
	}

	@Override
	default boolean update(HibLanguage element, InternalActionContext ac, EventQueueBatch batch) {
		throw new IllegalStateException("Languages can't be updated");
	}

	@Override
	default void delete(HibLanguage element, BulkActionContext bac) {
		throw new IllegalStateException("Languages can't be deleted");
	}
}
