package com.gentics.cailun.demo.verticle;

import com.gentics.cailun.core.rest.model.Content;
import com.gentics.cailun.core.rest.model.Language;

public class Product extends Content {

	public Product(Language language, String name, String filename) {
		super(language, name, filename);
	}

}
