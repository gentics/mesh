package com.gentics.madl.query;

/**
 * @deprecated Part of Tinkerpop 2 Blueprints, that needs to be abandoned in favor of Gremlin query.
 * 
 * A general interface for all enumerations that represent comparable operations.
 *
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Matthias Broecheler (me@matthiasb.com)
 */
@Deprecated
public interface Predicate {

    /**
     * If the underlying graph does not support the push-down predicate, then an in-memory evaluation can be done.
     *
     * @param first  the left hand side of the predicate
     * @param second the right hand side of the predicate
     * @return the truth of the predicate given the two arguments
     */
    public boolean evaluate(final Object first, final Object second);
}
