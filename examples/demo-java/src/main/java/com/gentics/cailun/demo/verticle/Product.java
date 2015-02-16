package com.gentics.cailun.demo.verticle;

import com.gentics.cailun.core.rest.model.Content;
import com.gentics.cailun.core.rest.model.Language;

public class Product extends Content {

	private static final long serialVersionUID = 8031434376966397117L;

	public Product(Language language, String name, String filename) {
		super(language, name, filename);
	}

}
