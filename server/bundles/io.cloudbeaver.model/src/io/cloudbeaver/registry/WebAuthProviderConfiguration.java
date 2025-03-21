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
package io.cloudbeaver.registry;

import io.cloudbeaver.auth.CBAuthConstants;
import io.cloudbeaver.auth.SMAuthProviderFederated;
import io.cloudbeaver.auth.SMSignOutLinkProvider;
import io.cloudbeaver.utils.ServletAppUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.auth.SMAuthProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.security.SMAuthProviderCustomConfiguration;

import java.util.Map;

/**
 * WebAuthProviderConfiguration.
 */
public class WebAuthProviderConfiguration {

    private static final Log log = Log.getLog(WebAuthProviderConfiguration.class);

    private final WebAuthProviderDescriptor providerDescriptor;
    private final SMAuthProviderCustomConfiguration config;

    public WebAuthProviderConfiguration(WebAuthProviderDescriptor providerDescriptor, SMAuthProviderCustomConfiguration config) {
        this.providerDescriptor = providerDescriptor;
        this.config = config;
    }

    public String getProviderId() {
        return providerDescriptor.getId();
    }

    public String getId() {
        return config.getId();
    }

    public String getDisplayName() {
        return config.getDisplayName();
    }

    public boolean isDisabled() {
        return config.isDisabled();
    }

    public String getIconURL() {
        return config.getIconURL();
    }

    public String getDescription() {
        return config.getDescription();
    }

    public Map<String, Object> getParameters() {
        return config.getParameters();
    }

    @Property
    public String getSignInLink() throws DBException {
        SMAuthProvider<?> instance = providerDescriptor.getInstance();
        return instance instanceof SMAuthProviderFederated smAuthProviderFederated ?
            buildRedirectUrl(smAuthProviderFederated.getSignInLink(getId()))
            : null;
    }

    private String buildRedirectUrl(String baseUrl) {
        return baseUrl + "?" + CBAuthConstants.CB_REDIRECT_URL_REQUEST_PARAM + "=" + ServletAppUtils.getFullServerUrl();
    }

    @Property
    public String getSignOutLink() throws DBException {
        SMAuthProvider<?> instance = providerDescriptor.getInstance();
        return instance instanceof SMSignOutLinkProvider smSignOutLinkProvider
            ? smSignOutLinkProvider.getCommonSignOutLink(getId(), config.getParameters())
            : null;
    }

    @Property
    public String getRedirectLink() throws DBException {
        SMAuthProvider<?> instance = providerDescriptor.getInstance();
        return instance instanceof SMAuthProviderFederated smAuthProviderFederated
            ? smAuthProviderFederated.getRedirectLink(getId(), config.getParameters())
            : null;
    }

    @Property
    public String getMetadataLink() throws DBException {
        SMAuthProvider<?> instance = providerDescriptor.getInstance();
        return instance instanceof SMAuthProviderFederated smAuthProviderFederated
            ? smAuthProviderFederated.getMetadataLink(getId(), config.getParameters())
            : null;
    }

    @Property
    public String getAcsLink() throws DBException {
        SMAuthProvider<?> instance = providerDescriptor.getInstance();
        return instance instanceof SMAuthProviderFederated smAuthProviderFederated
            ? smAuthProviderFederated.getAcsLink(getId(), config.getParameters())
            : null;
    }

    @Property
    public String getEntityIdLink() throws  DBException {
        SMAuthProvider<?> instance = providerDescriptor.getInstance();
        return instance instanceof SMAuthProviderFederated smAuthProviderFederated
            ? smAuthProviderFederated.getEntityIdLink(getId(), config.getParameters())
            : null;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
