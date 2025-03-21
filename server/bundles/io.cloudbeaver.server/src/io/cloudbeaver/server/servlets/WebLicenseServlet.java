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
package io.cloudbeaver.server.servlets;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import io.cloudbeaver.DBWebException;
import io.cloudbeaver.WebProjectImpl;
import io.cloudbeaver.WebServiceUtils;
import io.cloudbeaver.WebSessionProjectImpl;
import io.cloudbeaver.model.WebConnectionFolderInfo;
import io.cloudbeaver.model.session.WebSession;
import io.cloudbeaver.server.CBConstants;
import io.cloudbeaver.server.WebAppUtils;
import io.cloudbeaver.service.security.LicenseService;
import io.cloudbeaver.service.security.indaas.DatabaseDto;
import io.cloudbeaver.service.security.indaas.DriDatasourceService;
import io.cloudbeaver.utils.WebConnectionFolderUtils;
import io.cloudbeaver.utils.WebEventUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.navigator.DBNLocalFolder;
import org.jkiss.dbeaver.model.rm.RMController;
import org.jkiss.dbeaver.model.websocket.WSConstants;
import org.jkiss.dbeaver.model.websocket.event.resource.WSResourceProperty;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet(urlPatterns = "/indaas/*")
@MultipartConfig
public class WebLicenseServlet extends DefaultServlet {

    private static final Log log = Log.getLog(WebLicenseServlet.class);
    public static final Gson gson = new Gson();
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        LicenseService licenseService = new LicenseService();
        String requestURI = request.getRequestURI();
        if(requestURI.endsWith("expiredays")){
            licenseService.getExpiredays(request,response);
        } else if (requestURI.endsWith("installmessage")){
            licenseService.getInstallmessage(request,response);
        } else if (requestURI.endsWith("serverinfo")){
            licenseService.getServerinfo(request,response);
        } else if (requestURI.endsWith("licenseinfo")){
            licenseService.getLicenseinfo(request,response);
        } else if (requestURI.endsWith("business")){
            DriDatasourceService.getBusinessInfo(request,response);
        } else {
            response.sendError(404, "not found");
        }
    }

    public static void main(String[] args) {

    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        try {

                String requestURI = request.getRequestURI();
                if(requestURI.endsWith("syncbusiness")){
                    WebSession webSession = WebAppUtils.getWebApplication().getSessionManager().getWebSession(request,response);
                    String userId = webSession.getUserId();
                    RMController rmController = webSession.getRmController();
                    if (userId != null){
                        Map<String, String> stringStringMap = DriDatasourceService.flattenTree();
                        stringStringMap.remove("全部业务域");
                        stringStringMap = replaceValue(stringStringMap,"全部业务域/","");
                        stringStringMap.forEach((key, value) ->{
                                    createConnectionFolder(webSession,"g_GlobalConfiguration",value,key);
//                                    System.out.println(key + ": " + value);
                                });

                        HashMap<String, Object> map = new HashMap<>();
                        map.put("message", "业务域目录同步完成！");
                        map.put("code", 200);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write(gson.toJson(map));
                    } else {
                        response.sendError(401, "未通过认证！");
                    }

                } else if(requestURI.endsWith("getdatasource")){
                    WebSession webSession = WebAppUtils.getWebApplication().getSessionManager().getWebSession(request,response);
                    String userId = webSession.getUserId();
                    if (userId != null){
                        List<DatabaseDto> stringStringMap = DriDatasourceService.queryAllDatabase();

                        HashMap<String, Object> map = new HashMap<>();
                        map.put("message", "请求完成！");
                        map.put("code", 200);
                        map.put("data", stringStringMap);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write(gson.toJson(map));
                    } else {
                        response.sendError(401, "未通过认证！");
                    }

                } else if (requestURI.endsWith("syncresource")){
                    WebSession webSession = WebAppUtils.getWebApplication().getSessionManager().getWebSession(request,response);
                    String userId = webSession.getUserId();
                    RMController rmController = webSession.getRmController();
                    if (userId != null){
                        Map<String, String> stringStringMap = DriDatasourceService.flattenTree();
                        stringStringMap.remove("全部业务域");
                        stringStringMap = resourceReplaceValue(stringStringMap,"全部业务域/","");
                        stringStringMap.forEach((key, value) ->{
                            try {
                                rmController.createResource("g_GlobalConfiguration",value,true);
                                WebEventUtils.addRmResourceUpdatedEvent(
                                        "g_GlobalConfiguration",
                                        webSession,
                                        value,
                                        WSConstants.EventAction.CREATE,
                                        WSResourceProperty.NAME);
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                            }
//                                    System.out.println(key + ": " + value);
                        });

                        HashMap<String, Object> map = new HashMap<>();
                        map.put("message", "业务域目录同步完成！");
                        map.put("code", 200);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        response.getWriter().write(gson.toJson(map));
                    } else {
                        response.sendError(401, "未通过认证！");
                    }
                }else if(requestURI.endsWith("upload")){

                    LicenseService licenseService = new LicenseService();
                    licenseService.installLicense(request,response);
                } else {
                        response.sendError(404, "not found");
                }

//            } else {
//                response.sendError(404, "认证未通过");
//            }
        } catch (DBWebException e) {
            throw new RuntimeException(e);
        }

    }

    public void createConnectionFolder(
            @NotNull WebSession session,
            @Nullable String projectId,
            @Nullable String parentPath,
            @NotNull String folderName
    ) {

        try {
            WebConnectionFolderUtils.validateConnectionFolder(folderName);
            session.addInfoMessage("Create new folder");
            WebConnectionFolderInfo parentNode = null;
            if (parentPath.equals(folderName)){
                parentPath = null;
            }
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
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private WebSessionProjectImpl getProjectById(WebSession webSession, String projectId) throws DBWebException {
        WebSessionProjectImpl project = webSession.getProjectById(projectId);
        if (project == null) {
            throw new DBWebException("Project '" + projectId + "' not found");
        }
        return project;
    }

    /**
     * 替换 Map 中所有 value 的指定字符串
     *
     * @param map       需要处理的 Map
     * @param target    需要替换的目标字符串
     * @param replacement 替换后的字符串
     */
    private static Map<String, String> replaceValue(Map<String, String> map, String target, String replacement) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String value = entry.getValue();
            String key = entry.getKey();
            if (value != null) {
                // 替换目标字符串
                String newValue = value.replace(target, replacement);
                newValue = newValue.replace("/"+key,"");

                entry.setValue(newValue);
            }
        }
        return map;
    }

    private static Map<String, String> resourceReplaceValue(Map<String, String> map, String target, String replacement) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String value = entry.getValue();
            String key = entry.getKey();
            if (value != null) {
                // 替换目标字符串
                String newValue = value.replace(target, replacement);
//                newValue = newValue.replace("/"+key,"");

                entry.setValue(newValue);
            }
        }
        return map;
    }


}

