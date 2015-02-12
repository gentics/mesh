package com.gentics.cailun.core.repository.project;

import com.gentics.cailun.core.repository.project.custom.PathActions;
import com.gentics.cailun.core.rest.model.File;

public interface ProjectFileRepository<T extends File> extends ProjectCaiLunNodeRepository<T>, PathActions<T> {

}
