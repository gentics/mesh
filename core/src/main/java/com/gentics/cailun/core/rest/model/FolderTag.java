package com.gentics.cailun.core.rest.model;

public class FolderTag extends Tag<LocalizedFolderTag, Content, File> {

	public FolderTag(Language language, String name) {
		super(language, name);
	}

}

class LocalizedFolderTag extends LocalizedTag {

	public LocalizedFolderTag(Language language, String name) {
		super(language, name);
	}

}