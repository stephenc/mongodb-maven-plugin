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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;

/**
 * Base class for all MongoDB mojos.
 */
public abstract class AbstractMongoMojo extends AbstractMojo {

    /**
     * The enclosing project.
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * Skip the execution.
     *
     * @parameter expression="${mongodb.skip}" default-value="false"
     */
    protected boolean skip;

    /**
     * The current build session instance.
     *
     * @parameter default-value="${session}"
     * @required
     * @readonly
     */
    protected MavenSession session;

    /**
     * This plugins descriptor.
     *
     * @parameter expression="${plugin}"
     * @readonly
     */
    private PluginDescriptor pluginDescriptor;

    /**
     * This mojo's execution.
     *
     * @parameter expression="${mojoExecution}"
     * @required
     * @readonly
     */
    protected MojoExecution mojoExecution;

    public PluginDescriptor getPluginDescriptor() {
        if ( pluginDescriptor == null )
        {
            pluginDescriptor = mojoExecution.getMojoDescriptor().getPluginDescriptor();
        }
        return pluginDescriptor;
    }
}
