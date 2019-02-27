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
package com.syncleus.ferma;

public class Knows extends AbstractEdgeFrame {
    public static final ClassInitializer<Knows> DEFAULT_INITIALIZER = new DefaultClassInitializer(Knows.class);

    public void setYears(final int years) {
        setProperty("years", years);
    }

    public int getYears() {
        return getProperty("years");
    }
}
