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
package com.syncleus.ferma.traversals;

public interface SplitTraversal<T> {

    /**
     * Add an ExhaustMergePipe to the end of the pipeline. The one-step previous
     * MetaPipe in the pipeline's pipes are used as the internal pipes. The
     * pipes' emitted objects are merged where the first pipe's objects are
     * exhausted, then the second, etc.
     *
     * @return the extended Pipeline
     */
    T exhaustMerge();

    /**
     * Add a FairMergePipe to the end of the Pipeline. The one-step previous
     * MetaPipe in the pipeline's pipes are used as the internal pipes. The
     * pipes' emitted objects are merged in a round robin fashion.
     *
     * @return the extended Pipeline
     */
    T fairMerge();

}
