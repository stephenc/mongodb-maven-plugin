/*
 * Copyright 2012 Stephen Connolly.
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

package com.github.stephenc.mongodb.maven;

import org.apache.commons.exec.ProcessDestroyer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

class ProcessObserver implements ProcessDestroyer, Iterable<Process> {

    private final ProcessDestroyer delegate;

    private final Set<Process> processes = Collections.synchronizedSet(new HashSet<Process>());

    ProcessObserver(ProcessDestroyer delegate) {
        this.delegate = delegate;
    }

    public boolean add(Process process) {
        processes.add(process);
        return delegate.add(process);
    }

    public boolean remove(Process process) {
        processes.remove(process);
        return delegate.remove(process);
    }

    public int size() {
        return delegate.size();
    }

    public Iterator<Process> iterator() {
        return new HashSet<Process>(processes).iterator();
    }
}
