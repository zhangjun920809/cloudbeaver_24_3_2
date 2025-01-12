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
package io.cloudbeaver.service.ldap.auth;

import io.cloudbeaver.DBWUserIdentity;
import io.cloudbeaver.auth.SMAuthProviderExternal;
import io.cloudbeaver.auth.SMBruteForceProtected;
import io.cloudbeaver.auth.provider.local.LocalAuthProviderConstants;
import io.cloudbeaver.model.session.WebSession;
import io.cloudbeaver.model.user.WebUser;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.auth.SMSession;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.security.SMAuthProviderCustomConfiguration;
import org.jkiss.dbeaver.model.security.SMController;
import org.jkiss.utils.CommonUtils;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;

public class LdapAuthProvider implements SMAuthProviderExternal<SMSession>, SMBruteForceProtected {
    private static final Log log = Log.getLog(LdapAuthProvider.class);

    public LdapAuthProvider() {
    }

    @NotNull
    @Override
    public Map<String, Object> authExternalUser(
        @NotNull DBRProgressMonitor monitor,
        @Nullable SMAuthProviderCustomConfiguration providerConfig,
        @NotNull Map<String, Object> authParameters
    ) throws DBException {
        if (providerConfig == null) {
            throw new DBException("LDAP provider config is null");
        }
        String userName = JSONUtils.getString(authParameters, LdapConstants.CRED_USER_DN);
        if (CommonUtils.isEmpty(userName)) {
            throw new DBException("LDAP user dn is empty");
        }
        String password = JSONUtils.getString(authParameters, LdapConstants.CRED_PASSWORD);
        if (CommonUtils.isEmpty(password)) {
            throw new DBException("LDAP password is empty");
        }

        LdapSettings ldapSettings = new LdapSettings(providerConfig);
        Hashtable<String, String> environment = creteAuthEnvironment(ldapSettings);

        Map<String, Object> userData = null;
        if (!isFullDN(userName) && CommonUtils.isNotEmpty(ldapSettings.getLoginAttribute())) {
            userData = validateAndLoginUserAccessByUsername(userName, password, ldapSettings);

        }
        if (userData == null) {
            String fullUserDN = buildFullUserDN(userName, ldapSettings);
            validateUserAccess(fullUserDN, ldapSettings);
            userData = authenticateLdap(fullUserDN, password, ldapSettings, null, environment);
        }
        return userData;
    }
    /**
     * Find user and validate in ldap by uniq parameter from identityProviders
     *
     */
    private Map<String, Object> validateAndLoginUserAccessByUsername(
        @NotNull String login,
        @NotNull String password,
        @NotNull LdapSettings ldapSettings
    ) throws DBException {
        if (
            CommonUtils.isEmpty(ldapSettings.getBindUserDN())
            || CommonUtils.isEmpty(ldapSettings.getBindUserPassword())
        ) {
            return null;
        }
        Hashtable<String, String> serviceUserContext = creteAuthEnvironment(ldapSettings);
        serviceUserContext.put(Context.SECURITY_PRINCIPAL, ldapSettings.getBindUserDN());
        serviceUserContext.put(Context.SECURITY_CREDENTIALS, ldapSettings.getBindUserPassword());
        DirContext serviceContext;

        try {
            serviceContext = new InitialDirContext(serviceUserContext);
            String userDN = findUserDN(serviceContext, ldapSettings, login);
            if (userDN == null) {
                return null;
            }
            return authenticateLdap(userDN, password, ldapSettings, login, creteAuthEnvironment(ldapSettings));
        } catch (Exception e) {
            throw new DBException("LDAP authentication failed: " + e.getMessage(), e);
        }
    }

    /**
     * Find user and validate in ldap by fullUserDN
     */
    private void validateUserAccess(@NotNull String fullUserDN, @NotNull LdapSettings ldapSettings) throws DBException {
        if (
            CommonUtils.isEmpty(ldapSettings.getFilter())
            || CommonUtils.isEmpty(ldapSettings.getBindUserDN())
            || CommonUtils.isEmpty(ldapSettings.getBindUserPassword())
        ) {
            return;
        }

        var environment = creteAuthEnvironment(ldapSettings);
        environment.put(Context.SECURITY_PRINCIPAL, ldapSettings.getBindUserDN());
        environment.put(Context.SECURITY_CREDENTIALS, ldapSettings.getBindUserPassword());
        DirContext bindUserContext;
        try {
            bindUserContext = new InitialDirContext(environment);
            SearchControls searchControls = createSearchControls();
            var searchResult = bindUserContext.search(fullUserDN, ldapSettings.getFilter(), searchControls);
            if (!searchResult.hasMore()) {
                throw new DBException("Access denied");
            }
        } catch (DBException e) {
            throw e;
        } catch (Exception e) {
            throw new DBException("LDAP user access validation by filter failed: " + e.getMessage(), e);
        }
    }

    @NotNull
    private static Hashtable<String, String> creteAuthEnvironment(LdapSettings ldapSettings) {
        Hashtable<String, String> environment = new Hashtable<>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

        environment.put(Context.PROVIDER_URL, ldapSettings.getLdapProviderUrl());
        environment.put(Context.SECURITY_AUTHENTICATION, "simple");
        return environment;
    }

    private String findUserDN(DirContext serviceContext, LdapSettings ldapSettings, String userIdentifier) throws DBException {
        try {
            String searchFilter = buildSearchFilter(ldapSettings, userIdentifier);
            SearchControls searchControls = new SearchControls();
            searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchControls.setReturningAttributes(new String[]{"distinguishedName"});
            String baseDN = getBaseDN(serviceContext, ldapSettings);

            NamingEnumeration<SearchResult> results = serviceContext.search(baseDN, searchFilter, searchControls);

            if (results.hasMore()) {
                return results.next().getNameInNamespace();
            }
            return null;
        } catch (Exception e) {
            throw new DBException("Error finding user DN: " + e.getMessage(), e);
        }
    }

    private String getBaseDN(DirContext serviceContext, LdapSettings ldapSettings) throws DBException {
        if (CommonUtils.isEmpty(ldapSettings.getBaseDN())) {
            return getRootDN(serviceContext);
        }
        return ldapSettings.getBaseDN();
    }

    private String buildSearchFilter(LdapSettings ldapSettings, String userIdentifier) {
        String userFilter = String.format("(%s=%s)", ldapSettings.getLoginAttribute(), userIdentifier);
        if (CommonUtils.isNotEmpty(ldapSettings.getFilter())) {
            return String.format("(&%s%s)", userFilter, ldapSettings.getFilter());
        }
        return userFilter;
    }

    private String getRootDN(DirContext adminContext) throws DBException {
        try {
            Attributes attributes = adminContext.getAttributes("", new String[]{"namingContexts"});
            Attribute namingContexts = attributes.get("namingContexts");
            if (namingContexts != null && namingContexts.size() > 0) {
                return (String) namingContexts.get(0);
            }
            throw new DBException("Root DN not found in namingContexts");
        } catch (Exception e) {
            throw new DBException("Error retrieving root DN: " + e.getMessage(), e);
        }
    }

    @NotNull
    private String findUserNameFromDN(@NotNull String fullUserDN, @NotNull LdapSettings ldapSettings)
        throws DBException {
        String userId = null;
        for (String dn : fullUserDN.split(",")) {
            if (dn.startsWith(ldapSettings.getUserIdentifierAttr() + "=")) {
                userId = dn.split("=")[1];
                break;
            }
        }
        if (userId == null) {
            throw new DBException("Failed to determinate userId from user DN: " + fullUserDN);
        }
        return userId;
    }

    @NotNull
    @Override
    public DBWUserIdentity getUserIdentity(
        @NotNull DBRProgressMonitor monitor,
        @Nullable SMAuthProviderCustomConfiguration customConfiguration,
        @NotNull Map<String, Object> authParameters
    ) throws DBException {
        String userName = JSONUtils.getString(authParameters, LocalAuthProviderConstants.CRED_USER);
        if (CommonUtils.isEmpty(userName)) {
            throw new DBException("LDAP user name is empty");
        }
        String displayName = JSONUtils.getString(authParameters, LocalAuthProviderConstants.CRED_DISPLAY_NAME);
        if (CommonUtils.isEmpty(displayName)) {
            displayName = userName;
        }
        return new DBWUserIdentity(userName, displayName);
    }

    @Nullable
    @Override
    public DBPObject getUserDetails(
        @NotNull DBRProgressMonitor monitor,
        @NotNull WebSession webSession,
        @NotNull SMSession session,
        @NotNull WebUser user,
        boolean selfIdentity
    ) throws DBException {
        return null;
    }

    @NotNull
    @Override
    public String validateLocalAuth(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SMController securityController,
        @NotNull SMAuthProviderCustomConfiguration providerConfig,
        @NotNull Map<String, Object> userCredentials,
        @Nullable String activeUserId
    ) throws DBException {
        String userId = JSONUtils.getString(userCredentials, LdapConstants.CRED_USERNAME);
        if (CommonUtils.isEmpty(userId)) {
            throw new DBException("LDAP user id not found");
        }
        return activeUserId == null ? userId : activeUserId;
    }

    @Override
    public SMSession openSession(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SMSession mainSession,
        @Nullable SMAuthProviderCustomConfiguration customConfiguration,
        @NotNull Map<String, Object> userCredentials
    ) throws DBException {
        return new LdapSession(mainSession, mainSession.getSessionSpace(), userCredentials);
    }

    @Override
    public void closeSession(@NotNull SMSession mainSession, SMSession session) throws DBException {

    }

    @Override
    public void refreshSession(
        @NotNull DBRProgressMonitor monitor,
        @NotNull SMSession mainSession,
        SMSession session
    ) throws DBException {

    }

    @Override
    public Object getInputUsername(@NotNull Map<String, Object> cred) {
        return cred.get(LdapConstants.CRED_USERNAME);
    }

    private boolean isFullDN(String userName) {
        return userName.contains(",") && userName.contains("=");
    }

    private String buildFullUserDN(String userName, LdapSettings ldapSettings) {
        String fullUserDN = userName;

        if (!fullUserDN.startsWith(ldapSettings.getUserIdentifierAttr())) {
            fullUserDN = String.join("=", ldapSettings.getUserIdentifierAttr(), userName);
        }
        if (CommonUtils.isNotEmpty(ldapSettings.getBaseDN()) && !fullUserDN.endsWith(ldapSettings.getBaseDN())) {
            fullUserDN = String.join(",", fullUserDN, ldapSettings.getBaseDN());
        }

        return fullUserDN;
    }

    private SearchControls createSearchControls() {
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setTimeLimit(30_000);
        return searchControls;
    }

    private Map<String, Object> authenticateLdap(
        String userDN,
        String password,
        LdapSettings ldapSettings,
        @Nullable String login,
        Hashtable<String, String> environment
    ) throws DBException {
        environment.put(Context.SECURITY_PRINCIPAL, userDN);
        environment.put(Context.SECURITY_CREDENTIALS, password);
        DirContext userContext = null;
        try {
            userContext = new InitialDirContext(environment);
            Map<String, Object> userData = new HashMap<>();
            userData.put(LdapConstants.CRED_USERNAME, findUserNameFromDN(userDN, ldapSettings));
            userData.put(LdapConstants.CRED_SESSION_ID, UUID.randomUUID());
            if (login != null) {
                userData.put(LdapConstants.CRED_DISPLAY_NAME, login);
            }
            return userData;
        } catch (Exception e) {
            throw new DBException("LDAP authentication failed: " + e.getMessage(), e);
        } finally {
            if (userContext != null) {
                try {
                    userContext.close();
                } catch (NamingException e) {
                    log.warn("Error closing LDAP user context", e);
                }
            }
        }
    }

}
