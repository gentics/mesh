package com.gentics.mesh.core.data.model;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;

@NodeEntity
public class LanguageRoot extends AbstractPersistable {

	private static final long serialVersionUID = 5160771115848405859L;

	@RelatedTo(type = BasicRelationships.HAS_LANGUAGE, direction = Direction.OUTGOING, elementClass = Language.class)
	private Set<Language> languages = new HashSet<>();

	@Indexed(unique = true)
	private String unique = LanguageRoot.class.getSimpleName();

	public LanguageRoot() {
	}

	public Set<Language> getLanguages() {
		return languages;
	}
}
