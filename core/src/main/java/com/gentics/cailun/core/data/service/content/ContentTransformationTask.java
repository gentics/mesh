package com.gentics.cailun.core.data.service.content;

import io.vertx.ext.apex.Session;

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
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.model.relationship.Translated;
import com.gentics.cailun.core.data.service.tag.TagTransformationTask;
import com.gentics.cailun.core.rest.content.response.ContentResponse;
import com.gentics.cailun.core.rest.schema.response.SchemaReference;
import com.gentics.cailun.core.rest.tag.response.TagResponse;
import com.gentics.cailun.error.HttpStatusCodeErrorException;

public class ContentTransformationTask extends RecursiveTask<Void> {

	private static final long serialVersionUID = -1480528776879617657L;

	private static final Logger log = LoggerFactory.getLogger(ContentTransformationTask.class);

	private Content content;
	private TransformationInfo info;
	private ContentResponse restContent;
	private int depth;

	public ContentTransformationTask(Content content, TransformationInfo info, ContentResponse restContent, int depth) {
		this.content = content;
		this.info = info;
		this.restContent = restContent;
		this.depth = depth;
	}

	public ContentTransformationTask(Content content, TransformationInfo info, ContentResponse restContent) {
		this(content, info, restContent, 0);
	}

	@Override
	protected Void compute() {

		Set<ForkJoinTask<Void>> tasks = new HashSet<>();
		String uuid = content.getUuid();
		// Check whether the content has already been transformed by another task
		ContentResponse foundContent = (ContentResponse) info.getObjectReferences().get(uuid);
		if (foundContent == null) {
			try (Transaction tx = info.getGraphDb().beginTx()) {
				restContent.setPerms(info.getUserService().getPerms(info.getRoutingContext(), content));
				restContent.setUuid(content.getUuid());
				if (content.getSchema() != null) {
					ObjectSchema schema = info.getNeo4jTemplate().fetch(content.getSchema());
					SchemaReference schemaReference = new SchemaReference();
					schemaReference.setSchemaName(schema.getName());
					schemaReference.setSchemaUuid(schema.getUuid());
					restContent.setSchema(schemaReference);
				}

				User creator = content.getCreator();
				if (creator != null) {
					creator = info.getNeo4jTemplate().fetch(creator);
					restContent.setCreator(info.getUserService().transformToRest(creator, 0));
				}

				restContent.setOrder(content.getOrder());
				if (info.getLanguageTags().size() == 0) {
					for (Translated transalated : content.getI18nTranslations()) {
						String languageTag = transalated.getLanguageTag();
						// TODO handle schema
						I18NProperties properties = transalated.getI18nProperties();
						properties = info.getNeo4jTemplate().fetch(properties);
						restContent.addProperty(languageTag, "name", transalated.getI18nProperties().getProperty("name"));
						restContent.addProperty(languageTag, "filename", transalated.getI18nProperties().getProperty("filename"));
						restContent.addProperty(languageTag, "content", transalated.getI18nProperties().getProperty("content"));
						restContent.addProperty(languageTag, "teaser", transalated.getI18nProperties().getProperty("teaser"));
					}
				} else {
					for (String languageTag : info.getLanguageTags()) {

						Language language = info.getLanguageService().findByLanguageTag(languageTag);
						if (language == null) {
							throw new HttpStatusCodeErrorException(400, info.getI18n().get(info.getRoutingContext(), "error_language_not_found",
									languageTag));
						}

						// Add all i18n properties for the selected language to the response
						I18NProperties i18nProperties = info.getContentService().getI18NProperties(content, language);
						if (i18nProperties != null) {
							i18nProperties = info.getNeo4jTemplate().fetch(i18nProperties);
							for (String key : i18nProperties.getProperties().getPropertyKeys()) {
								restContent.addProperty(languageTag, key, i18nProperties.getProperty(key));
							}
						} else {
							log.error("Could not find any i18n properties for language {" + languageTag + "}. Skipping language.");
							continue;
						}
					}

				}

				if (content.getSchema() != null) {
					ObjectSchema schema = info.getNeo4jTemplate().fetch(content.getSchema());
					SchemaReference schemaReference = new SchemaReference();
					schemaReference.setSchemaName(schema.getName());
					schemaReference.setSchemaUuid(schema.getUuid());
					restContent.setSchema(schemaReference);
				}
				tx.success();
			}

			info.addObject(uuid, restContent);

		}

		if (depth < info.getMaxDepth()) {
			content.getTags().parallelStream().forEachOrdered(currentTag -> {

				try (Transaction tx = info.getGraphDb().beginTx()) {
					String currentUuid = currentTag.getUuid();
					Session session = info.getRoutingContext().session();
//					User user = info.getUserService().findUser(info.getRoutingContext());
					session.hasPermission(new CaiLunPermission(currentTag, PermissionType.READ).toString(), handler -> {
						if (handler.result()) {
							TagResponse currentRestTag = (TagResponse) info.getObject(currentUuid);
							if (currentRestTag == null) {
								Tag reloadedTag = info.getNeo4jTemplate().fetch(currentTag);
								currentRestTag = new TagResponse();
								// info.addTag(currentUuid, currentRestTag);
							TagTransformationTask subTask = new TagTransformationTask(reloadedTag, info, currentRestTag, depth + 1);
							tasks.add(subTask.fork());
						}
						restContent.getTags().add(currentRestTag);
					}
				}	);

					tx.success();
				}

			});
			// for (Tag currentTag : content.getTags()) {
			// boolean hasPerm = info.getSpringConfiguration().authService()
			// .hasPermission(info.getRoutingContext().session().getLoginID(), new CaiLunPermission(currentTag, PermissionType.READ));
			// if (hasPerm) {
			// restContent.getTags().add(
			// info.getTagService().transformToRest(info.getRoutingContext(), currentTag, info.getLanguageTags(), depth - 1));
			// }
			// }
		}

		return null;
	}

}
