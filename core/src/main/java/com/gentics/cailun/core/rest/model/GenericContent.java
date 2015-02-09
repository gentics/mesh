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
public class GenericContent extends GenericNode {

	private static final long serialVersionUID = 1100206059138098335L;

	@RelatedToVia(type = BasicRelationships.LINKED, direction = Direction.OUTGOING, elementClass = Linked.class)
	private Collection<Linked> links = new HashSet<>();

	@Indexed
	@NotNull
	protected String filename;

	public GenericContent(String name) {
		setName(name);
	}

	public void linkTo(GenericContent page) {
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

}
