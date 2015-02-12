package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import lombok.Data;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.rest.model.relationship.BasicRelationships;

@Data
@NodeEntity
public class TaggableNode extends CaiLunNode {

	@JsonIgnore
	@Fetch
	@RelatedTo(type = BasicRelationships.TAGGED, direction = Direction.OUTGOING, elementClass = LocalizedTag.class)
	private Set<LocalizedTag> childTags = new HashSet<>();

	public void setChildTags(Set<LocalizedTag> childTags) {
		this.childTags = childTags;
	}

	public LocalizedTag tag(String name) {
		LocalizedTag tag = new LocalizedTag(name);
		this.childTags.add(tag);
		return tag;
	}

	public LocalizedTag tag(LocalizedTag tag) {
		this.childTags.add(tag);
		return tag;
	}

	public boolean unTag(String name) {
		return this.childTags.remove(new LocalizedTag(name));
	}

	public boolean unTag(Tag tag) {
		return this.childTags.remove(tag);
	}

	/**
	 * Check whether the node has the given child tag.
	 * 
	 * @param name
	 * @return true, when the node has the given tag. Otherwise false.
	 */
	public boolean hasTag(Tag tag) {
		return this.childTags.contains(tag);
	}

	@JsonIgnore
	public Set<LocalizedTag> getChildTags() {
		return this.childTags;
	}

}
