package io.cloudbeaver.server.jetty;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cloudbeaver.DBWebException;
import io.cloudbeaver.model.session.BaseWebSession;
import io.cloudbeaver.model.session.WebSession;
import io.cloudbeaver.model.session.WebSessionAuthProcessor;
import io.cloudbeaver.server.CBApplication;
import io.cloudbeaver.server.WebAppUtils;
import io.cloudbeaver.service.security.indaas.LoginPorcess;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.auth.SMAuthInfo;
import org.jkiss.dbeaver.model.security.SMController;
import org.jkiss.utils.IOUtils;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class IndaasInterseptor implements Filter {
    private static final Log log = Log.getLog(IndaasInterseptor.class);
    private static final Gson gson = new GsonBuilder().create();
    private final CBApplication<?> application;
    public IndaasInterseptor(CBApplication<?> application) {
        this.application = application;
    }
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    public static void main(String[] args) {

    }

    private boolean checkIfSkip(String operName) {
        return "authLogout".equalsIgnoreCase(operName);
    }
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        // 强制转换为 HttpServletResponse 类型
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletresponse = (HttpServletResponse) response;
        // 创建可缓存的请求包装对象
        boolean isSkip = false;
        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(httpServletRequest);
        try {
            // 从包装后的请求中读取缓存体（无需再次调用getReader）
            String postBody = wrappedRequest.getBody(); // 假设CachedBodyHttpServletRequest提供getBody方法
            JsonElement json = gson.fromJson(postBody, JsonElement.class);
            if (json instanceof JsonObject) {
                String operNameJSON = ((JsonObject) json).get("operationName").getAsString();
                // 检查需要跳过的操作名
//                log.info("operNameJSON===="+operNameJSON);
                isSkip = checkIfSkip(operNameJSON);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 跳过对指定请求的验证
        if (isSkip){
            chain.doFilter(wrappedRequest, response);
        } else {

            String driUserCookie = null;
            String mdcPid = null;
            String cookieHeader = httpServletRequest.getHeader("Cookie");
            if (cookieHeader != null) {
                cookieHeader = cookieHeader.trim();
                String[] cookies = cookieHeader.split(";");
                String tmpdriUser = Arrays.stream(cookies)
                        .filter(name -> name.contains("DRI-USER"))
                        .findFirst().orElse("");
                if (tmpdriUser != null){
                    String[] split = tmpdriUser.split("=");
                    if (split.length == 2){
                        driUserCookie = split[1];
                    }
                }
                // 获取mdc独立登录时的pid
                String tmpmdcPid = Arrays.stream(cookies)
                        .filter(name -> name.contains("pID"))
                        .findFirst().orElse("");
                if (tmpmdcPid != null){
                    String[] split = tmpmdcPid.split("=");
                    if (split.length == 2){
                        mdcPid = split[1];
                    }
                }
            }
            String pid = null;
            //mdc单独登录时，获取不到pid,所以pid为null时，需要再验证mdc单独设置的逻辑
            if (driUserCookie != null ){
                driUserCookie = URLDecoder.decode(driUserCookie);
                HashMap<String, String> hashMap = gson.fromJson(driUserCookie, new HashMap<String, String>().getClass());
                if (hashMap != null){
                    pid =  hashMap.get("pID");
                }
            } else {
                pid = mdcPid;
            }
//            log.info("driUserCookie:"+driUserCookie);
            // 获取携带的Cookie (dir项目返回的cookie格式不一致)
            try {
                if ( pid == null){
                    throw new Exception("验证失败，请重新登录！");
                }
                String modelcenter = LoginPorcess.authenticateDB(null, pid, "modelcenter");
                log.debug("modelcenter验证结果："+modelcenter);
                //  验证cb原有逻辑
                Map<String, Object> authParameters = new HashMap<>();
                WebSession webSession = WebAppUtils.getWebApplication().getSessionManager().getWebSession((HttpServletRequest) request, (HttpServletResponse) response);
                String userId = webSession.getUserId();
//                log.info("webSession.getUserId():"+userId);
                // userId == null，表示cb自身的登录没有进行，modelcenter 不等于 userId 表示dri和cb当前登录用户不是同一个，也需要重新执行
                if(userId == null || (!modelcenter.equalsIgnoreCase(userId))){
                    if (userId != null && (!modelcenter.equalsIgnoreCase(userId))){
                        // 先移除websession
                        WebAppUtils.getWebApplication().getSessionManager().closeSession((HttpServletRequest) request);
                    }
                    //再新建websession
                    SMController securityController = webSession.getSecurityController();
                    String currentSmSessionId = (webSession.getUser() == null || CBApplication.getInstance().isConfigurationMode())
                            ? null
                            : webSession.getUserContext().getSmSessionId();
                    SMAuthInfo smAuthInfo = securityController.authenticateSSO(
                            webSession.getSessionId(),
                            modelcenter,
                            currentSmSessionId,
                            webSession.getSessionParameters(),
                            WebSession.CB_SESSION_TYPE,
                            "local",
                            null,
                            authParameters,
                            false
                    );
                    var authProcessor = new WebSessionAuthProcessor(webSession, smAuthInfo, true);
                    authProcessor.authenticateSession();
                }

                chain.doFilter(wrappedRequest, response);
            } catch (Exception e) {
                log.error(e.getMessage());
                // 失败后，删除cb原有的websession
                try {
                    //删除sessionid对应的websession对象
                    WebSession webSession = WebAppUtils.getWebApplication().getSessionManager().getWebSession((HttpServletRequest) request, (HttpServletResponse) response);
                    String userId = webSession.getUserId();
                    if(userId != null ){
                        // 移除websession
                        WebAppUtils.getWebApplication().getSessionManager().closeSession((HttpServletRequest) request);
                    }
                } catch (Exception ex) {
                    log.error(e.getMessage());
                }
                chain.doFilter(wrappedRequest, response);
            }
        }

    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
