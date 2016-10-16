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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * @author Thuan Luong
 */
@SuppressWarnings("unchecked")
final class PluginExecutionStateHolder {

    private static final String PROCESS_STACK_KEY = "PROCESS_STACK_KEY";

    static void addProcess(ProcessExecutor process, Map pluginContext) {
        getProcesses(pluginContext).push(process);
    }

    static Deque<ProcessExecutor> getProcesses(Map pluginContext) {
        Deque<ProcessExecutor> processes = (Deque<ProcessExecutor>) pluginContext.get(PROCESS_STACK_KEY);
        if (processes == null) {
            processes = new ArrayDeque<>();
            pluginContext.put(PROCESS_STACK_KEY, processes);
        }
        return processes;
    }
}
