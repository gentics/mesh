package com.gentics.cailun.core.rest.model;

import java.util.Collection;
import java.util.HashSet;

import lombok.NoArgsConstructor;

import org.antlr.v4.runtime.misc.NotNull;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedToVia;

@NodeEntity
@NoArgsConstructor
public class Page extends GenericNode {

	private static final long serialVersionUID = 1100206059138098335L;

	// @Fetch
	@RelatedToVia(type = "LINKED", direction = Direction.OUTGOING, elementClass = Linked.class)
	private Collection<Linked> links = new HashSet<>();

	@Indexed
	@NotNull
	protected String filename;

	protected String teaser;

	protected String title;

	protected String author;

	protected String content;

	public Page(String name) {
		setName(name);
	}

	public void linkTo(Page page) {
		// TODO maybe extract information about link start and end to speedup rendering of page with links
		Linked link = new Linked(this, page);
		this.links.add(link);
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getTeaser() {
		return teaser;
	}

	public void setTeaser(String teaser) {
		this.teaser = teaser;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}
}
