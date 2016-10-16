/*
 * Copyright 2016 Thuan Anh Luong
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

package solutions.tal.tools.maven.plugins.exec;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Thuan Luong
 */
abstract class AbstractProcessExecutorMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    MavenProject project;


    @Parameter(defaultValue = "false")
    private boolean waitForInterrupt;

    AbstractProcessExecutorMojo() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                final Deque<ProcessExecutor> processExecutors = PluginExecutionStateHolder.getProcesses(getPluginContext());
                if (!processExecutors.isEmpty()) {
                    stopAllProcesses();
                }
            }
        });
    }

    void waitForInterruptIfRequired() throws MojoExecutionException {
        if (waitForInterrupt) {
            try {
                sleepUntilInterrupted();
            } catch (IOException e) {
                throw new MojoExecutionException("Unexpected error", e);
            }
        }
    }

    private void sleepUntilInterrupted() throws IOException {
        getLog().info("Hit ENTER on the console to continue...");

        for (;;) {
            int ch = System.in.read();
            if (ch == -1 || ch == '\n') {
                break;
            }
        }
    }

    void stopAllProcesses() {
        final Deque<ProcessExecutor> processExecutors = PluginExecutionStateHolder.getProcesses(getPluginContext());
        while (!processExecutors.isEmpty()) {
            final ProcessExecutor processExecutor = processExecutors.pop();
            getLog().info("Stopping process: " + processExecutor.getName());
            processExecutor.stop();
            processExecutor.waitFor();
            getLog().info("Stopped process: " + processExecutor.getName());
        }
    }
}
