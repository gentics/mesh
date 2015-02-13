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
public class Tag extends LocalizableCaiLunNode<LocalizedTag> {

	private static final long serialVersionUID = 3547707185082166132L;

	@RelatedTo(type = BasicRelationships.HAS_FILE, elementClass = File.class, direction = Direction.OUTGOING)
	private Set<File> files = new HashSet<>();
	
	private Set<Content> contents = new HashSet<>();

	@RelatedTo(type = BasicRelationships.HAS_SUB_TAG, elementClass = Tag.class, direction = Direction.OUTGOING)
	private Set<Tag> childTags = new HashSet<>();

	
	public Set<Content> getContents() {
		return contents;
	}
	
	
	public void addFile(File node) {
		this.files.add(node);
	}

	@JsonIgnore
	public Set<File> getFiles() {
		return files;
	}

	public void addChildTag(Tag tag) {
		childTags.add(tag);
	}

	public Set<Tag> getChildTags() {
		return childTags;
	}

	public void setChildTags(Set<Tag> childTags) {
		this.childTags = childTags;
	}

	public Tag addLocalizedSubTag(Language language, String name) {
		Tag tag = new Tag();
		tag.addLocalizedTag(language, name);
		this.childTags.add(tag);
		return tag;
	}

	public LocalizedTag addLocalizedTag(Language language, String name) {
		LocalizedTag tag = new LocalizedTag(name);
		tag.setLanguage(language);
		return tag;

	}

}
