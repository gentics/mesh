package com.gentics.vertx.cailun.repository;

import java.util.Collection;
import java.util.HashSet;

import javax.validation.constraints.NotNull;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedToVia;

@NodeEntity
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class Page extends TaggableContent {
	
	private static final long serialVersionUID = 1100206059138098335L;

	// @Fetch
	@RelatedToVia(type = "LINKED", direction = Direction.OUTGOING, elementClass = Linked.class)
	private Collection<Linked> links = new HashSet<>();

	@Indexed
	@NotNull
	protected String name;

	@Indexed
	@NotNull
	protected String filename;

	protected String teaser;

	protected String title;

	protected String author;

	protected String content;

	public Page(String name) {
		this.name = name;
	}

	public void linkTo(Page page) {
		// TODO maybe extract information about link start and end to speedup rendering of page with links
		Linked link = new Linked(this, page);
		this.links.add(link);
	}

}
