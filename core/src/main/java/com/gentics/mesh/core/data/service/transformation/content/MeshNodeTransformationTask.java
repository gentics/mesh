package com.gentics.mesh.core.data.service.transformation.content;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.data.model.MeshNode;
import com.gentics.mesh.core.data.model.I18NProperties;
import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.data.model.auth.User;
import com.gentics.mesh.core.data.model.relationship.Translated;
import com.gentics.mesh.core.data.model.schema.ObjectSchema;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.data.service.transformation.tag.TagTraversalConsumer;
import com.gentics.mesh.core.rest.node.response.NodeResponse;
import com.gentics.mesh.core.rest.schema.response.SchemaReference;
import com.gentics.mesh.error.HttpStatusCodeErrorException;

public class MeshNodeTransformationTask extends RecursiveTask<Void> {

	private static final long serialVersionUID = -1480528776879617657L;

	private static final Logger log = LoggerFactory.getLogger(MeshNodeTransformationTask.class);

	private MeshNode node;
	private TransformationInfo info;
	private NodeResponse restNode;
	private int depth;

	public MeshNodeTransformationTask(MeshNode node, TransformationInfo info, NodeResponse restNode, int depth) {
		this.node = node;
		this.info = info;
		this.restNode = restNode;
		this.depth = depth;
	}

	public MeshNodeTransformationTask(MeshNode node, TransformationInfo info, NodeResponse restContent) {
		this(node, info, restContent, 0);
	}

	private void resolveLinks(MeshNode node) throws InterruptedException, ExecutionException {
		// TODO fix issues with generics - Maybe move the link replacer to a
		// spring service
		// TODO handle language
		//		@Autowired
		//		private LinkResolverFactoryImpl<LinkResolver> resolver;
		//		Language language = null;
		//		LinkReplacer replacer = new LinkReplacer(resolver);
		// content.setContent(language,
		// replacer.replace(content.getContent(language)));
	}

	@Override
	protected Void compute() {

		Set<ForkJoinTask<Void>> tasks = new HashSet<>();
		String uuid = node.getUuid();
		// Check whether the node has already been transformed by another task
		NodeResponse foundContent = (NodeResponse) info.getObjectReferences().get(uuid);
		if (foundContent == null) {
			try (Transaction tx = info.getGraphDb().beginTx()) {
				restNode.setPerms(info.getUserService().getPerms(info.getRoutingContext(), node));
				restNode.setUuid(node.getUuid());

				/* Load the schema information */
				if (node.getSchema() != null) {
					ObjectSchema schema = info.getNeo4jTemplate().fetch(node.getSchema());
					SchemaReference schemaReference = new SchemaReference();
					schemaReference.setSchemaName(schema.getName());
					schemaReference.setSchemaUuid(schema.getUuid());
					restNode.setSchema(schemaReference);
				}
				/* Load the creator information */
				User creator = node.getCreator();
				if (creator != null) {
					creator = info.getNeo4jTemplate().fetch(creator);
					restNode.setCreator(info.getUserService().transformToRest(creator));
				}

				/* Load the order */
				restNode.setOrder(node.getOrder());

				/* Load the i18n properties */
				boolean loadAllTags = info.getLanguageTags().size() == 0;
				if (loadAllTags) {
					for (Translated transalated : node.getI18nTranslations()) {
						String languageTag = transalated.getLanguageTag();
						// TODO handle schema
						I18NProperties properties = transalated.getI18nProperties();
						properties = info.getNeo4jTemplate().fetch(properties);
						restNode.addProperty(languageTag, "name", transalated.getI18nProperties().getProperty("name"));
						restNode.addProperty(languageTag, "filename", transalated.getI18nProperties().getProperty("filename"));
						restNode.addProperty(languageTag, "content", transalated.getI18nProperties().getProperty("content"));
						restNode.addProperty(languageTag, "teaser", transalated.getI18nProperties().getProperty("teaser"));
					}
				} else {
					for (String languageTag : info.getLanguageTags()) {
						Language language = info.getLanguageService().findByLanguageTag(languageTag);
						if (language == null) {
							throw new HttpStatusCodeErrorException(400, info.getI18n().get(info.getRoutingContext(), "error_language_not_found",
									languageTag));
						}

						// Add all i18n properties for the selected language to the response
						I18NProperties i18nProperties = info.getContentService().getI18NProperties(node, language);
						if (i18nProperties != null) {
							i18nProperties = info.getNeo4jTemplate().fetch(i18nProperties);
							for (String key : i18nProperties.getProperties().getPropertyKeys()) {
								restNode.addProperty(languageTag, key, i18nProperties.getProperty(key));
							}
						} else {
							log.error("Could not find any i18n properties for language {" + languageTag + "}. Skipping language.");
							continue;
						}
					}

				}

				tx.success();
			}

			/* Add the object to the list of object references */
			info.addObject(uuid, restNode);

		}

		if (depth < info.getMaxDepth()) {
			TagTraversalConsumer tagConsumer = new TagTraversalConsumer(info, depth, restNode, tasks);
			node.getTags().parallelStream().forEachOrdered(tagConsumer);
		}

		tasks.forEach(action -> action.join());

		return null;
	}

}
