package com.gentics.cailun.core.data.service.generic;

import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.data.model.generic.GenericTag;

public class GenericTagServiceImpl<T extends GenericTag<T, F>, F extends GenericFile> extends GenericPropertyContainerServiceImpl<T> implements GenericTagService<T, F> {

}
