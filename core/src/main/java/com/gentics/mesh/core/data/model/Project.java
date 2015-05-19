package com.gentics.mesh.core.data.model;

import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;

/**
 * A project is the root element for a tag hierarchy.
 * 
 * @author johannes2
 *
 */
@NodeEntity
public class Project extends GenericNode {

	private static final long serialVersionUID = -3565883313897315008L;

	@Indexed(unique = true)
	protected String name;

	@SuppressWarnings("unused")
	private Project() {
	}

	public Project(String name) {
		this.name = name;
	}

	@RelatedTo(type = BasicRelationships.HAS_ROOT_TAG, direction = Direction.OUTGOING, elementClass = TagFamilyRoot.class)
	private Set<TagFamilyRoot> tagFamilies;

	@RelatedTo(type = BasicRelationships.HAS_ROOT_SCHEMA, direction = Direction.OUTGOING, elementClass = ObjectSchemaRoot.class)
	private ObjectSchemaRoot rootSchema;
	
	@RelatedTo(type= BasicRelationships.HAS_ROOT_NODE, direction = Direction.OUTGOING, elementClass = MeshNode.class)
	private MeshNode rootNode;

	
	public MeshNode getRootNode() {
		return rootNode;
	}
	
	public void setRootNode(MeshNode rootNode) {
		this.rootNode = rootNode;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
