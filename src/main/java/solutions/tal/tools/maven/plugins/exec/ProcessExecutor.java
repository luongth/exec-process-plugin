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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author Thuan Luong
 */
final class ProcessExecutor {

    private final String name;

    private final String executable;

    private final List<String> args = new ArrayList<>();

    private final Map<String, String> environmentVariables = new HashMap<>();

    private Set<String> systemPropertyArgs;

    private Process process;

    private File outputFile;

    private ProcessExecutor(String name, String executable, List<String> args) {
        this.name = name;
        this.executable = executable;
        this.args.addAll(args);
    }

    static ProcessExecutor create(String name, String executable, List<String> args) {
        return new ProcessExecutor(name, executable, args);
    }

    ProcessExecutor withEnvironmentVariables(Map<String, String> environmentVariables) {
        this.environmentVariables.putAll(Maps.transformValues(environmentVariables,
                new Function<String, String>() {
                    @Override
                    public String apply(String input) {
                        return input != null ? input : "";
                    }
                }));
        return this;
    }

    ProcessExecutor withOutputFile(File outputFile) {
        this.outputFile = outputFile;
        return this;
    }

    ProcessExecutor withSystemProperties(Map<String, String> systemProperties) {
        final Iterable<String> transformedSystemProperties = Iterables.transform(systemProperties.entrySet(), new Function<Map.Entry<String,String>, String>() {
            @Override
            public String apply(Map.Entry<String, String> input) {
                return "-D" + input.getKey().trim() + "=" + input.getValue().trim();
            }
        });
        this.systemPropertyArgs = Sets.newHashSet(transformedSystemProperties);
        return this;
    }

    private void validate() throws MojoExecutionException {
        // check if executable exists
        final String sanitizedExec = sanitizedExecutable(executable);
        final Path path = Paths.get(sanitizedExec);
        if (!Files.isRegularFile(path) || !Files.isExecutable(path)) {
            throw new MojoExecutionException(String.format("'%s' is not a regular file and/or executable", executable));
        }
    }

    private static String sanitizedExecutable(String executable) {
        final String osName = System.getProperty("os.name").toLowerCase(Locale.US);
        return osName.contains("windows") ? executable + ".exe" : executable;
    }

    String getName() {
        return this.name;
    }

    void execute(File workingDir, final Log mavenLog) throws MojoExecutionException {
        validate();

        final List<String> arguments = new ArrayList<>();
        arguments.add(executable);
        if (systemPropertyArgs != null) {
            arguments.addAll(systemPropertyArgs);
        }
        arguments.addAll(args);

        if (mavenLog.isInfoEnabled()) {
            mavenLog.info("Command line arguments:\n" + arguments);
            mavenLog.info("With environment variables:\n" + environmentVariables);
        }

        final ProcessBuilder pb = new ProcessBuilder();
        pb.command(arguments);
        pb.directory(workingDir);
        pb.environment().putAll(environmentVariables);
        pb.redirectErrorStream(true);
        if (outputFile != null) {
            pb.redirectOutput(outputFile);
        } else {
            pb.inheritIO();
        }
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new MojoExecutionException("Error starting process", e);
        }
    }

    void stop() {
        process.destroy();
    }

    void waitFor() {
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


}
