package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.rest.model.relationship.BasicRelationships;

@NodeEntity
public class LocalizableCaiLunNode<T extends LocalizedNode> extends CaiLunNode {

	private static final long serialVersionUID = 440143172867274496L;

	@RelatedTo(type = BasicRelationships.HAS_LOCALISATION, elementClass = CaiLunNode.class, direction = Direction.OUTGOING)
	private Set<T> localisations = new HashSet<>();

	public void addLocalisation(T localisation) {
		this.localisations.add(localisation);
	}

	public Set<T> getLocalisations() {
		return localisations;
	}
	
//	public T getLocalisationForLanguage(String language) {
//		for(T localisation : localisations) {
////			if(localisation.get)
//		}
//	}
}
