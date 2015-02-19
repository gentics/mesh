package com.gentics.cailun.core.repository.generic;

import com.gentics.cailun.core.rest.model.generic.GenericFile;
import com.gentics.cailun.core.rest.model.generic.GenericTag;

public interface GenericTagRepository<T extends GenericTag<T, F>, F extends GenericFile> extends GenericNodeRepository<T> {

}
