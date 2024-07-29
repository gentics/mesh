package com.gentics.mesh.hibernate.data.dao;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.generic.UserProperties;
import com.gentics.mesh.core.data.user.HibCreatorTracking;
import com.gentics.mesh.core.data.user.HibEditorTracking;
import com.gentics.mesh.core.data.user.HibUser;

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
	public HibUser getCreator(HibBaseElement element) {
		if (element instanceof HibCreatorTracking) {
			return ((HibCreatorTracking) element).getCreator();
		}
		return null;
	}

	@Override
	public HibUser getEditor(HibBaseElement element) {
		if (element instanceof HibEditorTracking) {
			return ((HibEditorTracking) element).getEditor();
		}
		return null;
	}

	@Override
	public void setCreator(HibBaseElement element, HibUser user) {
		if (element instanceof HibCreatorTracking) {
			((HibCreatorTracking) element).setCreator(user);
		}		
	}

	@Override
	public void setEditor(HibBaseElement element, HibUser user) {
		if (element instanceof HibEditorTracking) {
			((HibEditorTracking) element).setEditor(user);
		}
	}	
}
