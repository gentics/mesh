package com.gentics.mesh.dagger.annotations;

import com.gentics.mesh.ElementType;

import dagger.MapKey;

@MapKey
public @interface ElementTypeKey {
	ElementType value();
}
