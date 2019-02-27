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
package com.syncleus.ferma.annotations;

import com.syncleus.ferma.*;
import com.tinkerpop.blueprints.Direction;

public abstract class Friend extends Person {
    public static final ClassInitializer<Friend> DEFAULT_INITIALIZER = new DefaultClassInitializer(Friend.class);
    
    
    @Incidence(label="knows", direction=Direction.OUT)
    public abstract Knows addKnows(Friend programmer, ClassInitializer<? extends Knows> type);

    @Incidence(label="knows", direction=Direction.IN)
    public abstract Knows addKnownBy(Friend programmer, ClassInitializer<? extends Knows> type);

    @Incidence(label="knows", direction=Direction.OUT)
    public abstract EdgeFrame addKnows(Friend programmer);

    @Incidence(label="knows", direction=Direction.IN)
    public abstract EdgeFrame addKnownBy(Friend programmer);
}
