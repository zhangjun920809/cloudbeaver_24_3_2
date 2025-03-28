/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package io.cloudbeaver.server.jobs;

import io.cloudbeaver.service.session.CBSessionManager;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.PeriodicJob;

import java.time.Duration;

/**
 * WebSessionMonitorJob
 */
public class WebSessionMonitorJob extends PeriodicJob {
    private static final Log log = Log.getLog(WebSessionMonitorJob.class);
    private final CBSessionManager sessionManager;

    public WebSessionMonitorJob(@NotNull DBPPlatform platform, @NotNull CBSessionManager sessionManager) {
        super("Web session monitor", platform, Duration.ofSeconds(10));
        this.sessionManager = sessionManager;
    }

    @Override
    protected void doJob(@NotNull DBRProgressMonitor monitor) {
        try {
            sessionManager.expireIdleSessions();
        } catch (Exception e) {
            log.error("Error on expire idle sessions", e);
        }
    }
}