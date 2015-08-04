package com.gentics.mesh.core.data.node.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_PARENT_NODE;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.service.I18NService.getI18n;
import static com.gentics.mesh.util.VerticleHelper.getPagingInfo;
import static com.gentics.mesh.util.VerticleHelper.getSelectedLanguageTags;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.GenericFieldContainerNode;
import com.gentics.mesh.core.data.impl.NodeFieldContainerImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.tag.TagFamilyTagGroup;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

public class NodeImpl extends GenericFieldContainerNode<NodeResponse> implements Node {

	private static final Logger log = LoggerFactory.getLogger(NodeImpl.class);

	@Override
	public String getType() {
		return Node.TYPE;
	}

	public List<? extends Tag> getTags() {
		return out(HAS_TAG).has(TagImpl.class).toListExplicit(TagImpl.class);
	}

	@Override
	public List<? extends NodeFieldContainer> getFieldContainers() {
		return out(HAS_FIELD_CONTAINER).has(NodeFieldContainerImpl.class).toListExplicit(NodeFieldContainerImpl.class);
	}

	@Override
	public NodeFieldContainer getFieldContainer(Language language) {
		return getFieldContainer(language, NodeFieldContainerImpl.class);
	}

	public NodeFieldContainer getOrCreateFieldContainer(Language language) {
		return getOrCreateFieldContainer(language, NodeFieldContainerImpl.class);
	}

	@Override
	public void addTag(Tag tag) {
		linkOut(tag.getImpl(), HAS_TAG);
	}

	@Override
	public void removeTag(Tag tag) {
		unlinkOut(tag.getImpl(), HAS_TAG);
	}

	@Override
	public void createLink(Node to) {
		// TODO maybe extract information about link start and end to speedup rendering of page with links
		// Linked link = new Linked(this, page);
		// this.links.add(link);
	}

	public void setSchemaContainer(SchemaContainer schema) {
		setLinkOut(schema.getImpl(), HAS_SCHEMA_CONTAINER);
	}

	public SchemaContainer getSchemaContainer() {
		return out(HAS_SCHEMA_CONTAINER).has(SchemaContainerImpl.class).nextOrDefaultExplicit(SchemaContainerImpl.class, null);
	}

	@Override
	public Schema getSchema() throws IOException {
		return getSchemaContainer().getSchema();
	}

	public List<? extends Node> getChildren() {
		return in(HAS_PARENT_NODE).has(NodeImpl.class).toListExplicit(NodeImpl.class);
	}

	public Node getParentNode() {
		return out(HAS_PARENT_NODE).has(NodeImpl.class).nextOrDefaultExplicit(NodeImpl.class, null);
	}

	public void setParentNode(Node parent) {
		setLinkOut(parent.getImpl(), HAS_PARENT_NODE);
	}

	@Override
	public Project getProject() {
		return out(ASSIGNED_TO_PROJECT).has(ProjectImpl.class).nextOrDefaultExplicit(ProjectImpl.class, null);
	}

	@Override
	public void setProject(Project project) {
		setLinkOut(project.getImpl(), ASSIGNED_TO_PROJECT);
	}

	@Override
	public Node create(User creator, SchemaContainer schemaContainer, Project project) {
		Node node = BootstrapInitializer.getBoot().nodeRoot().create(creator, schemaContainer, project);
		node.setParentNode(this);
		return node;
	}

	private String getLanguageInfo(List<String> languageTags) {
		Iterator<String> it = languageTags.iterator();

		String langInfo = "[";
		while (it.hasNext()) {
			langInfo += it.next();
			if (it.hasNext()) {
				langInfo += ",";
			}
		}
		langInfo += "]";
		return langInfo;
	}

	@Override
	public Node transformToRest(RoutingContext rc, Handler<AsyncResult<NodeResponse>> handler) {

		NodeResponse restNode = new NodeResponse();
		fillRest(restNode, rc);

		SchemaContainer container = getSchemaContainer();
		if (container == null) {
			throw new HttpStatusCodeErrorException(BAD_REQUEST, "The schema container for node {" + getUuid() + "} could not be found.");
		}

		try {
			Schema schema = container.getSchema();
			if (schema == null) {
				throw new HttpStatusCodeErrorException(BAD_REQUEST, "The schema for node {" + getUuid() + "} could not be found.");
			}
			/* Load the schema information */
			if (getSchemaContainer() != null) {
				SchemaReference schemaReference = new SchemaReference();
				schemaReference.setName(getSchema().getName());
				schemaReference.setUuid(getSchemaContainer().getUuid());
				restNode.setSchema(schemaReference);
			}

			if (getParentNode() != null) {
				restNode.setParentNodeUuid(getParentNode().getUuid());
			}
			/* Load the children */
			if (getSchema().isFolder()) {
				// //TODO handle uuid
				// //TODO handle expand
				List<String> children = new ArrayList<>();
				// //TODO check permissions
				for (Node child : getChildren()) {
					children.add(child.getUuid());
				}
				restNode.setContainer(true);
				restNode.setChildren(children);
			}

			NodeFieldContainer fieldContainer = null;
			Language foundLanguage = null;
			List<String> languageTags = getSelectedLanguageTags(rc);
			for (String languageTag : languageTags) {
				Language language = MeshRootImpl.getInstance().getLanguageRoot().findByLanguageTag(languageTag);
				if (language == null) {
					throw new HttpStatusCodeErrorException(BAD_REQUEST, getI18n().get(rc, "error_language_not_found", languageTag));
				}
				fieldContainer = getFieldContainer(language);
				// We found a container for one of the languages
				if (fieldContainer != null) {
					foundLanguage = language;
					break;
				}
			}

			restNode.setAvailableLanguages(getAvailableLanguageNames());

			if (fieldContainer == null) {
				String langInfo = getLanguageInfo(languageTags);
				log.info("The fields for node {" + getUuid() + "} can't be populated since the node has no matching language for the languages {"
						+ langInfo + "}. Fields will be empty.");
				// throw new HttpStatusCodeErrorException(400, getI18n().get(rc, "node_no_language_found", langInfo));
			} else {
				restNode.setLanguage(foundLanguage.getLanguageTag());
				for (FieldSchema fieldEntry : schema.getFields()) {
					com.gentics.mesh.core.rest.node.field.Field restField = fieldContainer.getRestField(fieldEntry.getName(), fieldEntry);
					if (fieldEntry.isRequired() && restField == null) {
						/* TODO i18n */
						throw new HttpStatusCodeErrorException(
								BAD_REQUEST,
								"The field {"
										+ fieldEntry.getName()
										+ "} is a required field but it could not be found in the node. Please add the field using an update call or change the field schema and remove the required flag.");
					}
					if (restField == null) {
						log.info("Field for key {" + fieldEntry.getName() + "} could not be found. Ignoring the field");
					} else {
						restNode.getFields().put(fieldEntry.getName(), restField);
					}
				}
			}

			try {
				for (Tag tag : getTags(rc)) {
					TagFamily tagFamily = tag.getTagFamily();
					String tagFamilyName = tagFamily.getName();
					String tagFamilyUuid = tagFamily.getUuid();
					TagReference reference = tag.tansformToTagReference();
					TagFamilyTagGroup group = restNode.getTags().get(tagFamilyName);
					if (group == null) {
						group = new TagFamilyTagGroup();
						group.setUuid(tagFamilyUuid);
						restNode.getTags().put(tagFamilyName, group);
					}
					group.getItems().add(reference);
				}
			} catch (InvalidArgumentException e) {
				// TODO i18n
				throw new HttpStatusCodeErrorException(BAD_REQUEST, "Could not transform tags");
			}

			handler.handle(Future.succeededFuture(restNode));
		} catch (IOException e) {
			// TODO i18n
			throw new HttpStatusCodeErrorException(BAD_REQUEST, "The schema for node {" + getUuid() + "} could not loaded.", e);
		}
		return this;

	}

	@Override
	public List<String> getAvailableLanguageNames() {
		// TODO Auto-generated method stub
		// TODO set language and all languages
		return null;
	}

	@Override
	public void delete() {
		// TODO handle linked containers
		getElement().remove();
	}

	@Override
	public Page<? extends Node> getChildren(MeshAuthUser requestUser, List<String> languageTags, PagingInfo pagingInfo)
			throws InvalidArgumentException {
		// TODO add permissions
		VertexTraversal<?, ?, ?> traversal = in(HAS_PARENT_NODE).has(NodeImpl.class);
		VertexTraversal<?, ?, ?> countTraversal = in(HAS_PARENT_NODE).has(NodeImpl.class);
		return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, NodeImpl.class);
	}

	@Override
	public Page<? extends Tag> getTags(RoutingContext rc) throws InvalidArgumentException {
		// TODO add permissions
		VertexTraversal<?, ?, ?> traversal = out(HAS_TAG).has(TagImpl.class);
		VertexTraversal<?, ?, ?> countTraversal = out(HAS_TAG).has(TagImpl.class);
		return TraversalHelper.getPagedResult(traversal, countTraversal, getPagingInfo(rc), TagImpl.class);
	}

	@Override
	public NodeImpl getImpl() {
		return this;
	}

}
