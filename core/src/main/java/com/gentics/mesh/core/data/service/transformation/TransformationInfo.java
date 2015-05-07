package com.gentics.mesh.core.data.service.transformation;

import io.vertx.ext.apex.RoutingContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import com.gentics.mesh.core.data.service.ContentService;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.data.service.LanguageService;
import com.gentics.mesh.core.data.service.TagService;
import com.gentics.mesh.core.data.service.UserService;
import com.gentics.mesh.core.rest.common.response.AbstractRestModel;
import com.gentics.mesh.etc.MeshSpringConfiguration;

public class TransformationInfo {

	private RoutingContext routingContext;

	private UserService userService;
	private LanguageService languageService;
	private GraphDatabaseService graphDb;
	private TagService tagService;
	private Neo4jTemplate neo4jTemplate;
	private MeshSpringConfiguration springConfiguration;
	private ContentService contentService;
	private I18NService i18nService;

	// Configuration
	private boolean includeTags;
	private boolean includeContents;
	private boolean includeChildTags;
	private int maxDepth;
	private List<String> languageTags = new ArrayList<>();

	// Storage for object references
	private Map<String, AbstractRestModel> objectReferences = new HashMap<>();

	public TransformationInfo(RoutingContext rc) {
		this.routingContext = rc;
	}

	public Map<String, AbstractRestModel> getObjectReferences() {
		return objectReferences;
	}

	public int getMaxDepth() {
		return maxDepth;
	}

	public AbstractRestModel getObject(String uuid) {
		return objectReferences.get(uuid);
	}

	public void addObject(String uuid, AbstractRestModel object) {
		objectReferences.put(uuid, object);
	}

	public List<String> getLanguageTags() {
		return languageTags;
	}

	public void setLanguageTags(List<String> languageTags) {
		this.languageTags = languageTags;
	}
	
	public LanguageService getLanguageService() {
		return languageService;
	}

	public void setLanguageService(LanguageService languageService) {
		this.languageService = languageService;
	}

	public MeshSpringConfiguration getSpringConfiguration() {
		return springConfiguration;
	}

	public void setSpringConfiguration(MeshSpringConfiguration springConfiguration) {
		this.springConfiguration = springConfiguration;
	}

	public UserService getUserService() {
		return userService;
	}

	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	public GraphDatabaseService getGraphDb() {
		return graphDb;
	}

	public void setGraphDb(GraphDatabaseService graphDb) {
		this.graphDb = graphDb;
	}

	public Neo4jTemplate getNeo4jTemplate() {
		return neo4jTemplate;
	}

	public void setNeo4jTemplate(Neo4jTemplate neo4jTemplate) {
		this.neo4jTemplate = neo4jTemplate;
	}

	public ContentService getContentService() {
		return contentService;
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	public RoutingContext getRoutingContext() {
		return routingContext;
	}

	public void setRoutingContext(RoutingContext routingContext) {
		this.routingContext = routingContext;
	}

	public void setTagService(TagService tagService) {
		this.tagService = tagService;
	}

	public TagService getTagService() {
		return tagService;
	}

	public I18NService getI18n() {
		return i18nService;
	}

	public void setI18nService(I18NService i18nService) {
		this.i18nService = i18nService;
	}

	public boolean isIncludeTags() {
		return includeTags;
	}

	public void setIncludeTags(boolean includeTags) {
		this.includeTags = includeTags;
	}

	public boolean isIncludeChildTags() {
		return includeChildTags;
	}

	public void setIncludeChildTags(boolean includeChildTags) {
		this.includeChildTags = includeChildTags;
	}

	public boolean isIncludeContents() {
		return includeContents;
	}

	public void setIncludeContents(boolean includeContents) {
		this.includeContents = includeContents;
	}

}
