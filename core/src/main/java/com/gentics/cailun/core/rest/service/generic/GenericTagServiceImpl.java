package com.gentics.cailun.core.rest.service.generic;

import com.gentics.cailun.core.rest.model.generic.GenericFile;
import com.gentics.cailun.core.rest.model.generic.GenericTag;

public class GenericTagServiceImpl<T extends GenericTag<T, F>, F extends GenericFile> extends GenericPropertyContainerServiceImpl<T> implements GenericTagService<T, F> {

}
