package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import lombok.NoArgsConstructor;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.rest.model.relationship.BasicRelationships;

/**
 * A tag is a basic container for other generic nodes. It can be used as a folder that holds contents in the form of various pages.
 * 
 * @author johannes2
 *
 */
@NodeEntity
@NoArgsConstructor
public class LocalizedTag extends CaiLunNode {

	private static final long serialVersionUID = 3547707185082166132L;

	@RelatedTo(type = BasicRelationships.HAS_LOCALIZED_TAGS, elementClass = Tag.class, direction = Direction.OUTGOING)
	private Set<Tag> localizedTags = new HashSet<>();

	@RelatedTo(type = BasicRelationships.HAS_FILE, elementClass = File.class, direction = Direction.OUTGOING)
	private Set<File> files = new HashSet<>();

	public void addTag(Tag tag) {
		localizedTags.add(tag);
	}

	public Set<Tag> getTags() {
		return localizedTags;
	}

	/**
	 * Adds a content in the form of a generic node to this tag.
	 * 
	 * @param node
	 */
	public void addContent(File node) {
		this.files.add(node);
	}

	@JsonIgnore
	public Set<File> getFiles() {
		return files;
	}

}
