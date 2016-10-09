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

import org.apache.maven.plugin.logging.Log;

import java.util.concurrent.TimeUnit;

/**
 * @author Thuan Luong
 */
public class FixedWaitTimeReadinessCheck implements ApplicationReadiness {

    private TimeUnit timeUnit;

    private long time;

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public void waitForReadiness(Log log, String processName) {
        try {
            log.info(String.format("Pausing build for %d %s to wait for process '%s' to complete startup", time, timeUnit.name(), processName));
            timeUnit.sleep(time);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
