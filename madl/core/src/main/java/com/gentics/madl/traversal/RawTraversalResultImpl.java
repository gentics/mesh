package com.gentics.madl.traversal;

import java.util.Iterator;

import com.gentics.mesh.madl.frame.ElementFrame;
import com.gentics.mesh.madl.tp3.mock.Element;
import com.syncleus.ferma.typeresolvers.TypeResolver;

public class RawTraversalResultImpl<T extends Element> extends AbstractRawTraversal<T> {

	private Iterable<T> it;
	private TypeResolver typeResolver;

	public RawTraversalResultImpl(Iterable<T> it, TypeResolver typeResolver) {
		this.it = it;
		this.typeResolver = typeResolver;
	}

	public RawTraversalResultImpl(Iterator<T> it, TypeResolver typeResolver) {
		this.it = () -> it;
		this.typeResolver = typeResolver;
	}

	@Override
	public Iterator<T> iterator() {
		return it.iterator();
	}

	@Override
	public <T extends ElementFrame> WrappedTraversalResult<T> frameExplicit(Class<T> classOfT) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends ElementFrame> WrappedTraversalResult<T> frameDynamic(Class<T> classOfT) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R> R next(Class<R> clazzOfR) {
		// TODO Auto-generated method stub
		return null;
	}

//	@Override
//	public <R> R next(Class<R> clazzOfR) {
//		T element = it.iterator().next();
//		return WrapperFactory.frameElement(element, clazzOfR);
//	}
//
//	@Override
//	public <W extends WrappedElement> WrappedTraversalResult<W> frameExplicit(Class<W> classOfE) {
//		Iterator<W> wrappedIt = stream().map(e -> {
//			return WrapperFactory.frameElement(e, classOfE);
//		}).iterator();
//		return new WrappedTraversalResultImpl<W>(wrappedIt);
//	}
//
//	@Override
//	public <W extends WrappedElement> WrappedTraversalResult<W> frameDynamic(Class<W> classOfE) {
//		@SuppressWarnings("unchecked")
//		Iterator<W> wrappedIt = (Iterator<W>) stream().map(e -> {
//			// TODO handle edges
//			//OrientVertex oV = (OrientVertex) e;
//			//String clazzName = oV.getRawElement().getSchemaType().get().getName();
//			String clazzName = null;
////			try {
//				// Class<? extends WrappedElement> clazz = Class.forName(clazzName);
//				//Class<?> clazz = Class.forName(clazzName);
//				Class<?> clazz = typeResolver.resolve(clazzName);
//				return WrapperFactory.frameElement(e, clazz);
////			} catch (ClassNotFoundException e2) {
////				throw new RuntimeException("Could not find class for name {" + clazzName + "}", e2);
////			}
//		}).iterator();
//		return new WrappedTraversalResultImpl<W>(wrappedIt);
//	}
}
