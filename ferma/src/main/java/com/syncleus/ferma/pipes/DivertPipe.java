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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.tinkerpop.pipes.Pipe;
import com.tinkerpop.pipes.PipeFunction;
import com.tinkerpop.pipes.sideeffect.SideEffectPipe;
import com.tinkerpop.pipes.util.AbstractMetaPipe;
import com.tinkerpop.pipes.util.FastNoSuchElementException;
import com.tinkerpop.pipes.util.PipeHelper;

public class DivertPipe<S, T> extends AbstractMetaPipe<S, S> {

    private final SideEffectPipe<S, T> pipeToCap;

    private final PipeFunction<T, ?> sideEffectFunction;

    public DivertPipe(final SideEffectPipe<S, T> pipeToCap, final PipeFunction<T, ?> sideEffectFunction) {
        this.pipeToCap = pipeToCap;
        this.sideEffectFunction = sideEffectFunction;
    }

    @Override
    public void setStarts(final Iterator<S> starts) {
        this.pipeToCap.setStarts(starts);
    }

    @Override
    protected S processNextStart() {
        if (this.pipeToCap instanceof SideEffectPipe.LazySideEffectPipe) {
            final S next = this.pipeToCap.next();
            sideEffectFunction.compute(this.pipeToCap.getSideEffect());
            return next;
        }
        else
            try {
                return this.pipeToCap.next();
            }
            catch (final NoSuchElementException e) {
                sideEffectFunction.compute(this.pipeToCap.getSideEffect());
                throw FastNoSuchElementException.instance();
            }
    }

    @Override
    public List getCurrentPath() {
        if (this.pathEnabled) {
            final List list = this.pipeToCap.getCurrentPath();
            list.add(this.currentEnd);
            return list;
        }
        else
            throw new RuntimeException(Pipe.NO_PATH_MESSAGE);
    }

    @Override
    public String toString() {
        return PipeHelper.makePipeString(this, this.pipeToCap);
    }

    @Override
    public List<Pipe> getPipes() {
        return Arrays.asList((Pipe) this.pipeToCap);
    }
}
