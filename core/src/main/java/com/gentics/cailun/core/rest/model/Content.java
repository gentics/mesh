package com.gentics.cailun.core.rest.model;

import java.util.Set;

import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
public class Content extends File {

	private static final long serialVersionUID = 1100206059138098335L;

	public static final String CONTENT_KEYWORD = "content";
	public static final String FILENAME_KEYWORD = "filename";
	public static final String NAME_KEYWORD = "name";

	// @RelatedToVia(type = BasicRelationships.LINKED, direction = Direction.OUTGOING, elementClass = Linked.class)
	// private Collection<Linked> links = new HashSet<>();

	@Fetch
	protected Set<I18NValue> filenames;

	@Indexed
	@Fetch
	protected Set<I18NValue> contents;

	// public void linkTo(LocalizedContent page) {
	// // TODO maybe extract information about link start and end to speedup rendering of page with links
	// Linked link = new Linked(this, page);
	// this.links.add(link);
	// }

	@SuppressWarnings("unused")
	private Content() {
		
	}

	

	public String getContent(Language language) {
		return getI18NProperty(language, CONTENT_KEYWORD);
	}

	public void setContent(Language language, String replace) {
		
	}
	

}
