package com.gentics.mesh.core.data.node.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_PARENT_NODE;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.service.I18NService.getI18n;
import static com.gentics.mesh.util.RoutingContextHelper.getSelectedLanguageTags;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
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
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.GenericFieldContainerNode;
import com.gentics.mesh.core.data.impl.NodeFieldContainerImpl;
import com.gentics.mesh.core.data.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.node.ContainerNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

public class NodeImpl extends GenericFieldContainerNode<NodeResponse> implements Node {

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

	public void setParentNode(ContainerNode parent) {
		setLinkOut(parent.getImpl(), HAS_PARENT_NODE);
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
			throw new HttpStatusCodeErrorException(400, "The schema container for node {" + getUuid() + "} could not be found.");
		}

		try {
			Schema schema = container.getSchema();
			if (schema == null) {
				throw new HttpStatusCodeErrorException(400, "The schema for node {" + getUuid() + "} could not be found.");
			}
			/* Load the schema information */
			if (getSchemaContainer() != null) {
				SchemaReference schemaReference = new SchemaReference();
				schemaReference.setName(getSchema().getName());
				schemaReference.setUuid(getSchemaContainer().getUuid());
				restNode.setSchema(schemaReference);
			}

			/* Load the children */
			if (getSchema().isContainer()) {
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
			//TODO set language and all languages

			NodeFieldContainer fieldContainer = null;
			List<String> languageTags = getSelectedLanguageTags(rc);
			for (String languageTag : languageTags) {
				Language language = MeshRootImpl.getInstance().getLanguageRoot().findByLanguageTag(languageTag);
				if (language == null) {
					throw new HttpStatusCodeErrorException(400, getI18n().get(rc, "error_language_not_found", languageTag));
				}
				fieldContainer = getFieldContainer(language);
				// We found a container for one of the languages
				if (fieldContainer != null) {
					break;
				}
			}

			if (fieldContainer == null) {
				// "Could not find any field for one of the languagetags that were specified."
				String langInfo = getLanguageInfo(languageTags);
				throw new HttpStatusCodeErrorException(400, getI18n().get(rc, "node_no_language_found", langInfo));
			}

			for (FieldSchema fieldEntry : schema.getFields()) {
				com.gentics.mesh.core.rest.node.field.Field restField = fieldContainer.getRestField(fieldEntry.getName(), fieldEntry);
				restNode.getFields().put(fieldEntry.getName(), restField);
			}

			handler.handle(Future.succeededFuture(restNode));
		} catch (IOException e) {
			throw new HttpStatusCodeErrorException(400, "The schema for node {" + getUuid() + "} could not loaded.", e);
		}
		return this;

	}

	public Page<? extends Tag> getTags(MeshAuthUser requestUser, String projectName, PagingInfo pagingInfo) throws InvalidArgumentException {

		//TODO filter permissions
		VertexTraversal<?, ?, ?> traversal = out(HAS_TAG).has(TagImpl.class);
		VertexTraversal<?, ?, ?> countTraversal = out(HAS_TAG).has(TagImpl.class);
		Page<? extends Tag> items = TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, TagImpl.class);
		return items;
		// public Page<Tag> findTags(String userUuid, String projectName, MeshNode node, List<String> languageTags, PagingInfo pagingInfo) {
		// // String langFilter = getLanguageFilter("l");
		// // if (languageTags == null || languageTags.isEmpty()) {
		// // langFilter = "";
		// // } else {
		// // langFilter += " AND ";
		// // }
		// //
		// // String baseQuery = PERMISSION_PATTERN_ON_TAG;
		// // baseQuery += TAG_PROJECT_PATTERN;
		// // baseQuery += "MATCH (node:MeshNode)-[:HAS_TAG]->(tag)-[l:HAS_I18N_PROPERTIES]-(sp:I18NProperties) ";
		// // baseQuery += "WHERE " + langFilter + USER_PERMISSION_FILTER + " AND " + PROJECT_FILTER;
		// //
		// // String query = baseQuery + " WITH sp, tag ORDER BY sp.`properties-name` desc RETURN DISTINCT tag as n";
		// // String countQuery = baseQuery + " RETURN count(DISTINCT tag) as count";
		// //
		// // Map<String, Object> parameters = new HashMap<>();
		// // parameters.put("languageTags", languageTags);
		// // parameters.put("projectName", projectName);
		// // parameters.put("userUuid", userUuid);
		// // parameters.put("node", node);
		// // return queryService.query(query, countQuery, parameters, pagingInfo, Tag.class);
		// return null;
		// }
	}

	public Page<Node> getChildren(MeshAuthUser requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo) {

		// if (languageTags == null || languageTags.size() == 0) {
		// return findChildren(userUuid, projectName, parentNode, pr);
		// } else {
		// return findChildren(userUuid, projectName, parentNode, languageTags, pr);
		// }

		// Page<MeshNode> findChildren(String userUuid, String projectName, MeshNode parentNode, List<String> languageTags, Pageable pr) {
		// @Query(value = MATCH_PERMISSION_ON_NODE + MATCH_NODE_OF_PROJECT + " MATCH (parentNode)-[:HAS_PARENT_NODE]->(node) " + "WHERE "
		// + FILTER_USER_PERM_AND_PROJECT + " AND id(parentNode) = {2} " + "WITH p, node " + ORDER_BY_NAME_DESC + "RETURN DISTINCT childNode",
		//
		// countQuery = MATCH_PERMISSION_ON_NODE + MATCH_NODE_OF_PROJECT + " MATCH (parentNode)-[:HAS_PARENT_NODE]->(node) " + "WHERE "
		// + FILTER_USER_PERM_AND_PROJECT + " AND id(parentNode) = {2} " + "RETURN count(DISTINCT node)"
		//
		// )
		// }

		// Page<MeshNode> findChildren(String userUuid, String projectName, MeshNode parentNode, Pageable pr) {
		// @Query(value = MATCH_PERMISSION_ON_NODE + MATCH_NODE_OF_PROJECT + " MATCH (parentNode)<-[:HAS_PARENT_NODE]-(node) " + "WHERE "
		// + FILTER_USER_PERM_AND_PROJECT + " AND id(parentNode) = {2} " + "WITH p, node " + "ORDER by p.`properties-name` desc "
		// + "RETURN DISTINCT node",
		//
		// countQuery = MATCH_PERMISSION_ON_NODE + MATCH_NODE_OF_PROJECT + " MATCH (parentNode)<-[:HAS_PARENT_NODE]-(node) " + "WHERE "
		// + FILTER_USER_PERM_AND_PROJECT + " AND id(parentNode) = {2} " + "RETURN count(DISTINCT node)")
		// }
		return null;

	}

	@Override
	public void delete() {
		getElement().remove();
	}

	@Override
	public NodeImpl getImpl() {
		return this;
	}

	@Override
	public Page<? extends Node> getChildren(MeshAuthUser requestUser, List<String> languageTags, PagingInfo pagingInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<? extends Tag> getTags(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {
		// TODO Auto-generated method stub
		return null;
	}

}
