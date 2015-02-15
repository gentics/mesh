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
	private Set<T> localizations = new HashSet<>();

	public void addLocalization(T localisation) {
		this.localizations.add(localisation);
	}

	public Set<T> getLocalizations() {
		return localizations;
	}

	/**
	 * Returns the localization for the given language.
	 * 
	 * @param language
	 * @return found localization, null when no localization could be found for the given language
	 */
	public T getLocalisation(Language language) {

		for (T localisation : localizations) {
			if (localisation.language == language) {
				return localisation;
			}
		}
		return null;

	}
}
