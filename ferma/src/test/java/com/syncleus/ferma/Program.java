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

public class Program extends AbstractVertexFrame {
    static final ClassInitializer<Program> DEFAULT_INITIALIZER = new DefaultClassInitializer(Program.class);

    public String getName() {
        return getProperty("name");
    }

    public void setName(final String name) {
        setProperty("name", name);
    }

    public String getLang() {
        return getProperty("lang");
    }

    public void setLang(final String lang) {
        setProperty("lang", lang);
    }

}
