package com.gentics.mesh.core.data.service.transformation;

import io.vertx.ext.web.RoutingContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.data.model.tinkerpop.MeshShiroUser;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.data.service.LanguageService;
import com.gentics.mesh.core.rest.common.response.AbstractRestModel;

public class TransformationInfo {

	//	private RoutingContext routingContext;
	//
	//	private UserService userService;
	//	private LanguageService languageService;
	//	private TagService tagService;
	//	private MeshSpringConfiguration springConfiguration;
	//	private MeshNodeService contentService;
	//	private I18NService i18nService;
	//
	private MeshShiroUser requestUser;
	//	// Configuration
	//	private boolean includeTags;
	//	private boolean includeContents;
	//	private boolean includeChildTags;
	//	private int maxDepth;
	//	private List<String> languageTags = new ArrayList<>();
	//
	// Storage for object references
	private Map<String, AbstractRestModel> objectReferences = new HashMap<>();

	public TransformationInfo(MeshShiroUser requestUser) {
		this.requestUser = requestUser;
	}

	public Map<String, AbstractRestModel> getObjectReferences() {
		return objectReferences;
	}

	public MeshShiroUser getRequestUser() {
		return requestUser;
	}

	//	public int getMaxDepth() {
	//		return maxDepth;
	//	}
	//
	public AbstractRestModel getObject(String uuid) {
		return objectReferences.get(uuid);
	}

	//
	public void addObject(String uuid, AbstractRestModel object) {
		objectReferences.put(uuid, object);
	}
	//
	//	public List<String> getLanguageTags() {
	//		return languageTags;
	//	}
	//
	//	public void setLanguageTags(List<String> languageTags) {
	//		this.languageTags = languageTags;
	//	}
	//	
	//	public LanguageService getLanguageService() {
	//		return languageService;
	//	}
	//
	//	public void setLanguageService(LanguageService languageService) {
	//		this.languageService = languageService;
	//	}
	//
	//	public MeshSpringConfiguration getSpringConfiguration() {
	//		return springConfiguration;
	//	}
	//
	//	public void setSpringConfiguration(MeshSpringConfiguration springConfiguration) {
	//		this.springConfiguration = springConfiguration;
	//	}
	//
	//	public UserService getUserService() {
	//		return userService;
	//	}
	//
	//	public void setUserService(UserService userService) {
	//		this.userService = userService;
	//	}
	//
	//	public MeshNodeService getContentService() {
	//		return contentService;
	//	}
	//
	//	public void setContentService(MeshNodeService contentService) {
	//		this.contentService = contentService;
	//	}
	//
	//	public RoutingContext getRoutingContext() {
	//		return routingContext;
	//	}
	//
	//	public void setRoutingContext(RoutingContext routingContext) {
	//		this.routingContext = routingContext;
	//	}
	//
	//	public void setTagService(TagService tagService) {
	//		this.tagService = tagService;
	//	}
	//
	//	public TagService getTagService() {
	//		return tagService;
	//	}
	//
	//	public I18NService getI18n() {
	//		return i18nService;
	//	}
	//
	//	public void setI18nService(I18NService i18nService) {
	//		this.i18nService = i18nService;
	//	}
	//
	//	public boolean isIncludeTags() {
	//		return includeTags;
	//	}
	//
	//	public void setIncludeTags(boolean includeTags) {
	//		this.includeTags = includeTags;
	//	}
	//
	//	public boolean isIncludeChildTags() {
	//		return includeChildTags;
	//	}
	//
	//	public void setIncludeChildTags(boolean includeChildTags) {
	//		this.includeChildTags = includeChildTags;
	//	}
	//
	//	public boolean isIncludeContents() {
	//		return includeContents;
	//	}
	//
	//	public void setIncludeContents(boolean includeContents) {
	//		this.includeContents = includeContents;
	//	}

	public RoutingContext getRoutingContext() {
		// TODO Auto-generated method stub
		return null;
	}

	public I18NService  getI18n() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<String> getLanguageTags() {
		// TODO Auto-generated method stub
		return null;
	}

	public LanguageService getLanguageService() {
		// TODO Auto-generated method stub
		return null;
	}

}
