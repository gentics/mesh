package com.gentics.cailun.core.data.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.data.model.generic.GenericPropertyContainer;
import com.gentics.cailun.core.data.model.relationship.BasicRelationships;

@NodeEntity
public class Content extends GenericPropertyContainer {

	private static final long serialVersionUID = -4927498999985839348L;

	private static Label label = DynamicLabel.label(Content.class.getSimpleName());

	public static Label getLabel() {

		/**
		 * TODO check whether the CallerSensitive annotation could be used to move this method into an abstract class?
		 * 
		 * @CallerSensitive public static Package getPackage(String name) { ClassLoader l = ClassLoader.getClassLoader(Reflection.getCallerClass());
		 */
		return label;
	}

	private long order = 0;

	public Content() {
	}

	public static final String TEASER_KEYWORD = null;

	@RelatedTo(type = BasicRelationships.HAS_CONTENT, direction = Direction.INCOMING, elementClass = Tag.class)
	private Set<Tag> tags = new HashSet<>();

	public Set<Tag> getTags() {
		return tags;
	}

	public void addTag(Tag tag) {
		tags.add(tag);
	}

	// @RelatedToVia(type = BasicRelationships.LINKED, direction = Direction.OUTGOING, elementClass = Linked.class)
	// private Collection<Linked> links = new HashSet<>();

	public long getOrder() {
		return order;
	}

	public void setOrder(long order) {
		this.order = order;
	}

}
