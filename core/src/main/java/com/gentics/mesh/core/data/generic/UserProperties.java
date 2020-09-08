package com.gentics.mesh.core.data.generic;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.user.HibUser;

public interface UserProperties {

	HibUser getCreator(HibBaseElement element);

	HibUser getEditor(HibBaseElement element);

	void setCreator(HibBaseElement element, HibUser user);

	void setEditor(HibBaseElement element, HibUser user);

}
