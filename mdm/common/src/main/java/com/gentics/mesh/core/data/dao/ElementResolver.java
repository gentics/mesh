package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Stack;
import java.util.function.BiFunction;

import com.gentics.mesh.core.data.HibBaseElement;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * A persisted leaf entity resolver upon the root entity.
 * 
 * @author plyhun
 *
 * @param <B> base element type
 * @param <E> looked entity type
 */
public interface ElementResolver<B extends HibBaseElement, E extends HibBaseElement> {

	public static final Logger log = LoggerFactory.getLogger(ElementResolver.class);

	/**
	 * Get the function that provides the looked element for the given root and uuid.
	 * 
	 * @return
	 */
	BiFunction<B, String, E> getFinder();

	/**
	 * Resolve the given stack to the element.
	 * 
	 * @param permissionRoot the permission provider for the element class
	 * @param root base element for the resolved elements
	 * @param stack path parts
	 * @return
	 */
	default HibBaseElement resolveToElement(HibBaseElement permissionRoot, B root, Stack<String> stack) {
		if (log.isDebugEnabled()) {
			log.debug("Resolving for {" + root.getClass().getSimpleName() + "}.");
			if (stack.isEmpty()) {
				log.debug("Stack: is empty");
			} else {
				log.debug("Stack: " + stack.peek());
			}
		}
		if (stack.isEmpty()) {
			return permissionRoot;
		} else {
			String uuid = stack.pop();
			if (stack.isEmpty()) {
				return getFinder().apply(root, uuid);
			} else {
				throw error(BAD_REQUEST, "Can't resolve remaining segments. Next segment would be: " + stack.peek());
			}
		}
	}
}
