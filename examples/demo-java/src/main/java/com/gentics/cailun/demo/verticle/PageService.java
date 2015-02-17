package com.gentics.cailun.demo.verticle;

import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.service.ContentService;

public interface PageService extends ContentService {

	public void setTeaser(Page page, Language language, String text);

	public void setTitle(Page page, Language language, String text);

}
