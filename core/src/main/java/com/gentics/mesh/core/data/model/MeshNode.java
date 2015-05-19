package com.gentics.mesh.core.data.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.mesh.core.data.model.generic.GenericPropertyContainer;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;

@NodeEntity
public class MeshNode extends GenericPropertyContainer {

	private static final long serialVersionUID = -4927498999985839348L;

	private static Label label = DynamicLabel.label(MeshNode.class.getSimpleName());

	@RelatedTo(type = BasicRelationships.HAS_TAG, direction = Direction.OUTGOING, elementClass = Tag.class)
	private Set<Tag> tags = new HashSet<>();

	@RelatedTo(type = BasicRelationships.HAS_PARENT_NODE, direction = Direction.INCOMING, elementClass = MeshNode.class)
	private Set<MeshNode> children = new HashSet<>();

	@RelatedTo(type = BasicRelationships.HAS_PARENT_NODE, direction = Direction.OUTGOING, elementClass = MeshNode.class)
	private MeshNode parentNode;

	public static Label getLabel() {

		/**
		 * TODO check whether the CallerSensitive annotation could be used to move this method into an abstract class?
		 * 
		 * @CallerSensitive public static Package getPackage(String name) { ClassLoader l = ClassLoader.getClassLoader(Reflection.getCallerClass());
		 */
		return label;
	}

	private long order = 0;

	public MeshNode() {
	}

	// @RelatedToVia(type = BasicRelationships.LINKED, direction = Direction.OUTGOING, elementClass = Linked.class)
	// private Collection<Linked> links = new HashSet<>();

	public long getOrder() {
		return order;
	}

	public void setOrder(long order) {
		this.order = order;
	}

	public void addTag(Tag tag) {
		tags.add(tag);
	}

	public boolean removeTag(Tag tag) {
		return tags.remove(tag);
	}

	public Set<Tag> getTags() {
		return tags;
	}

	public void setTags(Set<Tag> tags) {
		this.tags = tags;
	}

	public boolean hasTag(Tag tag) {
		for (Tag childTag : tags) {
			if (tag.equals(childTag)) {
				return true;
			}
		}
		return false;
	}

	public MeshNode getParentTag() {
		return parentNode;
	}

	public void setParent(MeshNode node) {
		this.parentNode = node;
	}

	public Set<MeshNode> getChildren() {
		return children;
	}

}
