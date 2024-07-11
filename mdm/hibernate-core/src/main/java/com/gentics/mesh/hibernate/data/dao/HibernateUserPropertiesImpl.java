package com.gentics.mesh.hibernate.data.dao;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.core.data.generic.UserProperties;
import com.gentics.mesh.core.data.user.CreatorTracking;
import com.gentics.mesh.core.data.user.EditorTracking;
import com.gentics.mesh.core.data.user.User;

/**
 * A helper component implementation for getting user properties from an entity element.
 * 
 * @author plyhun
 *
 */
@Singleton
public class HibernateUserPropertiesImpl implements UserProperties {

	@Inject
	public HibernateUserPropertiesImpl() {
	}

	@Override
	public User getCreator(BaseElement element) {
		if (element instanceof CreatorTracking) {
			return ((CreatorTracking) element).getCreator();
		}
		return null;
	}

	@Override
	public User getEditor(BaseElement element) {
		if (element instanceof EditorTracking) {
			return ((EditorTracking) element).getEditor();
		}
		return null;
	}

	@Override
	public void setCreator(BaseElement element, User user) {
		if (element instanceof CreatorTracking) {
			((CreatorTracking) element).setCreator(user);
		}		
	}

	@Override
	public void setEditor(BaseElement element, User user) {
		if (element instanceof EditorTracking) {
			((EditorTracking) element).setEditor(user);
		}
	}	
}
