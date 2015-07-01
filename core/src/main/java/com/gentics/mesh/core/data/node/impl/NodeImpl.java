package com.gentics.mesh.core.data.node.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_PARENT_NODE;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_TAG;

import java.io.IOException;
import java.util.List;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.generic.GenericFieldContainerNode;
import com.gentics.mesh.core.data.impl.NodeFieldContainerImpl;
import com.gentics.mesh.core.data.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.data.service.transformation.TransformationPool;
import com.gentics.mesh.core.data.service.transformation.node.NodeTransformationTask;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.paging.PagingInfo;

public class NodeImpl extends GenericFieldContainerNode implements Node {

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
		// addFramedEdge(HAS_TAG, tag);
		linkOut(tag.getImpl(), HAS_TAG);
	}

	@Override
	public void removeTag(Tag tag) {
		unlinkOut(tag.getImpl(), HAS_TAG);
	}

	public void setSchemaContainer(SchemaContainer schema) {
		setLinkOut(schema.getImpl(), HAS_SCHEMA_CONTAINER);
	}

	public SchemaContainer getSchemaContainer() {
		return out(HAS_SCHEMA_CONTAINER).has(SchemaContainerImpl.class).nextOrDefault(SchemaContainerImpl.class, null);
	}

	@Override
	public Schema getSchema() throws IOException {
		return getSchemaContainer().getSchema();
	}

	public List<? extends Node> getChildren() {
		return in(HAS_PARENT_NODE).has(NodeImpl.class).toListExplicit(NodeImpl.class);
	}

	public Node getParentNode() {
		return out(HAS_PARENT_NODE).has(NodeImpl.class).nextOrDefault(NodeImpl.class, null);
	}

	public void setParentNode(Node parent) {
		setLinkOut(parent.getImpl(), HAS_PARENT_NODE);
	}

	@Override
	public Node create() {
		// TODO check whether the mesh node is in fact a container node.
		NodeImpl node = getGraph().addFramedVertex(NodeImpl.class);
		node.setParentNode(this);
		return node;
	}

	public NodeResponse transformToRest(TransformationInfo info) {

		NodeResponse restContent = new NodeResponse();
		NodeTransformationTask task = new NodeTransformationTask(this, info, restContent);
		TransformationPool.getPool().invoke(task);
		return restContent;

	}

	

	public Page<Tag> getTags(MeshAuthUser requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo) {
		return null;
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

}
