package com.gentics.cailun.demo.verticle;

import com.gentics.cailun.core.rest.model.File;

import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.model.LocalizedTag;
import com.gentics.cailun.core.rest.model.Tag;

public class Category extends Tag<LocalizedCategory, Product, File> {

}

class LocalizedCategory extends LocalizedTag {

	public LocalizedCategory(Language language, String name) {
		super(language, name);
	}

}