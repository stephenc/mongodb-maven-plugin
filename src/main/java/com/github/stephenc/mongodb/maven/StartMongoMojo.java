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

import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.exec.ShutdownHookProcessDestroyer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.Os;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Starts a MongoDB instance.
 *
 * @goal start
 * @phase pre-integration-test
 */
public class StartMongoMojo extends AbstractMongoMojo {

    /**
     * The root of the MongoDB Installation. If not specified it is assumed that MongoDB's executable is on the Path.
     *
     * @parameter expression="${mongodb.installation}"
     */
    private File installation;

    /**
     * The database root.
     *
     * @parameter expression="${mongodb.databaseRoot}" default-value="${project.build.directory}/mongodb"
     */
    private File databaseRoot;

    /**
     * The port to start mongodb on.
     *
     * @parameter expression="${mongodb.port}" default-value="27017"
     */
    private int port;

    /**
     * Whether to start mongo in quiet or verbose mode.
     *
     * @parameter expression="${mongodb.verbose}" default-value="false"
     */
    private boolean verbose;

    /**
     * Whether to start mongo in with security.
     *
     * @parameter expression="${mongodb.auth}" default-value="false"
     */
    private boolean auth;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping mongodb: mongodb.skip==true");
            return;
        }
        if (installation == null) {
            getLog().info("Using mongod from PATH");
        } else {
            getLog().info("Using mongod installed in " + installation);
        }
        getLog().info("Using database root of " + databaseRoot);
        final Logger mongoLogger = Logger.getLogger("com.mongodb");
        Level mongoLevel = mongoLogger.getLevel();
        try {
            mongoLogger.setLevel(Level.SEVERE);
            MongoOptions opts = new MongoOptions();
            opts.autoConnectRetry = false;
            opts.connectionsPerHost = 1;
            opts.connectTimeout = 50;
            opts.socketTimeout = 50;
            Mongo instance;
            try {
                instance = new Mongo(new ServerAddress("localhost", port), opts);
                List<String> databaseNames = instance.getDatabaseNames();
                throw new MojoExecutionException(
                        "Port " + port + " is already running a MongoDb instance with the following databases "
                                + databaseNames);
            } catch (MongoException.Network e) {
                // fine... no instance running
            } catch (MongoException e) {
                throw new MojoExecutionException("Port " + port + " is already running a MongoDb instance");
            } catch (UnknownHostException e) {
                // ignore... localhost is always known!
            }
        } finally {
            mongoLogger.setLevel(mongoLevel);
        }

        CommandLine commandLine = null;
        if (installation != null && installation.isDirectory()) {
            File bin = new File(installation, "bin");
            File exe = new File(bin, Os.isFamily(Os.FAMILY_WINDOWS) ? "mongod.exe" : "mongod");
            if (exe.isFile()) {
                commandLine = new CommandLine(exe);
            } else {
                throw new MojoExecutionException(
                        "Could not find mongo executables in specified installation: " + installation
                                + " expected to find " + exe + " but it does not exist.");
            }
        }
        if (commandLine == null) {
            commandLine = new CommandLine(Os.isFamily(Os.FAMILY_WINDOWS) ? "mongod.exe" : "mongod");
        }
        if (databaseRoot.isFile()) {
            throw new MojoExecutionException("Database root " + databaseRoot + " is a file and not a directory");
        }
        if (!databaseRoot.isDirectory()) {
            getLog().debug("Creating database root directory: " + databaseRoot);
            if (!databaseRoot.mkdirs()) {
                throw new MojoExecutionException("Could not create database root directory " + databaseRoot);
            }
        }

        if (!verbose) {
            commandLine.addArgument("--quiet");
        }

        commandLine.addArgument(auth ? "--auth" : "--noauth");

        commandLine.addArgument("--port");
        commandLine.addArgument(Integer.toString(port));

        commandLine.addArgument("--dbpath");
        commandLine.addArgument(databaseRoot.getAbsolutePath());

        Executor exec = new DefaultExecutor();
        DefaultExecuteResultHandler execHandler = new DefaultExecuteResultHandler();
        exec.setWorkingDirectory(databaseRoot);
        ProcessObserver processObserver = new ProcessObserver(new ShutdownHookProcessDestroyer());
        exec.setProcessDestroyer(processObserver);

        LogOutputStream stdout = new MavenLogOutputStream(getLog());
        LogOutputStream stderr = new MavenLogOutputStream(getLog());

        getLog().info("Executing command line: " + commandLine);
        exec.setStreamHandler(new PumpStreamHandler(stdout, stderr));
        try {
            exec.execute(commandLine, execHandler);
            getLog().info("Waiting for MongoDB to start...");
            long timeout = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(120);
            mongoLevel = mongoLogger.getLevel();
            try {
                mongoLogger.setLevel(Level.SEVERE);
                while (System.currentTimeMillis() < timeout && !execHandler.hasResult()) {
                    MongoOptions opts = new MongoOptions();
                    opts.autoConnectRetry = false;
                    opts.connectionsPerHost = 1;
                    opts.connectTimeout = 250;
                    opts.socketTimeout = 250;
                    Mongo instance;
                    try {
                        instance = new Mongo(new ServerAddress("localhost", port), opts);
                        List<String> databaseNames = instance.getDatabaseNames();
                        getLog().info("MongoDb started.");
                        getLog().info("Databases: " + databaseNames);
                    } catch (MongoException.Network e) {
                        // ignore, wait and try again
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e1) {
                            // ignore
                        }
                        continue;
                    } catch (MongoException e) {
                        getLog().info("MongoDb started.");
                        getLog().info("Unable to list databases due to " + e.getMessage());
                    }
                    break;
                }
            } finally {
                mongoLogger.setLevel(mongoLevel);
            }
            if (execHandler.hasResult()) {
                ExecuteException exception = execHandler.getException();
                if (exception != null) {
                    throw new MojoFailureException(exception.getMessage(), exception);
                }
                throw new MojoFailureException(
                        "Command " + commandLine + " exited with exit code " + execHandler.getExitValue());
            }
            Map pluginContext = session.getPluginContext(getPluginDescriptor(), project);
            pluginContext.put(ProcessObserver.class.getName() + ":" + Integer.toString(port), processObserver);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

    }
}
