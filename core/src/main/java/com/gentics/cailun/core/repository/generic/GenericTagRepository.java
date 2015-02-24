package com.gentics.cailun.core.repository.generic;

import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.data.model.generic.GenericTag;

public interface GenericTagRepository<T extends GenericTag<T, F>, F extends GenericFile> extends GenericNodeRepository<T> {

}
