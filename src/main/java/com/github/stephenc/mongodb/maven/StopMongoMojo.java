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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Stops a MongoDB instance previously started with {@code start}.
 *
 * @goal stop
 * @phase post-integration-test
 */
public class StopMongoMojo extends AbstractMongoMojo {

    /**
     * The port mongodb was started on.
     *
     * @parameter expression="${mongodb.port}" default-value="27017"
     */
    private int port;

    public void execute() throws MojoExecutionException, MojoFailureException {
        ProcessObserver processObserver;
        Map pluginContext = session.getPluginContext(getPluginDescriptor(), project);
        String key = ProcessObserver.class.getName() + ":" + Integer.toString(port);
        processObserver = (ProcessObserver) pluginContext.get(key);
        if (processObserver == null) {
            return;
        }

        Set<Process> remaining = new HashSet<Process>();
        for (Process p : processObserver) {
            getLog().info("Stopping MongoDb instance " + p + "...");
            remaining.add(p);
            p.destroy();
        }
        while (!remaining.isEmpty()) {
            Iterator<Process> iterator = remaining.iterator();
            while (iterator.hasNext()) {
                Process p = iterator.next();
                try {
                    p.waitFor();
                    iterator.remove();
                    getLog().info("MongoDb instance " + p + " stopped.");
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
        if (processObserver.size() == 0) {
            pluginContext.remove(key);
        }
    }
}
