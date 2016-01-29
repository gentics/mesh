package com.gentics.mesh.core.data.schema.handler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.schema.SchemaChange;

/**
 * Central registry that will store all {@link SchemaChange} specific handler implementations.
 */
@Component
public class ChangeHandlerRegistry {

	private Map<String, ChangeHandler> handlers = Collections.synchronizedMap(new HashMap<>());

	/**
	 * Register the handler using the operation.
	 * 
	 * @param operation
	 * @param abstractChangeHandler
	 */
	public void register(String operation, ChangeHandler changeHandler) {
		handlers.put(operation, changeHandler);
	}

	/**
	 * Unregister the handler using the operation key.
	 * 
	 * @param operation
	 */
	public void unregister(String operation) {
		handlers.remove(operation);
	}

	/**
	 * Return the change handler for the given operation.
	 * 
	 * @return Found change handler or null if no handler could be found
	 */
	public ChangeHandler getHandler(String operation) {
		return handlers.get(operation);
	}

}
