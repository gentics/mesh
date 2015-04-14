package com.gentics.cailun.core.data.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.data.model.generic.GenericPropertyContainer;
import com.gentics.cailun.core.data.model.relationship.BasicRelationships;

@NodeEntity
public class Content extends GenericPropertyContainer {

	private static final long serialVersionUID = -4927498999985839348L;

	public static final String TEASER_KEY = "teaser";
	public static final String TITLE_KEY = "title";

	private long order = 0;

	public Content() {
	}

	public String getTeaser(Language language) {
		return getProperty(language, TEASER_KEY);
	}

	public String getTitle(Language language) {
		return getProperty(language, TITLE_KEY);
	}

	public static final String CONTENT_KEYWORD = "content";
	public static final String FILENAME_KEYWORD = "filename";
	public static final String NAME_KEYWORD = "name";

	public static final String TEASER_KEYWORD = null;

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_CONTENT, direction = Direction.INCOMING, elementClass = Tag.class)
	private Set<Tag> tags = new HashSet<>();

	public String getFilename(Language language) {
		return getProperty(language, FILENAME_KEYWORD);
	}

	public Set<Tag> getTags() {
		return tags;
	}

	public void addTag(Tag tag) {
		tags.add(tag);
	}

	// @RelatedToVia(type = BasicRelationships.LINKED, direction = Direction.OUTGOING, elementClass = Linked.class)
	// private Collection<Linked> links = new HashSet<>();

	public String getContent(Language language) {
		return getProperty(language, CONTENT_KEYWORD);
	}

	public long getOrder() {
		return order;
	}

	public void setOrder(long order) {
		this.order = order;
	}

}
