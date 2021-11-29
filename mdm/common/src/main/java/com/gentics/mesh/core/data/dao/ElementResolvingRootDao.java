package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Stack;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * An extension to {@link RootDao} with the possibilities to reference persisted leaf entities upon the root entity.
 * 
 * @author plyhun
 *
 * @param <R>
 * @param <L>
 */
public interface ElementResolvingRootDao<R extends HibCoreElement<? extends RestModel>, L extends HibCoreElement<? extends RestModel>> extends RootDao<R, L> {

	/**
	 * Get the final type of the leaf persistence entity of the dao.
	 * 
	 * @return
	 */
	Class<? extends L> getPersistenceClass(R root);

	@Override
	default HibBaseElement resolveToElement(R root, Stack<String> stack) {
		if (log.isDebugEnabled()) {
			log.debug("Resolving for {" + getPersistenceClass(root).getSimpleName() + "}.");
			if (stack.isEmpty()) {
				log.debug("Stack: is empty");
			} else {
				log.debug("Stack: " + stack.peek());
			}
		}
		if (stack.isEmpty()) {
			return root;
		} else {
			String uuid = stack.pop();
			if (stack.isEmpty()) {
				return findByUuid(root, uuid);
			} else {
				throw error(BAD_REQUEST, "Can't resolve remaining segments. Next segment would be: " + stack.peek());
			}
		}
	}
}
