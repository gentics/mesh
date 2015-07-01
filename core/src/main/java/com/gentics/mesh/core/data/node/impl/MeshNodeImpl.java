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
import com.gentics.mesh.core.data.MeshNodeFieldContainer;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.generic.GenericFieldContainerNode;
import com.gentics.mesh.core.data.impl.MeshNodeFieldContainerImpl;
import com.gentics.mesh.core.data.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.node.MeshNode;
import com.gentics.mesh.core.data.service.transformation.TransformationInfo;
import com.gentics.mesh.core.data.service.transformation.TransformationPool;
import com.gentics.mesh.core.data.service.transformation.node.MeshNodeTransformationTask;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.HTMLFieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SelectFieldSchema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.paging.PagingInfo;

public class MeshNodeImpl extends GenericFieldContainerNode implements MeshNode {

	public List<? extends Tag> getTags() {
		return out(HAS_TAG).has(TagImpl.class).toListExplicit(TagImpl.class);
	}

	@Override
	public List<? extends MeshNodeFieldContainer> getFieldContainers() {
		return out(HAS_FIELD_CONTAINER).has(MeshNodeFieldContainerImpl.class).toListExplicit(MeshNodeFieldContainerImpl.class);
	}

	@Override
	public MeshNodeFieldContainer getFieldContainer(Language language) {
		return getFieldContainer(language, MeshNodeFieldContainerImpl.class);
	}

	public MeshNodeFieldContainer getOrCreateFieldContainer(Language language) {
		return getOrCreateFieldContainer(language, MeshNodeFieldContainerImpl.class);
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

	public List<? extends MeshNode> getChildren() {
		return in(HAS_PARENT_NODE).has(MeshNodeImpl.class).toListExplicit(MeshNodeImpl.class);
	}

	public MeshNode getParentNode() {
		return out(HAS_PARENT_NODE).has(MeshNodeImpl.class).nextOrDefault(MeshNodeImpl.class, null);
	}

	public void setParentNode(MeshNode parent) {
		setLinkOut(parent.getImpl(), HAS_PARENT_NODE);
	}

	@Override
	public MeshNode create() {
		// TODO check whether the mesh node is in fact a container node.
		MeshNodeImpl node = getGraph().addFramedVertex(MeshNodeImpl.class);
		node.setParentNode(this);
		return node;
	}

	public NodeResponse transformToRest(TransformationInfo info) {

		NodeResponse restContent = new NodeResponse();
		MeshNodeTransformationTask task = new MeshNodeTransformationTask(this, info, restContent);
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

	public Page<MeshNode> getChildren(MeshAuthUser requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo) {

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
	public MeshNodeImpl getImpl() {
		return this;
	}

}
