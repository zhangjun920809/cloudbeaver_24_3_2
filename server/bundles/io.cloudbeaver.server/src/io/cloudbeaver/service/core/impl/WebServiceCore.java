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
package io.cloudbeaver.service.core.impl;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.cloudbeaver.*;
import io.cloudbeaver.indaas.erd.model.WebERDDiagramInfo;
import io.cloudbeaver.indaas.erd.model.WebERDUtils;
import io.cloudbeaver.model.*;
import io.cloudbeaver.model.app.ServletApplication;
import io.cloudbeaver.model.session.WebSession;
import io.cloudbeaver.registry.WebHandlerRegistry;
import io.cloudbeaver.registry.WebSessionHandlerDescriptor;
import io.cloudbeaver.server.WebAppUtils;
import io.cloudbeaver.server.WebApplication;
import io.cloudbeaver.service.core.DBWServiceCore;
import io.cloudbeaver.service.security.SMUtils;
import io.cloudbeaver.service.security.indaas.DriDatasourceService;
import io.cloudbeaver.utils.ServletAppUtils;
import io.cloudbeaver.utils.WebConnectionFolderUtils;
import io.cloudbeaver.utils.WebDataSourceUtils;
import io.cloudbeaver.utils.WebEventUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.model.ERDContentProviderDefault;
import org.jkiss.dbeaver.erd.model.ERDContext;
import org.jkiss.dbeaver.erd.model.ERDDiagram;
import org.jkiss.dbeaver.erd.model.ERDUtils;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerType;
import org.jkiss.dbeaver.model.net.DBWNetworkHandler;
import org.jkiss.dbeaver.model.net.DBWTunnel;
import org.jkiss.dbeaver.model.net.ssh.SSHSession;
import org.jkiss.dbeaver.model.rm.RMProjectType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.model.secret.DBSSecretValue;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.websocket.WSConstants;
import org.jkiss.dbeaver.model.websocket.event.datasource.WSDataSourceConnectEvent;
import org.jkiss.dbeaver.model.websocket.event.datasource.WSDataSourceProperty;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.registry.network.NetworkHandlerDescriptor;
import org.jkiss.dbeaver.registry.network.NetworkHandlerRegistry;
import org.jkiss.dbeaver.registry.settings.ProductSettingsRegistry;
import org.jkiss.dbeaver.runtime.jobs.ConnectionTestJob;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Web service implementation
 */
public class WebServiceCore implements DBWServiceCore {

    private static final Log log = Log.getLog(WebServiceCore.class);

    @Override
    public WebServerConfig getServerConfig() {
        return WebAppUtils.getWebApplication().getWebServerConfig();
    }
    public Map<String, Object> generateEntityDiagram(WebSession webSession, List<String> nodeIds) throws DBWebException {
        try {
            ERDDiagram diagram = generateDiagram(webSession, nodeIds);
            return WebERDUtils.serializeDiagram(webSession, diagram, true);
        } catch (DBException var4) {
            throw new DBWebException("Couldn't generate ER diagram content", var4);
        }
    }

    public WebERDDiagramInfo generateEntityDiagramExtended(@NotNull WebSession webSession, @NotNull List<String> nodeIds) throws DBWebException {
        try {
            ERDDiagram diagram = generateDiagram(webSession, nodeIds);
            ERDContext erdContext = WebERDUtils.buildContext(webSession, diagram);
            if (erdContext == null) {
                throw new DBWebException("Error building ERD context");
            } else {
                return new WebERDDiagramInfo(diagram, erdContext, true);
            }
        } catch (DBException var5) {
            throw new DBWebException("Couldn't generate ER diagram content", var5);
        }
    }
    private static ERDDiagram generateDiagram(@NotNull WebSession webSession, @NotNull List<String> nodeIds) throws DBException {
        List<DBSObject> rootObjects = new ArrayList();
        DBRProgressMonitor monitor = webSession.getProgressMonitor();
        DBNModel navigatorModel = webSession.getNavigatorModelOrThrow();
        Iterator var6 = nodeIds.iterator();

        while(var6.hasNext()) {
            String nodeId = (String)var6.next();
            DBNNode node = navigatorModel.getNodeByPath(monitor, nodeId);
            if (node instanceof DBNDatabaseNode databaseNode) {
                rootObjects.add(databaseNode.getObject());
            }
        }

        ERDDiagram diagram = new ERDDiagram((DBSObject)null, "Web ERD", new ERDContentProviderDefault());

        Object root;
        for(Iterator var11 = rootObjects.iterator(); var11.hasNext(); diagram.fillEntities(monitor, ERDUtils.collectDatabaseTables(monitor, (DBSObject)root, diagram, true, false), (DBSObject)root)) {
            root = (DBSObject)var11.next();
            if (root instanceof DBPDataSourceContainer dataSourceContainer) {
                if (!dataSourceContainer.isConnected()) {
                    dataSourceContainer.connect(monitor, true, true);
                }

                root = ((DBSObject)root).getDataSource();
            }
        }

        Object var13;
        if (rootObjects.size() == 1 && (var13 = rootObjects.get(0)) instanceof DBSObjectContainer) {
            DBSObjectContainer objectContainer = (DBSObjectContainer)var13;
            diagram.setRootObjectContainer(objectContainer);
        }

        return diagram;
    }
    @Override
    public List<WebDatabaseDriverInfo> getDriverList(@NotNull WebSession webSession, String driverId) {
        List<WebDatabaseDriverInfo> result = new ArrayList<>();
        for (DBPDriver driver : WebAppUtils.getWebPlatform().getApplicableDrivers()) {
            if (driverId == null || driverId.equals(driver.getFullId())) {
                result.add(new WebDatabaseDriverInfo(webSession, driver));
            }
        }
        return result;
    }

    @Override
    public List<WebDatabaseAuthModel> getAuthModels(@NotNull WebSession webSession) {
        return DataSourceProviderRegistry.getInstance().getAllAuthModels().stream()
            .map(am -> new WebDatabaseAuthModel(webSession, am)).collect(Collectors.toList());
    }

    @Override
    public List<WebNetworkHandlerDescriptor> getNetworkHandlers(@NotNull WebSession webSession) {
        return NetworkHandlerRegistry.getInstance().getDescriptors().stream()
            .filter(d -> !d.isDesktopHandler())
            .map(d -> new WebNetworkHandlerDescriptor(webSession, d)).collect(Collectors.toList());
    }

    @Override
    public List<WebConnectionInfo> getUserConnections(
        @NotNull WebSession webSession,
        @Nullable String projectId,
        @Nullable String id,
        @Nullable List<String> projectIds
    ) throws DBWebException {
        if (id != null) {
            WebConnectionInfo connectionInfo = getConnectionState(webSession, projectId, id);
            if (connectionInfo != null) {
                return Collections.singletonList(connectionInfo);
            }
        }
        var stream = webSession.getAccessibleProjects().stream();
        if (projectId != null) {
            stream = stream.filter(c -> c.getId().equals(projectId));
        }
        if (projectIds != null) {
            stream = stream.filter(c -> projectIds.contains(c.getId()));
        }
        Set<String> applicableDrivers = WebServiceUtils.getApplicableDriversIds();
        return stream
            .flatMap(p -> p.getConnections().stream())
            .filter(c -> applicableDrivers.contains(c.getDataSourceContainer().getDriver().getId()))
            .toList();
    }

    @Deprecated
    @Override
    public List<WebDataSourceConfig> getTemplateDataSources() throws DBWebException {

        List<WebDataSourceConfig> result = new ArrayList<>();
        DBPDataSourceRegistry dsRegistry = WebServiceUtils.getGlobalDataSourceRegistry();

        for (DBPDataSourceContainer ds : dsRegistry.getDataSources()) {
            if (ds.isTemplate()) {
                if (WebAppUtils.getWebPlatform().getApplicableDrivers().contains(ds.getDriver())) {
                    result.add(new WebDataSourceConfig(ds));
                } else {
                    log.debug("Template datasource '" + ds.getName() + "' ignored - driver is not applicable");
                }
            }
        }

        return result;
    }

    @Override
    public List<WebConnectionInfo> getTemplateConnections(
        @NotNull WebSession webSession, @Nullable String projectId
    ) throws DBWebException {
        if (webSession.getApplication().isDistributed()) {
            return List.of();
        }
        List<WebConnectionInfo> result = new ArrayList<>();
        if (projectId == null) {
            for (WebSessionProjectImpl project : webSession.getAccessibleProjects()) {
                getTemplateConnectionsFromProject(webSession, project, result);
            }
        } else {
            WebSessionProjectImpl project = getProjectById(webSession, projectId);
            getTemplateConnectionsFromProject(webSession, project, result);
        }
        return result;
    }

    private void getTemplateConnectionsFromProject(
        @NotNull WebSession webSession,
        @NotNull WebSessionProjectImpl project,
        List<WebConnectionInfo> result
    ) {
        DBPDataSourceRegistry registry = project.getDataSourceRegistry();
        for (DBPDataSourceContainer ds : registry.getDataSources()) {
            if (ds.isTemplate() &&
                project.getDataSourceFilter().filter(ds) &&
                WebAppUtils.getWebPlatform().getApplicableDrivers().contains(ds.getDriver())) {
                result.add(new WebConnectionInfo(webSession, ds));
            }
        }
    }

    @Override
    public List<WebConnectionFolderInfo> getConnectionFolders(
        @NotNull WebSession webSession, @Nullable String projectId, @Nullable String id
    ) throws DBWebException {
        if (projectId == null) {
            return webSession.getAccessibleProjects().stream()
                .flatMap(pr -> getConnectionFoldersFromProject(webSession, pr).stream())
                .collect(Collectors.toList());
        }
        if (id != null) {
            WebConnectionFolderInfo folderInfo = WebConnectionFolderUtils.getFolderInfo(webSession, projectId, id);
            return Collections.singletonList(folderInfo);
        }
        DBPProject project = getProjectById(webSession, projectId);
        return getConnectionFoldersFromProject(webSession, project);
    }

    private List<WebConnectionFolderInfo> getConnectionFoldersFromProject(
        @NotNull WebSession webSession,
        @NotNull DBPProject project
    ) {
        return project.getDataSourceRegistry().getAllFolders().stream()
            .map(f -> new WebConnectionFolderInfo(webSession, f)).collect(Collectors.toList());
    }

    @Override
    public String[] getSessionPermissions(@NotNull WebSession webSession) throws DBWebException {
        if (ServletAppUtils.getServletApplication().isConfigurationMode()) {
            return new String[]{
                DBWConstants.PERMISSION_ADMIN
            };
        }
        return webSession.getSessionPermissions().toArray(new String[0]);
    }

    @Override
    public WebSession openSession(
        @NotNull WebSession webSession,
        @Nullable String defaultLocale,
        @NotNull HttpServletRequest servletRequest,
        @NotNull HttpServletResponse servletResponse
    ) throws DBWebException {
        for (WebSessionHandlerDescriptor hd : WebHandlerRegistry.getInstance().getSessionHandlers()) {
            try {
                hd.getInstance().handleSessionOpen(webSession, servletRequest, servletResponse);
            } catch (Exception e) {
                log.error("Error calling session handler '" + hd.getId() + "'", e);
                webSession.addSessionError(e);
            }
        }
        webSession.setLocale(defaultLocale);
        return webSession;
    }

    /**
     * Updates the user's permissions
     *
     * @deprecated CB-2773. The actual way to get session state is {@code WSSessionStateEvent} which sends periodically via web socket.
     */
    @Deprecated
    @Override
    public WebSession getSessionState(@NotNull WebSession webSession) throws DBWebException {
        try {
            webSession.getUserContext().refreshPermissions();
        } catch (DBException e) {
            throw new DBWebException("Cannot refresh user permissions", e);
        }
        return webSession;
    }

    @Override
    public List<WebServerMessage> readSessionLog(
        @NotNull WebSession webSession,
        Integer maxEntries,
        Boolean clearEntries
    ) {
        return webSession.readLog(maxEntries, clearEntries);
    }

    @Override
    public boolean closeSession(HttpServletRequest request) throws DBWebException {
        try {
            var baseWebSession = WebAppUtils.getWebApplication().getSessionManager().closeSession(request);
            if (baseWebSession instanceof WebSession webSession) {
                for (WebSessionHandlerDescriptor hd : WebHandlerRegistry.getInstance().getSessionHandlers()) {
                    try {
                        hd.getInstance().handleSessionClose(webSession);
                    } catch (Exception e) {
                        log.error("Error calling session handler '" + hd.getId() + "'", e);
                        baseWebSession.addSessionError(e);
                    }
                }
                return true;
            }
        } catch (Exception e) {
            throw new DBWebException("Error closing session", e);
        }
        return false;
    }

    @Override
    @Deprecated
    public boolean touchSession(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) throws DBWebException {
        return WebAppUtils.getWebApplication().getSessionManager().touchSession(request, response);
    }

    @Override
    @Deprecated
    public WebSession updateSession(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response)
        throws DBWebException {
        var sessionManager = WebAppUtils.getWebApplication().getSessionManager();
        sessionManager.touchSession(request, response);
        return sessionManager.getWebSession(request, response, true);
    }

    @Override
    public boolean refreshSessionConnections(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response)
        throws DBWebException {
        WebSession session = WebAppUtils.getWebApplication().getSessionManager().getWebSession(request, response);
        if (session == null) {
            return false;
        } else {
            // We do full user refresh because we need to get config from global project
            session.refreshUserData();
            return true;
        }
    }

    @Override
    public boolean changeSessionLanguage(@NotNull WebSession webSession, String locale) {
        webSession.setLocale(locale);
        return true;
    }

    @Override
    public WebConnectionInfo getConnectionState(
        @NotNull WebSession webSession,
        @Nullable String projectId,
        @NotNull String connectionId
    ) throws DBWebException {
        return WebDataSourceUtils.getWebConnectionInfo(webSession, projectId, connectionId);
    }


    @Override
    public WebConnectionInfo initConnection(
        @NotNull WebSession webSession,
        @Nullable String projectId,//u_cbadmin
        @NotNull String connectionId,//mysql8-xxx
        @NotNull Map<String, Object> authProperties,
        @Nullable List<WebNetworkHandlerConfigInput> networkCredentials,
        @Nullable Boolean saveCredentials,
        @Nullable Boolean sharedCredentials,
        @Nullable String selectedSecretId
    ) throws DBWebException {
        WebConnectionInfo connectionInfo = WebDataSourceUtils.getWebConnectionInfo(webSession, projectId, connectionId);//返回信息包含了数据源的的信息
        connectionInfo.setSavedCredentials(authProperties, networkCredentials);

        var dataSourceContainer = (DataSourceDescriptor) connectionInfo.getDataSourceContainer();
        if (dataSourceContainer.isConnected()) {
            throw new DBWebException("Datasource '" + dataSourceContainer.getName() + "' is already connected");
        }
        if (dataSourceContainer.isSharedCredentials() && selectedSecretId != null) {
            List<DBSSecretValue> allSecrets;
            try {
                allSecrets = dataSourceContainer.listSharedCredentials();
            } catch (DBException e) {
                throw new DBWebException("Error loading connection secret", e);
            }
            DBSSecretValue selectedSecret =
                allSecrets.stream()
                    .filter(secret -> selectedSecretId.equals(secret.getUniqueId()))
                    .findFirst().orElse(null);
            if (selectedSecret == null) {
                throw new DBWebException("Secret not found:" + selectedSecretId);
            }
            dataSourceContainer.setSelectedSharedCredentials(selectedSecret);
        }

        boolean oldSavePassword = dataSourceContainer.isSavePassword();
        try {
            boolean connect = dataSourceContainer.connect(webSession.getProgressMonitor(), true, false);
            if (connect) {
                webSession.addSessionEvent(
                    new WSDataSourceConnectEvent(
                        projectId,
                        connectionId,
                        webSession.getSessionId(),
                        webSession.getUserId()
                    )
                );
            }
        } catch (Exception e) {
            throw new DBWebException("Error connecting to database", e);
        } finally {
            dataSourceContainer.setSavePassword(oldSavePassword);
            connectionInfo.clearCache();
        }
        // Mark all specified network configs as saved
        boolean[] saveConfig = new boolean[1];

        if (networkCredentials != null) {
            networkCredentials.forEach(c -> {
                if (CommonUtils.toBoolean(c.isSavePassword())) {
                    DBWHandlerConfiguration handlerCfg = dataSourceContainer.getConnectionConfiguration()
                        .getHandler(c.getId());
                    if (handlerCfg != null &&
                        // check username param only for ssh config
                        !(CommonUtils.isEmpty(c.getUserName()) && CommonUtils.equalObjects(handlerCfg.getType(),
                            DBWHandlerType.TUNNEL))
                    ) {
                        WebDataSourceUtils.updateHandlerCredentials(handlerCfg, c);
                        handlerCfg.setSavePassword(true);
                        saveConfig[0] = true;
                    }
                }
            });
        }
        if (saveCredentials != null && saveCredentials) {
            // Save all passed credentials in the datasource container
            WebServiceUtils.saveAuthProperties(
                dataSourceContainer,
                dataSourceContainer.getConnectionConfiguration(),
                authProperties,
                true,
                sharedCredentials == null ? false : sharedCredentials
            );

            var project = dataSourceContainer.getProject();
            if (project.isUseSecretStorage()) {
                try {
                    dataSourceContainer.persistSecrets(
                        DBSSecretController.getProjectSecretController(dataSourceContainer.getProject())
                    );
                } catch (DBException e) {
                    throw new DBWebException("Failed to save credentials", e);
                }
            }

            WebDataSourceUtils.saveCredentialsInDataSource(connectionInfo,
                dataSourceContainer,
                dataSourceContainer.getConnectionConfiguration());
            saveConfig[0] = true;
        }
        if (WebServiceUtils.isGlobalProject(dataSourceContainer.getProject())) {
            // Do not flush config for global project (only admin can do it - CB-2415)
            saveConfig[0] = false;
        }
        if (saveConfig[0]) {
            dataSourceContainer.persistConfiguration();
        }

        return connectionInfo;
    }

    @Override
    public WebConnectionInfo createConnection(
        @NotNull WebSession webSession,
        @Nullable String projectId,
        @NotNull WebConnectionConfig connectionConfig
    ) throws DBWebException {
        WebSessionProjectImpl project = getProjectById(webSession, projectId);
        var rmProject = project.getRMProject();
        if (rmProject.getType() == RMProjectType.USER
            && !webSession.hasPermission(DBWConstants.PERMISSION_ADMIN)
            && !ServletAppUtils.getServletApplication().getAppConfiguration().isSupportsCustomConnections()
        ) {
            throw new DBWebException("New connection create is restricted by server configuration");
        }
        webSession.addInfoMessage("Create new connection");
        DBPDataSourceRegistry sessionRegistry = project.getDataSourceRegistry();

        // we don't need to save credentials for templates
        if (connectionConfig.isTemplate()) {
            connectionConfig.setSaveCredentials(false);
        }
        DBPDataSourceContainer newDataSource = WebServiceUtils.createConnectionFromConfig(connectionConfig,
            sessionRegistry);
        if (CommonUtils.isEmpty(newDataSource.getName())) {
            newDataSource.setName(CommonUtils.notNull(connectionConfig.getName(), "NewConnection"));
        }

        try {
            sessionRegistry.addDataSource(newDataSource);

            sessionRegistry.checkForErrors();
        } catch (DBException e) {
            sessionRegistry.removeDataSource(newDataSource);
            throw new DBWebException("Failed to create connection", e);
        }

        WebConnectionInfo connectionInfo = project.addConnection(newDataSource);
        webSession.addInfoMessage("New connection was created - " + WebServiceUtils.getConnectionContainerInfo(
            newDataSource));
        WebEventUtils.addDataSourceUpdatedEvent(
            webSession.getProjectById(projectId),
            webSession,
            connectionInfo.getId(),
            WSConstants.EventAction.CREATE,
            WSDataSourceProperty.CONFIGURATION
        );
        return connectionInfo;
    }

    @Override
    public WebConnectionInfo createDriDatasource(
            @NotNull WebSession webSession,
            @Nullable String projectId,
            @NotNull String connectionId,
             String dbname,
             String datasourceName,
             int businessId,
            String descs
    ) throws DBWebException {

        WebConnectionInfo connectionInfo = WebDataSourceUtils.getWebConnectionInfo(webSession, projectId, connectionId);
        try{
            //获取用户
            String userId = connectionInfo.getSession().getUserId();
            DBPDataSourceContainer dataSourceContainer = connectionInfo.getDataSourceContainer();
            DBPConnectionConfiguration connectionConfiguration = dataSourceContainer.getConnectionConfiguration();
            String userName = connectionConfiguration.getUserName();
            String password = connectionConfiguration.getUserPassword();
            String hostName = connectionConfiguration.getHostName();
            String hostPort = connectionConfiguration.getHostPort();
            String url = connectionConfiguration.getUrl();
            String driver = dataSourceContainer.getDriver().getName();

            //配置的额外参数
            Map<String, String> properties = connectionConfiguration.getProperties();
            String extraParams = "";
            if (url.contains("mysql")){
                properties.put("characterEncoding","UTF-8");
                properties.put("useSSL","false");
                properties.put("allowPublicKeyRetrieval","true");
                properties.put("zeroDateTimeBehavior","convertToNull");
                properties.put("useInformationSchema","true");
                extraParams = properties.entrySet()
                        .stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.joining("&"));
            }

            //该连接没有设置持久化密码，所以无法获取
            if (password == null ){
                throw new DBWebException("为DRI创建数据源失败！此连接需启用密码持久化功能!");
            }

            //jdbc:mysql://localhost:3306/ 格式
            if (url != null && url.endsWith("/")){
                url = url + dbname;
            } else {
                //jdbc:mysql://localhost:3306/indaas_log  格式
                url = url.substring(0,url.lastIndexOf("/")+1) + dbname;
            }

//            log.info(connectionInfo.getDataSourceContainer().getConnectionConfiguration().toString());
//            log.info("自定义接口调用-------" + dbname);
//            log.info("自定义接口调用-------" + businessId);
//            log.info("自定义接口调用--driver-----" + driver);
            log.info("数据源url:" + url);
//            log.info("自定义接口调用---userName----" + userName);
//            log.info("自定义接口调用----password---" + password);
//            log.info("自定义接口调用----datasourceName---" + datasourceName);
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("host",hostName);
            jsonObject.addProperty("port",hostPort);
            jsonObject.addProperty("dataBase",dbname);
            jsonObject.addProperty("username",userName);
            jsonObject.addProperty("password",password);
            jsonObject.addProperty("extraParams",extraParams);
            DriDatasourceService.createDriDatasource(datasourceName,jsonObject,url,userId,businessId,driver,descs);
        } catch(Exception e){
            throw new DBWebException("创建数据源失败！", e);
        }
        return connectionInfo;
    }

    @Override
    public WebConnectionInfo updateConnection(
        @NotNull WebSession webSession,
        @Nullable String projectId,
        @NotNull WebConnectionConfig config
    ) throws DBWebException {
        // Do not check for custom connection option. Already created connections can be edited.
        // Also template connections can be edited
//        if (!CBApplication.getInstance().getAppConfiguration().isSupportsCustomConnections()) {
//            throw new DBWebException("Connection edit is restricted by server configuration");
//        }

        WebConnectionInfo connectionInfo = WebDataSourceUtils.getWebConnectionInfo(webSession, projectId, config.getConnectionId());
        DBPDataSourceContainer dataSource = connectionInfo.getDataSourceContainer();
        webSession.addInfoMessage("Update connection - " + WebServiceUtils.getConnectionContainerInfo(dataSource));
        DataSourceDescriptor oldDataSource;
        oldDataSource = dataSource.getRegistry().createDataSource(dataSource);
        oldDataSource.setId(dataSource.getId());
        if (!CommonUtils.isEmpty(config.getName())) {
            dataSource.setName(config.getName());
        }

        if (config.getDescription() != null) {
            dataSource.setDescription(config.getDescription());
        }

        WebSessionProjectImpl project = getProjectById(webSession, projectId);
        DBPDataSourceRegistry sessionRegistry = project.getDataSourceRegistry();
        dataSource.setFolder(config.getFolder() != null ? sessionRegistry.getFolder(config.getFolder()) : null);
        if (config.isDefaultAutoCommit() != null) {
            dataSource.setDefaultAutoCommit(config.isDefaultAutoCommit());
        }
        WebServiceUtils.setConnectionConfiguration(dataSource.getDriver(),
            dataSource.getConnectionConfiguration(),
            config);

        // we should check that the config has changed but not check for password changes
        dataSource.setSharedCredentials(config.isSharedCredentials());
        dataSource.setSavePassword(config.isSaveCredentials());
        boolean sharedCredentials = dataSource.isSharedCredentials() || !dataSource.getProject()
            .isUseSecretStorage() && dataSource.isSavePassword();
        if (sharedCredentials) {
            //we must notify about the shared password change
            WebServiceUtils.saveAuthProperties(
                dataSource,
                dataSource.getConnectionConfiguration(),
                config.getCredentials(),
                config.isSaveCredentials(),
                config.isSharedCredentials()
            );
        }
        boolean sendEvent = !((DataSourceDescriptor) dataSource).equalSettings(oldDataSource);
        if (!sharedCredentials) {
            // secret controller is responsible for notification, password changes applied after checks
            WebServiceUtils.saveAuthProperties(
                dataSource,
                dataSource.getConnectionConfiguration(),
                config.getCredentials(),
                config.isSaveCredentials(),
                config.isSharedCredentials()
            );
        }

        WSDataSourceProperty property = getDatasourceEventProperty(oldDataSource, dataSource);

        try {
            sessionRegistry.updateDataSource(dataSource);
            sessionRegistry.checkForErrors();
        } catch (DBException e) {
            throw new DBWebException("Failed to update connection", e);
        }
        if (sendEvent) {
            WebEventUtils.addDataSourceUpdatedEvent(
                webSession.getProjectById(projectId),
                webSession,
                connectionInfo.getId(),
                WSConstants.EventAction.UPDATE,
                property
            );
        }
        return connectionInfo;
    }

    private WSDataSourceProperty getDatasourceEventProperty(
        DataSourceDescriptor oldDataSource,
        DBPDataSourceContainer dataSource
    ) {
        if (!oldDataSource.equalConfiguration((DataSourceDescriptor) dataSource)) {
            return WSDataSourceProperty.CONFIGURATION;
        }

        var nameChanged = !CommonUtils.equalObjects(oldDataSource.getName(), dataSource.getName());
        var descriptionChanged = !CommonUtils.equalObjects(oldDataSource.getDescription(), dataSource.getDescription());
        if (nameChanged && descriptionChanged) {
            return WSDataSourceProperty.CONFIGURATION;
        }

        return nameChanged ? WSDataSourceProperty.NAME : WSDataSourceProperty.CONFIGURATION;
    }

    @Override
    public boolean deleteConnection(
        @NotNull WebSession webSession, @Nullable String projectId, @NotNull String connectionId
    ) throws DBWebException {
        WebConnectionInfo connectionInfo = WebDataSourceUtils.getWebConnectionInfo(webSession, projectId, connectionId);
        if (connectionInfo.getDataSourceContainer().getProject() != getProjectById(webSession, projectId)) {
            throw new DBWebException("Global connection '" + connectionInfo.getName() + "' configuration cannot be deleted");
        }
        webSession.addInfoMessage("Delete connection - " +
            WebServiceUtils.getConnectionContainerInfo(connectionInfo.getDataSourceContainer()));
        closeAndDeleteConnection(webSession, projectId, connectionId, true);
        WebEventUtils.addDataSourceUpdatedEvent(
            webSession.getProjectById(projectId),
            webSession,
            connectionId,
            WSConstants.EventAction.DELETE,
            WSDataSourceProperty.CONFIGURATION
        );
        return true;
    }

    @Override
    @Deprecated
    public WebConnectionInfo createConnectionFromTemplate(
        @NotNull WebSession webSession,
        @NotNull String projectId,
        @NotNull String templateId,
        @Nullable String connectionName
    ) throws DBWebException {
        WebSessionProjectImpl project = getProjectById(webSession, projectId);
        DBPDataSourceRegistry templateRegistry = project.getDataSourceRegistry();
        DBPDataSourceContainer dataSourceTemplate = templateRegistry.getDataSource(templateId);
        if (dataSourceTemplate == null) {
            throw new DBWebException("Template data source '" + templateId + "' not found");
        }

        DBPDataSourceRegistry projectRegistry = webSession.getSingletonProject().getDataSourceRegistry();
        DBPDataSourceContainer newDataSource = projectRegistry.createDataSource(dataSourceTemplate);

        ServletApplication app = ServletAppUtils.getServletApplication();
        if (app instanceof WebApplication webApplication) {
            ((DataSourceDescriptor) newDataSource).setNavigatorSettings(
                webApplication.getAppConfiguration().getDefaultNavigatorSettings());
        }

        if (!CommonUtils.isEmpty(connectionName)) {
            newDataSource.setName(connectionName);
        }
        try {
            projectRegistry.addDataSource(newDataSource);

            projectRegistry.checkForErrors();
        } catch (DBException e) {
            throw new DBWebException(e.getMessage(), e);
        }

        return project.addConnection(newDataSource);
    }

    @Override
    public WebConnectionInfo copyConnectionFromNode(
        @NotNull WebSession webSession,
        @Nullable String projectId,
        @NotNull String nodePath,
        @NotNull WebConnectionConfig config
    ) throws DBWebException {
        try {
            DBNModel navigatorModel = webSession.getNavigatorModelOrThrow();
            WebSessionProjectImpl project = getProjectById(webSession, projectId);
            DBPDataSourceRegistry dataSourceRegistry = project.getDataSourceRegistry();

            DBNNode srcNode = navigatorModel.getNodeByPath(webSession.getProgressMonitor(), nodePath);
            if (srcNode == null) {
                throw new DBException("Node '" + nodePath + "' not found");
            }
            if (!(srcNode instanceof DBNDataSource)) {
                throw new DBException("Node '" + nodePath + "' is not a datasource node");
            }
            DBPDataSourceContainer dataSourceTemplate = ((DBNDataSource) srcNode).getDataSourceContainer();

            DBPDataSourceContainer newDataSource = dataSourceRegistry.createDataSource(dataSourceTemplate);

            ServletApplication app = ServletAppUtils.getServletApplication();
            if (app instanceof WebApplication webApplication) {
                ((DataSourceDescriptor) newDataSource).setNavigatorSettings(
                    webApplication.getAppConfiguration().getDefaultNavigatorSettings());
            }

            // Copy props from config
            if (!CommonUtils.isEmpty(config.getName())) {
                newDataSource.setName(config.getName());
            }
            if (!CommonUtils.isEmpty(config.getDescription())) {
                newDataSource.setDescription(config.getDescription());
            }

            dataSourceRegistry.addDataSource(newDataSource);

            dataSourceRegistry.checkForErrors();
            WebConnectionInfo connectionInfo = project.addConnection(newDataSource);
            WebEventUtils.addDataSourceUpdatedEvent(
                webSession.getProjectById(projectId),
                webSession,
                connectionInfo.getId(),
                WSConstants.EventAction.CREATE,
                WSDataSourceProperty.CONFIGURATION
            );
            return connectionInfo;
        } catch (DBException e) {
            throw new DBWebException("Error copying connection", e);
        }
    }

    @Override
    public WebConnectionInfo testConnection(
        @NotNull WebSession webSession, @Nullable String projectId, @NotNull WebConnectionConfig connectionConfig
    ) throws DBWebException {
        String connectionId = connectionConfig.getConnectionId();

        connectionConfig.setSaveCredentials(true); // It is used in createConnectionFromConfig

        DataSourceDescriptor dataSource = (DataSourceDescriptor) WebDataSourceUtils.getLocalOrGlobalDataSource(
            webSession, projectId, connectionId);

        WebProjectImpl project = getProjectById(webSession, projectId);
        DBPDataSourceRegistry sessionRegistry = project.getDataSourceRegistry();
        DataSourceDescriptor testDataSource;
        if (dataSource != null) {
            try {
                // Check that creds are saved to trigger secrets resolve
                dataSource.isCredentialsSaved();
            } catch (DBException e) {
                throw new DBWebException("Can't determine whether datasource credentials are saved", e);
            }

            testDataSource = (DataSourceDescriptor) dataSource.createCopy(dataSource.getRegistry());
            WebServiceUtils.setConnectionConfiguration(
                testDataSource.getDriver(),
                testDataSource.getConnectionConfiguration(),
                connectionConfig
            );
            if (connectionConfig.getSelectedSecretId() != null) {
                try {
                    dataSource.listSharedCredentials()
                        .stream()
                        .filter(secret -> connectionConfig.getSelectedSecretId().equals(secret.getSubjectId()))
                        .findFirst()
                        .ifPresent(testDataSource::setSelectedSharedCredentials);

                } catch (DBException e) {
                    throw new DBWebException("Failed to load secret value: " + connectionConfig.getSelectedSecretId());
                }
            }
            WebServiceUtils.saveAuthProperties(
                testDataSource,
                testDataSource.getConnectionConfiguration(),
                connectionConfig.getCredentials(),
                true,
                false,
                true
            );
        } else {
            testDataSource = (DataSourceDescriptor) WebServiceUtils.createConnectionFromConfig(connectionConfig,
                sessionRegistry);
        }
        webSession.provideAuthParameters(webSession.getProgressMonitor(),
            testDataSource,
            testDataSource.getConnectionConfiguration());
        testDataSource.setSavePassword(true); // We need for test to avoid password callback
        if (DataSourceDescriptor.class.isAssignableFrom(testDataSource.getClass())) {
            testDataSource.setAccessCheckRequired(!webSession.hasPermission(DBWConstants.PERMISSION_ADMIN));
        }
        try {
            ConnectionTestJob ct = new ConnectionTestJob(testDataSource, param -> {
            });
            ct.run(webSession.getProgressMonitor());
            if (ct.getConnectError() != null) {
                throw new DBWebException("Connection failed", ct.getConnectError());
            }
            WebConnectionInfo connectionInfo = new WebConnectionInfo(webSession, testDataSource);
            connectionInfo.setConnectError(ct.getConnectError());
            connectionInfo.setServerVersion(ct.getServerVersion());
            connectionInfo.setClientVersion(ct.getClientVersion());
            connectionInfo.setConnectTime(RuntimeUtils.formatExecutionTime(ct.getConnectTime()));
            return connectionInfo;
        } catch (DBException e) {
            throw new DBWebException("Error connecting to database", e);
        }
    }

    @Override
    public WebNetworkEndpointInfo testNetworkHandler(
        @NotNull WebSession webSession,
        @NotNull WebNetworkHandlerConfigInput nhConfig
    ) throws DBWebException {
        DBRProgressMonitor monitor = webSession.getProgressMonitor();
        monitor.beginTask("Instantiate SSH tunnel", 2);

        NetworkHandlerDescriptor handlerDescriptor = NetworkHandlerRegistry.getInstance()
            .getDescriptor(nhConfig.getId());
        if (handlerDescriptor == null) {
            throw new DBWebException("Network handler '" + nhConfig.getId() + "' not found");
        }
        try {
            DBWNetworkHandler handler = handlerDescriptor.createHandler(DBWNetworkHandler.class);
            if (handler instanceof DBWTunnel tunnel) {
                DBPConnectionConfiguration connectionConfig = new DBPConnectionConfiguration();
                connectionConfig.setHostName(DBConstants.HOST_LOCALHOST);
                connectionConfig.setHostPort(CommonUtils.toString(nhConfig.getProperties()
                    .get(DBWHandlerConfiguration.PROP_PORT)));
                try {
                    monitor.subTask("Initialize tunnel");

                    DBWHandlerConfiguration configuration = new DBWHandlerConfiguration(handlerDescriptor, null);
                    WebDataSourceUtils.updateHandlerConfig(configuration, nhConfig);
                    configuration.setSavePassword(true);
                    configuration.setEnabled(true);
                    tunnel.initializeHandler(monitor, configuration, connectionConfig);
                    monitor.worked(1);
                    // Get info
                    if (tunnel.getImplementation() instanceof SSHSession session) {
                        return new WebNetworkEndpointInfo(
                            "Connected",
                            session.getClientVersion(),
                            session.getServerVersion());
                    } else {
                        return new WebNetworkEndpointInfo("Connected");
                    }
                } finally {
                    monitor.subTask("Close tunnel");
                    tunnel.closeTunnel(monitor);
                    monitor.worked(1);
                }
            } else {
                return new WebNetworkEndpointInfo(nhConfig.getId() + " is not a tunnel");
            }
        } catch (Exception e) {
            throw new DBWebException("Error testing network handler endpoint", e);
        } finally {
            // Close it
            monitor.done();
        }
    }

    @Override
    public WebConnectionInfo closeConnection(
        @NotNull WebSession webSession, @Nullable String projectId, @NotNull String connectionId
    ) throws DBWebException {
        return closeAndDeleteConnection(webSession, projectId, connectionId, false);
    }

    @NotNull
    private WebConnectionInfo closeAndDeleteConnection(
        @NotNull WebSession webSession,
        @NotNull String projectId,
        @NotNull String connectionId,
        boolean forceDelete
    ) throws DBWebException {
        WebSessionProjectImpl project = getProjectById(webSession, projectId);
        WebConnectionInfo connectionInfo = project.getWebConnectionInfo(connectionId);

        DBPDataSourceContainer dataSourceContainer = connectionInfo.getDataSourceContainer();
        boolean disconnected = WebDataSourceUtils.disconnectDataSource(webSession, dataSourceContainer);
        if (forceDelete) {
            DBPDataSourceRegistry registry = project.getDataSourceRegistry();
            registry.removeDataSource(dataSourceContainer);
            try {
                registry.checkForErrors();
            } catch (DBException e) {
                try {
                    registry.addDataSource(dataSourceContainer);
                } catch (DBException ex) {
                    log.error("Error re-adding after delete attempt", e);
                }
                throw new DBWebException("Failed to delete connection", e);
            }
            project.removeConnection(dataSourceContainer);
        } else {
            // Just reset saved credentials
            connectionInfo.clearCache();
        }

        return connectionInfo;
    }

    // Projects
    @Override
    public List<WebProjectInfo> getProjects(@NotNull WebSession session) {
        var customConnectionsEnabled =
            ServletAppUtils.getServletApplication().getAppConfiguration().isSupportsCustomConnections()
                || SMUtils.isRMAdmin(session);
        return session.getAccessibleProjects().stream()
            .map(pr -> new WebProjectInfo(session, pr, customConnectionsEnabled))
            .collect(Collectors.toList());
    }
    //新增
    @Override
    public List<BusinessDomainVO> getBusinessInfo(@NotNull WebSession session) {
        List<BusinessDomainVO> businessInfo = null;
        return businessInfo;
    }

    // Folders
    @Override
    public WebConnectionFolderInfo createConnectionFolder(
        @NotNull WebSession session,
        @Nullable String projectId,
        @Nullable String parentPath,
        @NotNull String folderName
    ) throws DBWebException {
        WebConnectionFolderUtils.validateConnectionFolder(folderName);
        session.addInfoMessage("Create new folder");
        WebConnectionFolderInfo parentNode = null;
        try {
            if (parentPath != null) {
                parentNode = WebConnectionFolderUtils.getFolderInfo(session, projectId, parentPath);
            }
            WebProjectImpl project = getProjectById(session, projectId);
            DBPDataSourceRegistry sessionRegistry = project.getDataSourceRegistry();
            DBPDataSourceFolder newFolder = WebConnectionFolderUtils.createFolder(parentNode,
                folderName,
                sessionRegistry);
            WebConnectionFolderInfo folderInfo = new WebConnectionFolderInfo(session, newFolder);
            WebServiceUtils.updateConfigAndRefreshDatabases(session, projectId);
            WebEventUtils.addNavigatorNodeUpdatedEvent(
                session.getProjectById(projectId),
                session,
                DBNLocalFolder.makeLocalFolderItemPath(newFolder),
                WSConstants.EventAction.CREATE
            );
            return folderInfo;
        } catch (DBException e) {
            throw new DBWebException(e.getMessage(), e);
        }
    }

    @Override
    public WebConnectionFolderInfo renameConnectionFolder(
        @NotNull WebSession session,
        @Nullable String projectId,
        @NotNull String folderPath,
        @NotNull String newName
    ) throws DBWebException {
        WebConnectionFolderUtils.validateConnectionFolder(newName);
        WebConnectionFolderInfo folderInfo = WebConnectionFolderUtils.getFolderInfo(session, projectId, folderPath);
        var oldFolderNode = DBNLocalFolder.makeLocalFolderItemPath(folderInfo.getDataSourceFolder());
        folderInfo.getDataSourceFolder().setName(newName);
        var newFolderNode = DBNLocalFolder.makeLocalFolderItemPath(folderInfo.getDataSourceFolder());
        WebServiceUtils.updateConfigAndRefreshDatabases(session, projectId);
        WebEventUtils.addNavigatorNodeUpdatedEvent(
            session.getProjectById(projectId),
            session,
            oldFolderNode,
            WSConstants.EventAction.DELETE
        );
        WebEventUtils.addNavigatorNodeUpdatedEvent(
            session.getProjectById(projectId),
            session,
            newFolderNode,
            WSConstants.EventAction.CREATE
        );
        return folderInfo;
    }

    @Override
    public boolean deleteConnectionFolder(
        @NotNull WebSession session, @Nullable String projectId, @NotNull String folderPath
    ) throws DBWebException {
        try {
            WebConnectionFolderInfo folderInfo = WebConnectionFolderUtils.getFolderInfo(session, projectId, folderPath);
            DBPDataSourceFolder folder = folderInfo.getDataSourceFolder();
            WebProjectImpl project = getProjectById(session, projectId);
            if (folder.getDataSourceRegistry().getProject() != project) {
                throw new DBWebException("Global folder '" + folderInfo.getId() + "' cannot be deleted");
            }
            var folderNode = DBNLocalFolder.makeLocalFolderItemPath(folderInfo.getDataSourceFolder());
            session.addInfoMessage("Delete folder");
            DBPDataSourceRegistry sessionRegistry = project.getDataSourceRegistry();
            sessionRegistry.removeFolder(folderInfo.getDataSourceFolder(), false);
            WebServiceUtils.updateConfigAndRefreshDatabases(session, projectId);
            WebEventUtils.addNavigatorNodeUpdatedEvent(
                session.getProjectById(projectId),
                session,
                folderNode,
                WSConstants.EventAction.DELETE
            );
        } catch (DBException e) {
            throw new DBWebException(e.getMessage(), e);
        }
        return true;
    }

    @Override
    public WebConnectionInfo setConnectionNavigatorSettings(
        WebSession webSession, @Nullable String projectId, String id, DBNBrowseSettings settings
    ) throws DBWebException {
        WebConnectionInfo connectionInfo = WebDataSourceUtils.getWebConnectionInfo(webSession, projectId, id);
        DataSourceDescriptor dataSourceDescriptor = ((DataSourceDescriptor) connectionInfo.getDataSourceContainer());
        dataSourceDescriptor.setNavigatorSettings(settings);
        dataSourceDescriptor.persistConfiguration();
        WebEventUtils.addDataSourceUpdatedEvent(
            webSession.getProjectById(projectId),
            webSession,
            id,
            WSConstants.EventAction.UPDATE,
            WSDataSourceProperty.CONFIGURATION);
        return connectionInfo;
    }

    @Override
    public WebAsyncTaskInfo getAsyncTaskInfo(WebSession webSession, String taskId, Boolean removeOnFinish)
        throws DBWebException {
        return webSession.asyncTaskStatus(taskId, CommonUtils.toBoolean(removeOnFinish));
    }

    @Override
    public boolean cancelAsyncTask(WebSession webSession, String taskId) throws DBWebException {
        return webSession.asyncTaskCancel(taskId);
    }

    @Override
    public WebProductSettings getProductSettings(@NotNull WebSession webSession) {
        return new WebProductSettings(webSession, ProductSettingsRegistry.getInstance().getSettings());
    }

    private WebSessionProjectImpl getProjectById(WebSession webSession, String projectId) throws DBWebException {
        WebSessionProjectImpl project = webSession.getProjectById(projectId);
        if (project == null) {
            throw new DBWebException("Project '" + projectId + "' not found");
        }
        return project;
    }
}
