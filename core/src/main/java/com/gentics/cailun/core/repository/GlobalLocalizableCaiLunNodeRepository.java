package com.gentics.cailun.core.repository;

import com.gentics.cailun.core.rest.model.LocalizableCaiLunNode;
import com.gentics.cailun.core.rest.model.LocalizedNode;


public interface GlobalLocalizableCaiLunNodeRepository<T extends LocalizableCaiLunNode<E>, E extends LocalizedNode> extends GlobalCaiLunNodeRepository<T> {

}
