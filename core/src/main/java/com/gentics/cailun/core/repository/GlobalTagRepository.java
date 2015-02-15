package com.gentics.cailun.core.repository;

import com.gentics.cailun.core.rest.model.Content;
import com.gentics.cailun.core.rest.model.File;
import com.gentics.cailun.core.rest.model.LocalizedTag;
import com.gentics.cailun.core.rest.model.Tag;

public interface GlobalTagRepository<T extends Tag<LT, C, F>, LT extends LocalizedTag, C extends Content, F extends File> extends
		GlobalLocalizableCaiLunNodeRepository<T, LT> {

}
