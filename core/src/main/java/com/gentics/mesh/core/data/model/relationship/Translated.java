package com.gentics.mesh.core.data.model.relationship;

import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;

import com.gentics.mesh.core.data.model.I18NProperties;
import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.generic.GenericNode;

@RelationshipEntity(type=BasicRelationships.HAS_I18N_PROPERTIES)
public class Translated extends AbstractPersistable {

	private static final long serialVersionUID = -8955212917270622506L;

	// TODO maybe it is not a good thing to store the tag in here as well?
	/**
	 * RFC5646 specific language tag
	 */
	private String languageTag;

	@StartNode
	private GenericNode startNode;

	@EndNode
	private I18NProperties i18nProperties;

	public Translated() {
	}

	public Translated(GenericNode startNode, I18NProperties i18nProperties, Language language) {
		this.startNode = startNode;
		this.i18nProperties = i18nProperties;
		this.languageTag = language.getLanguageTag();
	}

	public String getLanguageTag() {
		return languageTag;
	}

	public I18NProperties getI18nProperties() {
		return i18nProperties;
	}

	public GenericNode getStartNode() {
		return startNode;
	}

}
