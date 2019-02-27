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
package com.syncleus.ferma.pipes;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.syncleus.ferma.Path;

import com.tinkerpop.pipes.AbstractPipe;
import com.tinkerpop.pipes.Pipe;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.transform.TransformPipe;

public class PathPipe<S> extends AbstractPipe<S, List> implements TransformPipe<S, List> {

    private final PipeFunction[] pathFunctions;

    public PathPipe(final PipeFunction... pathFunctions) {
        if (pathFunctions.length == 0)
            this.pathFunctions = null;
        else
            this.pathFunctions = pathFunctions;
    }

    @Override
    public void setStarts(final Iterator<S> starts) {
        super.setStarts(starts);
        this.enablePath(true);
    }

    @Override
    public List processNextStart() {
        if (this.starts instanceof Pipe) {
            this.starts.next();
            final List path = ((Pipe) this.starts).getCurrentPath();
            if (null == this.pathFunctions)
                return path;
            else {
                final List closedPath = new Path();
                int nextFunction = 0;
                for (final Object object : path) {
                    closedPath.add(this.pathFunctions[nextFunction].compute(object));
                    nextFunction = (nextFunction + 1) % this.pathFunctions.length;
                }
                return closedPath;
            }
        }
        else
            throw new NoSuchElementException("The start of this pipe was not a pipe");
    }

}
