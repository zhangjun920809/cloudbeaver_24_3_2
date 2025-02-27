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
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        // 强制转换为 HttpServletResponse 类型
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletresponse = (HttpServletResponse) response;
        // 创建可缓存的请求包装对象
        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(httpServletRequest);
        boolean isSkip = false;
        if (isSkip){
            chain.doFilter(wrappedRequest, response);
        } else {

            String driUserCookie = null;
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

            }
            String pid = null;
            if (driUserCookie != null ){
                driUserCookie = URLDecoder.decode(driUserCookie);
                HashMap<String, String> hashMap = gson.fromJson(driUserCookie, new HashMap<String, String>().getClass());
                if (hashMap != null){
                    pid =  hashMap.get("pID");
                }
            }
            log.info("driUserCookie:"+driUserCookie);
            // 获取携带的Cookie (dir项目返回的cookie格式不一致)
            HashMap<String, Object> map = new HashMap<>();
            try {
                if ( pid == null){
                    throw new Exception("验证失败，请重新登录！");
                }
                String modelcenter = LoginPorcess.authenticateDB(null, pid, "modelcenter");
                log.info("modelcenter验证结果："+modelcenter);
                //  验证cb原有逻辑
                Map<String, Object> authParameters = new HashMap<>();
                WebSession webSession = WebAppUtils.getWebApplication().getSessionManager().getWebSession((HttpServletRequest) request, (HttpServletResponse) response);
                String userId = webSession.getUserId();
                log.info("webSession.getUserId():"+userId);
                // userId == null，表示cb自身的登录没有进行，modelcenter 不等于 userId 表示dri和cb当前登录用户不是同一个，也需要重新执行
                if(userId == null || (!modelcenter.equalsIgnoreCase(userId))){
                    // 先移除websession
                    WebAppUtils.getWebApplication().getSessionManager().closeSession((HttpServletRequest) request);
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
                e.printStackTrace();
//            ((HttpServletResponse) response).sendRedirect("/#/login");  //无法跳转
                // 失败后，删除cb原有的websession
                try {
                    //删除sessionid对应的websession对象
                    WebSession webSession = WebAppUtils.getWebApplication().getSessionManager().getWebSession((HttpServletRequest) request, (HttpServletResponse) response);
                    String userId = webSession.getUserId();
                    log.info("webSession.getUserId():"+userId);
                    if(userId != null ){
                        // 移除websession
                        WebAppUtils.getWebApplication().getSessionManager().closeSession((HttpServletRequest) request);
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
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
