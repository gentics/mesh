package com.gentics.mesh.core.data;

import java.util.Map;

/**
 * Map which provides a way to lookup persistence classes via the interface of those classes.
 */
public interface PersistenceClassMap extends Map<Class<?>, Class<?>>  {

}
