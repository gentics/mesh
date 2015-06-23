package com.gentics.mesh.core.data.service.transformation.node;

import static com.gentics.mesh.core.data.service.I18NService.getI18n;
import static com.gentics.mesh.core.data.service.LanguageService.getLanguageService;
import io.vertx.core.impl.ConcurrentHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.data.model.tinkerpop.I18NProperties;
import com.gentics.mesh.core.data.model.tinkerpop.Language;
import com.gentics.mesh.core.data.model.tinkerpop.MeshAuthUser;
import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.data.model.tinkerpop.MeshUser;
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

		MeshAuthUser requestUser = info.getRequestUser();
		Set<ForkJoinTask<Void>> tasks = new ConcurrentHashSet<>();
		String uuid = node.getUuid();
		// Check whether the node has already been transformed by another task
		NodeResponse foundContent = (NodeResponse) info.getObjectReferences().get(uuid);
		if (foundContent == null) {
			restNode.setPermissions(requestUser.getPermissionNames(node));
			restNode.setUuid(node.getUuid());

			/* Load the schema information */
			if (node.getSchema() != null) {
				SchemaReference schemaReference = new SchemaReference();
				schemaReference.setSchemaName(node.getSchema().getName());
				schemaReference.setUuid(node.getSchema().getUuid());
				restNode.setSchema(schemaReference);
			}
			/* Load the creator information */
			MeshUser creator = node.getCreator();
			if (creator != null) {
				restNode.setCreator(creator.transformToRest());
			}

			/* Load the order */
			//	restNode.setOrder(node.getOrder());

			/* Load the children */
			if (node.getSchema().isNestingAllowed()) {
				//TODO handle uuid
				//TODO handle expand
				List<String> children = new ArrayList<>();
				//TODO check permissions
				for (MeshNode child : node.getChildren()) {
					children.add(child.getUuid());
				}
				restNode.setContainer(true);
				restNode.setChildren(children);
			}

			/* Load the i18n properties */
			for (String languageTag : info.getLanguageTags()) {
				Language language = getLanguageService().findByLanguageTag(languageTag);
				if (language == null) {
					throw new HttpStatusCodeErrorException(400, getI18n().get(info.getRoutingContext(), "error_language_not_found", languageTag));
				}

				// Add all i18n properties for the selected language to the response
				I18NProperties i18nProperties = node.getI18nProperties(language);
				if (i18nProperties != null) {
					for (String key : i18nProperties.getProperties().keySet()) {
						restNode.addProperty(key, i18nProperties.getProperty(key));
					}
				} else {
					log.error("Could not find any i18n properties for language {" + languageTag + "}. Skipping language.");
					continue;
				}
			}

			/* Add the object to the list of object references */
			info.addObject(uuid, restNode);

		}

		if (depth < 2) {
			TagTraversalConsumer tagConsumer = new TagTraversalConsumer(info, depth, restNode, tasks);
			//TODO replace this with iterator handling
			node.getTags().spliterator().forEachRemaining(tagConsumer);
		}

		tasks.forEach(action -> action.join());

		return null;
	}

}
