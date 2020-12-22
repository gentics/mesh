package com.gentics.mesh.dagger.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.dagger.module.DaoTransformableModule;

import dagger.MapKey;

/**
 * Dagger map key annotation that us used in the {@link DaoTransformableModule} to manage binding for map entries.
 */
@MapKey
@Retention(RetentionPolicy.RUNTIME)
public @interface ElementTypeKey {

	/**
	 * Value reference fot keys.
	 * 
	 * @return
	 */
	ElementType value();
}
