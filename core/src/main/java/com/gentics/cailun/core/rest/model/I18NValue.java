package com.gentics.cailun.core.rest.model;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.cailun.core.rest.model.relationship.BasicRelationships;

@NodeEntity
public class I18NValue extends AbstractPersistable {

	private static final long serialVersionUID = 7462760705716392395L;

	@Indexed
	@Fetch
	private String value;

	@Fetch
	private String key;

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_LANGUAGE, direction = Direction.OUTGOING, elementClass = Language.class)
	private Language language;

	@SuppressWarnings("unused")
	private I18NValue() {

	}

	public I18NValue(Language lang, String key, String value) {
		this.language = lang;
		this.key = key;
		this.value = value;
	}

	public Language getLanguage() {
		return language;
	}

	public void setLanguage(Language language) {
		this.language = language;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
