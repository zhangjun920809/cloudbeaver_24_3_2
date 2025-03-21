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
package io.cloudbeaver.server.events;

import io.cloudbeaver.model.session.BaseWebSession;
import io.cloudbeaver.server.WebAppUtils;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.websocket.WSEventHandler;
import org.jkiss.dbeaver.model.websocket.event.WSEvent;

import java.util.Collection;

public class WSDefaultEventHandler<EVENT extends WSEvent> implements WSEventHandler<EVENT> {

    private static final Log log = Log.getLog(WSDefaultEventHandler.class);

    @Override
    public void handleEvent(@NotNull EVENT event) {
        log.debug(event.getTopicId() + " event handled");
        Collection<BaseWebSession> allSessions = WebAppUtils.getWebApplication()
            .getSessionManager()
            .getAllActiveSessions();
        for (var activeUserSession : allSessions) {
            if (!isAcceptableInSession(activeUserSession, event)) {
                log.debug("Cannot handle " + event.getTopicId() + " event '" + event.getId() +
                    "' in session " + activeUserSession.getSessionId());
                continue;
            }
            log.debug(event.getTopicId() + " event '" + event.getId() + "' handled");
            updateSessionData(activeUserSession, event);
        }
    }

    protected void updateSessionData(@NotNull BaseWebSession activeUserSession, @NotNull EVENT event) {
        activeUserSession.addSessionEvent(event);
    }

    protected boolean isAcceptableInSession(@NotNull BaseWebSession activeUserSession, @NotNull EVENT event) {
        return !WSWebUtils.isSessionIdEquals(activeUserSession, event.getSessionId()); // skip events from current session
    }
}
