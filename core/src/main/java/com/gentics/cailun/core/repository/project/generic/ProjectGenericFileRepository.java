package com.gentics.cailun.core.repository.project.generic;

import com.gentics.cailun.core.repository.project.custom.PathActions;
import com.gentics.cailun.core.rest.model.generic.GenericFile;

public interface ProjectGenericFileRepository<T extends GenericFile> extends ProjectGenericNodeRepository<T>, PathActions<T> {

}
