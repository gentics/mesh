package com.gentics.cailun.core.data.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.data.model.generic.GenericPropertyContainer;
import com.gentics.cailun.core.data.model.relationship.BasicRelationships;

/**
 * A tag is the main structural element. It allows the creation of tag hierarchies. Tags have important limitations. A tag can and must only have one parent.
 * 
 * @author johannes2
 *
 */
@NodeEntity
public class Tag extends GenericPropertyContainer {

	private static final long serialVersionUID = 7645315435657775862L;

	private static Label label = DynamicLabel.label(Tag.class.getSimpleName());

	public Tag() {
		this.schemaName = "tag";
	}

	public static Label getLabel() {

		/**
		 * TODO check whether the CallerSensitive annotation could be used to move this method into an abstract class?
		 * 
		 * @CallerSensitive public static Package getPackage(String name) { ClassLoader l = ClassLoader.getClassLoader(Reflection.getCallerClass());
		 */
		return label;
	}

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_CONTENT, direction = Direction.OUTGOING, elementClass = Content.class)
	private Set<Content> contents = new HashSet<>();

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_SUB_TAG, direction = Direction.OUTGOING, elementClass = Tag.class)
	private Set<Tag> childTags = new HashSet<>();

	public void addTag(Tag tag) {
		childTags.add(tag);
	}

	public boolean removeTag(Tag tag) {
		return childTags.remove(tag);
	}

	public Set<Tag> getTags() {
		return childTags;
	}

	public void setTags(Set<Tag> childTags) {
		this.childTags = childTags;
	}

	public boolean hasTag(Tag tag) {
		for (Tag childTag : childTags) {
			if (tag.equals(childTag)) {
				return true;
			}
		}
		return false;
	}

	public void addContent(Content content) {
		this.contents.add(content);
	}

	public Set<Content> getContents() {
		return contents;
	}

	public boolean removeContent(Content content) {
		return contents.remove(content);
	}

}
