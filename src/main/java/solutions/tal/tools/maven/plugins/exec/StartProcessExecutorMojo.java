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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.resolver.filter.CumulativeScopeArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author Thuan Luong
 */
@Mojo(name = "start", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class StartProcessExecutorMojo extends AbstractProcessExecutorMojo {

    @Parameter
    private ApplicationReadiness readinessCheck;

    public void setReadinessCheck(ApplicationReadiness readinessCheck) {
        this.readinessCheck = readinessCheck;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final List<String> commandArguments = new ArrayList<>();
        for (Object argument : arguments) {
            if (argument instanceof Classpath) {
                commandArguments.add(buildClasspathStringArgument());
            } else if (argument instanceof String) {
                commandArguments.add((String) argument);
            }
        }
        final ProcessExecutor processExecutor = ProcessExecutor
                .create(name, executable, commandArguments)
                .withOutputFile(outputFile)
                .withEnvironmentVariables(environmentVariables);

        PluginExecutionStateHolder.addProcess(processExecutor, getPluginContext());

        getLog().info("Starting process: " + name);
        processExecutor.execute(deriveWorkingDir(workingDir), getLog());
        if (readinessCheck != null) {
            readinessCheck.waitForReadiness(getLog(), name);
        }
        getLog().info("Started process: " + name);
        if (waitForInterrupt) {
            try {
                sleepUntilInterrupted();
            } catch (IOException e) {
                throw new MojoExecutionException("Unexpected error", e);
            }
        }
    }

    private File deriveWorkingDir(File workingDir) {
        return workingDir != null ? workingDir : new File(project.getBuild().getOutputDirectory());
    }

    private String buildClasspathStringArgument() throws MojoExecutionException {
        // get project artifacts
        try {
            project.setArtifactFilter(new CumulativeScopeArtifactFilter(Collections.singletonList(Artifact.SCOPE_RUNTIME)));
            final List<String> runtimeClassPathElements = project.getRuntimeClasspathElements();
            final StringBuilder sb = new StringBuilder();
            final Iterator<String> it = runtimeClassPathElements.iterator();
            while (it.hasNext()) {
                sb.append(it.next());
                if (it.hasNext()) {
                    sb.append(File.pathSeparatorChar);
                }
            }
            return sb.toString();
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Failed to determine runtime classpath for project", e);
        }
    }

}
