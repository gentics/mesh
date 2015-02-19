package com.gentics.cailun.core.rest.service.generic;

import com.gentics.cailun.core.rest.model.generic.GenericFile;
import com.gentics.cailun.core.rest.model.generic.GenericTag;

public interface GenericTagService<T extends GenericTag<T, F>, F extends GenericFile> extends GenericPropertyContainerService<T> {

}
