package com.gentics.cailun.core.data.service.tag;

import io.vertx.ext.apex.Session;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.function.Consumer;

import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.I18NProperties;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.content.ContentTransformationTask;
import com.gentics.cailun.core.data.service.content.TransformationInfo;
import com.gentics.cailun.core.rest.content.response.ContentResponse;
import com.gentics.cailun.core.rest.schema.response.SchemaReference;
import com.gentics.cailun.core.rest.tag.response.TagResponse;
import com.gentics.cailun.error.HttpStatusCodeErrorException;

public class TagTransformationTask extends RecursiveTask<Void> {

	private static final long serialVersionUID = -5106639943022399262L;

	private static final Logger log = LoggerFactory.getLogger(TagTransformationTask.class);

	private Tag tag;
	private TransformationInfo info;
	private TagResponse restTag;
	private int depth;

	public TagTransformationTask(Tag tag, TransformationInfo info, TagResponse restTag, int depth) {
		this.tag = tag;
		this.info = info;
		this.restTag = restTag;
		this.depth = depth;
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
					restTag.setCreator(info.getUserService().transformToRest(creator));
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

		if (depth < info.getMaxDepth()) {
			tag.getTags().parallelStream().forEachOrdered(currentTag -> {
				try (Transaction tx = info.getGraphDb().beginTx()) {
					String currentUuid = currentTag.getUuid();
					Session session = info.getRoutingContext().session();
					session.hasPermission(new CaiLunPermission(currentTag, PermissionType.READ).toString(), handler -> {

						try (Transaction tx2 = info.getGraphDb().beginTx()) {
							if (handler.result()) {
								Tag loadedTag = info.getNeo4jTemplate().fetch(currentTag);
								TagResponse currentRestTag = (TagResponse) info.getObject(currentUuid);
								if (currentRestTag == null) {
									currentRestTag = new TagResponse();
									// info.addTag(currentUuid, currentRestTag);
							TagTransformationTask subTask = new TagTransformationTask(loadedTag, info, currentRestTag, depth + 1);
							tasks.add(subTask.fork());

						}
						restTag.getTags().add(currentRestTag);
						tx2.success();
					}

					Collections.sort(restTag.getTags(), new UuidRestModelComparator<TagResponse>());

				}
			}		);

					tx.success();
				}

			});

			ContentTraversalConsumer contentConsumer = new ContentTraversalConsumer(info, depth, restTag, tasks);
			tag.getContents().parallelStream().forEachOrdered(contentConsumer);

		}
		tasks.forEach(action -> action.join());
		return null;
	}
}

class ContentTraversalConsumer implements Consumer<Content> {

	private TransformationInfo info;
	private int currentDepth;
	private TagResponse restTag;
	private Set<ForkJoinTask<Void>> tasks; 

	public ContentTraversalConsumer(TransformationInfo info, int currentDepth, TagResponse restTag, Set<ForkJoinTask<Void>>  tasks) {
		this.info = info;
		this.currentDepth = currentDepth;
		this.restTag = restTag;
		this.tasks = tasks;
	}

	@Override
	public void accept(Content content) {
		try (Transaction tx = info.getGraphDb().beginTx()) {
			String currentUuid = content.getUuid();
			info.getRoutingContext()
					.session()
					.hasPermission(
							new CaiLunPermission(content, PermissionType.READ).toString(),
							handler -> {
								try (Transaction tx2 = info.getGraphDb().beginTx()) {

									if (handler.result()) {
										Content loadedContent = info.getNeo4jTemplate().fetch(content);
										ContentResponse currentRestContent = (ContentResponse) info.getObject(currentUuid);
										if (currentRestContent == null) {
											currentRestContent = new ContentResponse();
											ContentTransformationTask subTask = new ContentTransformationTask(loadedContent, info,
													currentRestContent, currentDepth + 1);
											tasks.add(subTask.fork());
										}
										restTag.getContents().add(currentRestContent);

									}
									tx2.success();
								}
							});
			tx.success();
		}

	}

}
