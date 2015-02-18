package com.gentics.cailun.core.repository.generic;

import com.gentics.cailun.core.rest.model.generic.GenericFile;
import com.gentics.cailun.core.rest.model.generic.GenericTag;

public interface GlobalGenericTagRepository<T extends GenericTag<T, F>, F extends GenericFile> extends GlobalGenericNodeRepository<T> {

}
