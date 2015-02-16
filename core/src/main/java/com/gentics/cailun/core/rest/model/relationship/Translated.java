package com.gentics.cailun.core.rest.model.relationship;

import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;

import com.gentics.cailun.core.rest.model.AbstractPersistable;
import com.gentics.cailun.core.rest.model.CaiLunNode;
import com.gentics.cailun.core.rest.model.I18NValue;
import com.gentics.cailun.core.rest.model.Language;

@RelationshipEntity
public class Translated extends AbstractPersistable {

	private static final long serialVersionUID = -8955212917270622506L;

	// TODO maybe it is not a good thing to store the tag in here as well?
	/**
	 * RFC5646 specific language tag
	 */
	private String languageTag;

	@StartNode
	private CaiLunNode startNode;

	@Fetch
	@EndNode
	private I18NValue i18nValue;

	public Translated() {
	}

	public Translated(CaiLunNode startNode, I18NValue value, Language language) {
		this.startNode = startNode;
		this.i18nValue = value;
		this.languageTag = language.getLanguageTag();
	}

	public String getLanguageTag() {
		return languageTag;
	}

	public I18NValue getI18nValue() {
		return i18nValue;
	}

	public CaiLunNode getStartNode() {
		return startNode;
	}

}
