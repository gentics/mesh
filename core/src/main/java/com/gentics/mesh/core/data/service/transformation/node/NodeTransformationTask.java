package com.gentics.mesh.core.data.service.transformation.node;

import static com.gentics.mesh.core.data.service.I18NService.getI18n;
import static com.gentics.mesh.core.data.service.LanguageService.getLanguageService;
import io.vertx.core.impl.ConcurrentHashSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshUser;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.data.service.transformation.tag.TagTraversalConsumer;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.util.BlueprintTransaction;

public class NodeTransformationTask extends RecursiveTask<Void> {

	private static final long serialVersionUID = -1480528776879617657L;

	private static final Logger log = LoggerFactory.getLogger(NodeTransformationTask.class);

	private Node node;
	private TransformationInfo info;
	private NodeResponse restNode;
	private int depth;

	public NodeTransformationTask(Node node, TransformationInfo info, NodeResponse restNode, int depth) {
		this.node = node;
		this.info = info;
		this.restNode = restNode;
	}

	public NodeTransformationTask(Node node, TransformationInfo info, NodeResponse restContent) {
		this(node, info, restContent, 0);
	}

	private void resolveLinks(Node node) throws InterruptedException, ExecutionException {
		// TODO fix issues with generics - Maybe move the link replacer to a
		// spring service
		// TODO handle language
		// @Autowired
		// private LinkResolverFactory<LinkResolver> resolver;
		// Language language = null;
		// LinkReplacer replacer = new LinkReplacer(resolver);
		// content.setContent(language,
		// replacer.replace(content.getContent(language)));
	}

	@Override
	protected Void compute() {

		MeshAuthUser requestUser = info.getRequestUser();
		Set<ForkJoinTask<Void>> tasks = new ConcurrentHashSet<>();
		try (BlueprintTransaction tx = new BlueprintTransaction(MeshSpringConfiguration.getMeshSpringConfiguration()
				.getFramedThreadedTransactionalGraph())) {

			String uuid = node.getUuid();
			// Check whether the node has already been transformed by another task
			NodeResponse foundContent = (NodeResponse) info.getObjectReferences().get(uuid);
			if (foundContent == null) {
				restNode.setPermissions(requestUser.getPermissionNames(node));
				restNode.setUuid(node.getUuid());

				Schema schema = node.getSchema();
				if (schema == null) {
					throw new HttpStatusCodeErrorException(400, "The schema for node {" + node.getUuid() + "} could not be found.");
				}
				/* Load the schema information */
				if (node.getSchemaContainer() != null) {
					SchemaReference schemaReference = new SchemaReference();
					schemaReference.setName(node.getSchema().getName());
					schemaReference.setUuid(node.getSchemaContainer().getUuid());
					restNode.setSchema(schemaReference);
				}
				/* Load the creator information */
				MeshUser creator = node.getCreator();
				if (creator != null) {
					restNode.setCreator(creator.transformToRest());
				}

				/* Load the children */
				if (node.getSchema().isContainer()) {
					// //TODO handle uuid
					// //TODO handle expand
					List<String> children = new ArrayList<>();
					// //TODO check permissions
					for (Node child : node.getChildren()) {
						children.add(child.getUuid());
					}
					restNode.setContainer(true);
					restNode.setChildren(children);
				}

				NodeFieldContainer fieldContainer = null;
				for (String languageTag : info.getLanguageTags()) {
					Language language = getLanguageService().findByLanguageTag(languageTag);
					if (language == null) {
						throw new HttpStatusCodeErrorException(400, getI18n().get(info.getRoutingContext(), "error_language_not_found", languageTag));
					}
					fieldContainer = node.getFieldContainer(language);
					// We found a container for one of the languages
					if (fieldContainer != null) {
						break;
					}
				}

				if (fieldContainer == null) {
					throw new HttpStatusCodeErrorException(400, "Could not find any field for one of the languagetags that were specified.");
				}

				for (Entry<String, ? extends FieldSchema> fieldEntry : schema.getFields().entrySet()) {
					Field restField = fieldContainer.getRestField(fieldEntry.getKey(), fieldEntry.getValue());
					restNode.getFields().put(fieldEntry.getKey(), restField);
				}

				/* Add the object to the list of object references */
				info.addObject(uuid, restNode);

			}

			if (depth < 2) {
				TagTraversalConsumer tagConsumer = new TagTraversalConsumer(info, depth, restNode, tasks);
				// TODO replace this with iterator handling
				node.getTags().spliterator().forEachRemaining(tagConsumer);
			}

			tasks.forEach(action -> action.join());
		} catch (IOException e) {
			// TODO handle error - we need to tell our caller
			e.printStackTrace();
			return null;
		}

		return null;
	}
}
