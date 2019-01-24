/**
 * Copyright 2004 - 2016 Syncleus, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Part or all of this source file was forked from a third-party project, the details of which are listed below.
 *
 * Source Project: Totorom
 * Source URL: https://github.com/BrynCooke/totorom
 * Source License: Apache Public License v2.0
 * When: November, 20th 2014
 */
package com.syncleus.ferma;

import java.util.Comparator;

/**
 * Useful comparators when dealing with framed elements
 *
 */
public abstract class Comparators {
    
    private static final IdComparator ID_COMPARATOR = new IdComparator();
    private static final IdAsLongComparator ID_AS_LONG_COMPARATOR = new IdAsLongComparator();
    
    //this is a utility class, so we don't want it instantiated.
    private Comparators() {
    }

    /**
     * Creates a comparator that compares by ID.
     * 
     * @return The comparator.
     */
    public static Comparator<ElementFrame> id() {
        return ID_COMPARATOR;
    }

    /**
     * Compare by id parsed as a long (Useful for tinkergraph)
     * 
     * @return The comparator.
     */
    public static Comparator<ElementFrame> idAsLong() {
        return ID_AS_LONG_COMPARATOR;
    }

    /**
     * Compare by property. Note that no value may be null.
     * 
     * @param property
     *            The property to compare by.
     * @return The result of comparing the property.
     */
    public static Comparator<ElementFrame> property(final String property) {
        return new Comparator<ElementFrame>() {
            @Override
            public int compare(final ElementFrame t, final ElementFrame t1) {
                final Comparable c1 = t.getProperty(property);
                final Comparable c2 = t1.getProperty(property);

                return c1.compareTo(c2);
            }
        };
    }

    private static final class IdComparator implements Comparator<ElementFrame> {
        @Override
        public int compare(final ElementFrame t, final ElementFrame t1) {
            final Comparable c1 = t.getId();
            final Comparable c2 = t1.getId();

            return c1.compareTo(c2);
        }
    }
    
    private static final class IdAsLongComparator implements Comparator<ElementFrame> {
        @Override
        public int compare(final ElementFrame t, final ElementFrame t1) {
            final Long c1 = Long.parseLong((String) t.getId());
            final Long c2 = Long.parseLong((String) t1.getId());

            return c1.compareTo(c2);
        }
    }
}
