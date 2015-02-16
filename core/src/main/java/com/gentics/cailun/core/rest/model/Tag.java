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
public class Tag<T extends Tag<T, F>, F extends File> extends CaiLunNode {

	private static final long serialVersionUID = 3547707185082166132L;

	@RelatedTo(type = BasicRelationships.HAS_FILE, elementClass = File.class, direction = Direction.OUTGOING)
	private Set<F> files = new HashSet<>();

	@RelatedTo(type = BasicRelationships.HAS_SUB_TAG, elementClass = Tag.class, direction = Direction.OUTGOING)
	private Set<Tag<T, F>> childTags = new HashSet<>();

	public Tag(Language language, String name) {
//		setName(language, name);
	}

	public void addFile(F node) {
		this.files.add(node);
	}

	@JsonIgnore
	public Set<F> getFiles() {
		return files;
	}

	public void addChildTag(Tag<T, F> tag) {
		childTags.add(tag);
	}

	public Set<Tag<T, F>> getChildTags() {
		return childTags;
	}

	public void setChildTags(Set<Tag<T, F>> childTags) {
		this.childTags = childTags;
	}

}
