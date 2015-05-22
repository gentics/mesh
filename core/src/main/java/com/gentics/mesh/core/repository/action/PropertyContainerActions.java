package com.gentics.mesh.core.repository.action;

import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.data.model.generic.GenericPropertyContainer;

public interface PropertyContainerActions<T extends GenericPropertyContainer> {

	public void setDisplayName(T node, Language language, String name);

	public void setContent(T node, Language language, String text);

	public void setName(T node, Language language, String filename);

	public String getName(T node, Language language);

	public String getContent(T node, Language language);

	public String getTeaser(T node, Language language);

	public String getTitle(T node, Language language);

	public String getDisplayName(T node, Language language);

}
