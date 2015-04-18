package com.gentics.cailun.core.data.service.tag;

import io.vertx.ext.apex.Session;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.I18NProperties;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.content.ContentTransformationTask;
import com.gentics.cailun.core.data.service.content.TransformationInfo;
import com.gentics.cailun.core.rest.content.response.ContentResponse;
import com.gentics.cailun.core.rest.schema.response.SchemaReference;
import com.gentics.cailun.core.rest.tag.response.TagResponse;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
import com.gentics.cailun.util.PermissionUtils;

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
					restTag.setSchema(new SchemaReference(schema.getName(), schema.getUuid()));
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
					session.hasPermission(PermissionUtils.convert(currentTag, PermissionType.READ), handler -> {
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

						Collections.sort(restTag.getTags(), new Comparator<TagResponse>() {

							@Override
							public int compare(TagResponse o1, TagResponse o2) {
								// TODO use order and sorting here?
								String uuid1 = o1.getUuid();
								String uuid2 = o2.getUuid();
								if (uuid1 == null) {
									uuid1 = "";
								}
								if (uuid2 == null) {
									uuid2 = "";
								}
								return uuid1.compareTo(uuid2);
							}

						});

					}
				}	);

					tx.success();
				}

			});

			tag.getContents()
					.parallelStream()
					.forEachOrdered(
							currentContent -> {
								try (Transaction tx = info.getGraphDb().beginTx()) {
									String currentUuid = currentContent.getUuid();
									info.getRoutingContext()
											.session()
											.hasPermission(
													PermissionUtils.convert(currentContent, PermissionType.READ),
													handler -> {

														if (handler.result()) {
															Content loadedContent = info.getNeo4jTemplate().fetch(currentContent);
															ContentResponse currentRestContent = (ContentResponse) info.getObject(currentUuid);
															if (currentRestContent == null) {
																currentRestContent = new ContentResponse();
																ContentTransformationTask subTask = new ContentTransformationTask(loadedContent,
																		info, currentRestContent, depth + 1);
																tasks.add(subTask.fork());
															}
															restTag.getContents().add(currentRestContent);

														}
													});
									tx.success();
								}
							});

		}
		tasks.forEach(action -> action.join());
		return null;
	}
}
