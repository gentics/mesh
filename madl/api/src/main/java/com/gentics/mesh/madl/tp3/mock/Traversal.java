package com.gentics.mesh.madl.tp3.mock;

import java.io.Serializable;
import java.util.Iterator;

public interface Traversal<S, E> extends Iterator<E>, Serializable, Cloneable, AutoCloseable {

}
