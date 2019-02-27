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

import java.util.*;

public class GenericClassInitializer<C> implements ClassInitializer<C> {
    private final Class<C> type;
    private final Map<String, Object> properties;

    public GenericClassInitializer(final Class<C> type, final Map<String, Object> properties) {
        this.type = type;
        this.properties = Collections.unmodifiableMap(new HashMap<>(properties));
    }

    @Override
    public Class<C> getInitializationType() {
        return this.type;
    }
    
    protected Map<String, Object> getProperties() {
        return properties;
    }
    
    @Override
    public void initalize(final C frame) {
        if( !(frame instanceof ElementFrame) )
            throw new IllegalArgumentException("frame was not an instance of an ElementFrame");
        final ElementFrame elementFrame = (ElementFrame) frame;
        for(final Map.Entry<String, Object> property : this.properties.entrySet() )
            elementFrame.setProperty(property.getKey(), property.getValue());
    }
}
