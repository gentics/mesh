package com.gentics.cailun.core.repository;

import com.gentics.cailun.core.rest.model.File;
import com.gentics.cailun.core.rest.model.Tag;

public interface GlobalTagRepository<T extends Tag<T, F>, F extends File> extends GlobalCaiLunNodeRepository<T> {

}
