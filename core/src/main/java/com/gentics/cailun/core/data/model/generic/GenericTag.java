package com.gentics.cailun.core.data.model.generic;

import java.util.HashSet;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.data.model.relationship.BasicRelationships;

/**
 * A tag is a basic container for other generic nodes. It can be used as a folder that holds contents in the form of various pages.
 * 
 * @author johannes2
 *
 */
@NodeEntity
public class GenericTag<T extends GenericTag<T, F>, F extends GenericFile> extends GenericPropertyContainer {

	private static final long serialVersionUID = 3547707185082166132L;

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_FILE, direction = Direction.OUTGOING)
	private Set<F> files = new HashSet<>();

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_SUB_TAG, direction = Direction.OUTGOING)
	private Set<T> childTags = new HashSet<>();

	protected GenericTag() {

	}

	/**
	 * Add a file to the set of files for this tag.
	 * 
	 * @param file
	 */
	public void addFile(F file) {
		this.files.add(file);
	}

	public boolean removeFile(F file) {
		return this.files.remove(file);
	}

	@JsonIgnore
	public Set<F> getFiles() {
		return files;
	}

	public void addTag(T tag) {
		childTags.add(tag);
	}

	public boolean removeTag(GenericTag<T, F> tag) {
		return childTags.remove(tag);
	}

	public Set<T> getTags() {
		return childTags;
	}

	public void setTags(Set<T> childTags) {
		this.childTags = childTags;
	}

	public boolean hasTag(GenericTag<T, F> tag) {
		for (GenericTag<T, F> childTag : childTags) {
			if (tag.equals(childTag)) {
				return true;
			}
		}
		return false;
	}

}
