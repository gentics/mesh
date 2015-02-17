package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
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
public class Tag<T extends Tag<T, F>, F extends File> extends CaiLunNode {

	private static final long serialVersionUID = 3547707185082166132L;

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_FILE, elementClass = File.class, direction = Direction.OUTGOING)
	private Set<File> files = new HashSet<>();

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_SUB_TAG, elementClass = Tag.class, direction = Direction.OUTGOING)
	private Set<Tag> tags = new HashSet<>();

	protected Tag() {

	}

	public void addFile(F node) {
		this.files.add(node);
	}

	public boolean removeFile(F file) {
		return this.files.remove(file);
	}

	@JsonIgnore
	public Set<File> getFiles() {
		return files;
	}

	public void addTag(Tag<T, F> tag) {
		tags.add(tag);
	}

	public boolean removeTag(Tag<T, F> tag) {
		return tags.remove(tag);
	}

	public Set<Tag> getTags() {
		return tags;
	}

	public void setTags(Set<Tag> childTags) {
		this.tags = childTags;
	}

	public boolean hasTag(Tag<T, F> tag) {
		for (Tag<T, F> childTag : tags) {
			if (tag.equals(childTag)) {
				return true;
			}
		}
		return false;
	}

}
