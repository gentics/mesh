package com.gentics.mesh.core.data.service.transformation.tag;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.data.model.I18NProperties;
import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.data.model.ObjectSchema;
import com.gentics.mesh.core.data.model.Tag;
import com.gentics.mesh.core.data.model.auth.User;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.data.service.transformation.content.ContentTraversalConsumer;
import com.gentics.mesh.core.rest.schema.response.SchemaReference;
import com.gentics.mesh.core.rest.tag.response.TagResponse;
import com.gentics.mesh.error.HttpStatusCodeErrorException;

public class TagTransformationTask extends RecursiveTask<Void> {

	private static final long serialVersionUID = -5106639943022399262L;

	private static final Logger log = LoggerFactory.getLogger(TagTransformationTask.class);

	private Tag tag;
	private TransformationInfo info;
	private TagResponse restTag;
	private int currentDepth;

	public TagTransformationTask(Tag tag, TransformationInfo info, TagResponse restTag, int depth) {
		this.tag = tag;
		this.info = info;
		this.restTag = restTag;
		this.currentDepth = depth;
	}

	public TagTransformationTask(Tag tag, TransformationInfo info, TagResponse restTag) {
		this(tag, info, restTag, 0);
	}

	@Override
	protected Void compute() {
		Set<ForkJoinTask<Void>> tasks = new HashSet<>();
		String uuid = tag.getUuid();
		// Check whether the tag has already been transformed by another task
		TagResponse foundTag = (TagResponse) info.getObjectReferences().get(uuid);
		if (foundTag == null) {
			try (Transaction tx = info.getGraphDb().beginTx()) {

				restTag.setPerms(info.getUserService().getPerms(info.getRoutingContext(), tag));

				restTag.setUuid(tag.getUuid());
				if (tag.getSchema() != null) {
					ObjectSchema schema = info.getNeo4jTemplate().fetch(tag.getSchema());
					SchemaReference schemaReference = new SchemaReference();
					schemaReference.setSchemaName(schema.getName());
					schemaReference.setSchemaUuid(schema.getUuid());
					restTag.setSchema(schemaReference);
				}

				User creator = tag.getCreator();
				if (creator != null) {
					creator = info.getNeo4jTemplate().fetch(creator);
					restTag.setCreator(info.getUserService().transformToRest(creator, 0));
				}

				for (String languageTag : info.getLanguageTags()) {
					Language language = info.getLanguageService().findByLanguageTag(languageTag);
					if (language == null) {
						throw new HttpStatusCodeErrorException(400, info.getI18n().get(info.getRoutingContext(), "error_language_not_found",
								languageTag));
					}
					// TODO tags can also be dynamically enhanced. Maybe we should check the schema here? This would be costly. Currently we are just returning
					// all
					// found i18n properties for the language.

					// Add all i18n properties for the selected language to the response
					I18NProperties i18nProperties = info.getTagService().getI18NProperties(tag, language);
					if (i18nProperties != null) {
						i18nProperties = info.getNeo4jTemplate().fetch(i18nProperties);
						for (String key : i18nProperties.getProperties().getPropertyKeys()) {
							restTag.addProperty(languageTag, key, i18nProperties.getProperty(key));
						}
					} else {
						log.error("Could not find any i18n properties for language {" + languageTag + "}. Skipping language.");
						continue;
					}
				}
				tx.success();
			}

			info.addObject(uuid, restTag);
		} else {
			restTag = foundTag;
		}
		
//		if (currentDepth < info.getMaxDepth()) {
//		}
//		if (info.isIncludeTags()) {
//			TagTraversalConsumer tagConsumer = new TagTraversalConsumer(info, currentDepth, restTag, tasks);
//			tag.getTags().parallelStream().forEachOrdered(tagConsumer);
//		} else {
//			restTag.setTags(null);
//		}
//
//		if (info.isIncludeContents()) {
//			ContentTraversalConsumer contentConsumer = new ContentTraversalConsumer(info, currentDepth, restTag, tasks);
//			tag.getContents().parallelStream().forEachOrdered(contentConsumer);
//		} else {
//			restTag.setContents(null);
//		}
//
//		if (info.isIncludeChildTags()) {
//			TagTraversalConsumer tagConsumer = new TagTraversalConsumer(info, currentDepth, restTag, tasks);
//			tag.getChildTags().parallelStream().forEachOrdered(tagConsumer);
//		} else {
//			restTag.setChildTags(null);
//		}

		// Wait for our forked tasks
		tasks.forEach(action -> action.join());
		return null;
	}
}
