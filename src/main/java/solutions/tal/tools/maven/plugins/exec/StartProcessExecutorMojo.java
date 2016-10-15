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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Thuan Luong
 */
@Mojo(name = "start", defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST)
public class StartProcessExecutorMojo extends AbstractProcessExecutorMojo {

    @Component
    private RepositorySystem repositorySystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repositorySystemSession;

    @Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true)
    private List<RemoteRepository> remoteProjectRepositories;

    @Parameter
    private String executable;

    @Parameter
    private String name;

    @Parameter
    private File outputFile;

    @Parameter
    private File workingDir;

    @Parameter
    private List<?> arguments;

    @Parameter
    private List<Dependency> dependencies;

    @Parameter
    private List<String> additionalClasspathElements;

    @Parameter
    private Map<String, String> environmentVariables = new HashMap<>();

    @Parameter
    private ApplicationReadiness readinessCheck;

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
        waitForInterruptIfRequired();
    }

    private File deriveWorkingDir(File workingDir) {
        return workingDir != null ? workingDir : new File(project.getBuild().getOutputDirectory());
    }

    private String buildClasspathStringArgument() throws MojoExecutionException {
        final List<String> runtimeClasspathElements = new ArrayList<>();
        if (additionalClasspathElements != null) {
            runtimeClasspathElements.addAll(additionalClasspathElements);
        }
        if (dependencies != null) {
            try {
                for (Dependency dependency : dependencies) {
                    final org.eclipse.aether.graph.Dependency rootDependency = toAetherDependency(dependency);
                    final CollectRequest collectRequest = new CollectRequest(rootDependency, remoteProjectRepositories);
                    final DependencyRequest dependencyRequest =
                            new DependencyRequest(collectRequest, DependencyFilterUtils.classpathFilter(Artifact.SCOPE_RUNTIME));

                    final DependencyResult result = repositorySystem.resolveDependencies(repositorySystemSession, dependencyRequest);
                    if (result.getCollectExceptions().isEmpty()) {
                        final List<ArtifactResult> resolvedArtifactResults = result.getArtifactResults();
                        for (ArtifactResult resolvedArtifactResult : resolvedArtifactResults) {
                            runtimeClasspathElements.add(resolvedArtifactResult.getArtifact().getFile().getAbsolutePath());
                        }
                    } else {
                        throw new MojoExecutionException("Failed to resolve dependencies for: " + dependency);
                    }
                }
            } catch (DependencyResolutionException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
        final StringBuilder sb = new StringBuilder();
        final Iterator<String> it = runtimeClasspathElements.iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(File.pathSeparatorChar);
            }
        }
        return sb.toString();
    }

    private static org.eclipse.aether.graph.Dependency toAetherDependency(Dependency dependency) {
        final org.eclipse.aether.artifact.Artifact artifact = new DefaultArtifact(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getClassifier(),
                dependency.getType(),
                dependency.getVersion());
        final org.eclipse.aether.graph.Dependency aetherDependency =
                new org.eclipse.aether.graph.Dependency(artifact, Artifact.SCOPE_RUNTIME);
        return aetherDependency.setExclusions(Sets.newHashSet(Iterables.transform(dependency.getExclusions(),
                new Function<Exclusion, org.eclipse.aether.graph.Exclusion>() {
                    @Override
                    public org.eclipse.aether.graph.Exclusion apply(Exclusion input) {
                        return new org.eclipse.aether.graph.Exclusion(input.getGroupId(), input.getArtifactId(), "*", "*");
                    }
                })));
    }

}
