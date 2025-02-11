package io.cloudbeaver.service.security;

import com.google.gson.Gson;
import io.cloudbeaver.license.core.LicenseInstall;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import org.jkiss.dbeaver.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class LicenseService {

    private static final Log log = Log.getLog(LicenseService.class);
    private static final Gson gson = new Gson();
    /**
     * 获取license离过期的天数
     *
     * @param request
     * @return
     */
//    @Path("/license/expiredays")

    public void getExpiredays(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HashMap<String, Object> map = new HashMap<>();
        try {
            Long expiredays = LicenseUtils.expireDays;
            map.put("message", "");
            map.put("code", 200);
            map.put("data", expiredays);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(gson.toJson(map));
        } catch (Exception e) {
            map.put("message", "获取有效天数失败！");
            map.put("code", 500);
            map.put("data", null);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(gson.toJson(map));
        }
    }

    /**
     * 获取license安装信息
     *
     * @param request
     * @return
     */
//    @Path("/license/installmessage")
    public void getInstallmessage(HttpServletRequest request, HttpServletResponse response) throws IOException{
        HashMap<String, Object> map = new HashMap<>();
        try {
            HashMap<String, Object> installMessage = LicenseUtils.installMessage;
            Object message = installMessage.get("message");
            map.put("message", "获取license安装信息成功");
            map.put("code", 200);
            map.put("data", message);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(gson.toJson(map));
        } catch (Exception e) {
            map.put("message", "获取license安装信息失败！");
            map.put("code", 500);
            map.put("data", null);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(gson.toJson(map));
        }
    }
    /**
     * 获取服务器信息，包括cpu,ip 和当前用户，表盘数量
     *
     * @param request
     * @return
     */
//    @Path("/license/serverinfo")
    public void getServerinfo(HttpServletRequest request, HttpServletResponse response) throws IOException{
        HashMap<String, Object> map = new HashMap<>();
        try {
            HashMap<String, Object> serverInfo = LicenseUtils.getServerInfo();
            map.put("message", "获取服务器信息完成！");
            map.put("code", 200);
            map.put("data", serverInfo);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(gson.toJson(map));
        } catch (Exception e) {
            map.put("message", "获取服务器信息失败！");
            map.put("code", 500);
            map.put("data", null);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(gson.toJson(map));
        }
    }

    /**
     * 获取当前license包含的信息，包括license 中cpu,ip 和当前用户，表盘数量
     *
     * @param request
     * @return
     */
    public void getLicenseinfo(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HashMap<String, Object> map = new HashMap<>();
        try {
            HashMap<String, Object> serverInfo = LicenseUtils.getLicenseParams();
            map.put("message", "获取license信息完成！");
            map.put("code", 200);
            map.put("data", serverInfo);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(gson.toJson(map));
        } catch (Exception e) {
            map.put("message", "获取license信息失败！");
            map.put("code", 500);
            map.put("data", null);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(gson.toJson(map));
        }
    }
    /**
     * 上传并安装license
     *
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    public void installLicense(HttpServletRequest request, HttpServletResponse response){
        HashMap<String, Object> map = new HashMap<>();
        try{
            MultipartConfigElement multiPartConfig = new MultipartConfigElement("");
            request.setAttribute("org.eclipse.jetty.multipartConfig", multiPartConfig);
            // 2. 获取上传的文件
            Part filePart = request.getPart("file"); // "file" 是前端表单的字段名
            // 3. 获取文件信息
            String fileName = filePart.getSubmittedFileName();
            InputStream inputStream = filePart.getInputStream();
            log.info("fileName==="+fileName);
            // 4. 保存文件
            String licensePath = LicenseInstall.licensePath;
            log.info("licensePath==="+licensePath);
            try (FileOutputStream out = new FileOutputStream(new File(licensePath))) {
                int read;
                byte[] bytes = new byte[1024];
                while ((read = inputStream.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                }
            }

            LicenseUtils.installLisencNew();
            LicenseUtils.checkLicense();
//        // 5. 返回响应
            map.put("message", "安装成功！");
            map.put("code", 200);
            map.put("data", null);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(gson.toJson(map));
        }catch(Exception e){
            e.printStackTrace();
            log.error(e.getMessage());
        }

    }

    public static boolean graphsqlChecklicense(HttpServletRequest request, HttpServletResponse response){

        HashMap<String, Object> map = new HashMap<>();
        boolean installIsSuccess = LicenseUtils.installIsSuccess;
        boolean checkIssuccess = LicenseUtils.checkIssuccess;
        HashMap<String, Object> installMessage = LicenseUtils.installMessage;
        installMessage.forEach((k,v)->{
            System.out.println("key:"+k);
            System.out.println(v);
        });

        if (!installIsSuccess || !checkIssuccess){
            try{
                HashMap<String, Object> serverInfo = LicenseUtils.getServerInfo();

                map.put("message", "验证失败");
                map.put("code", 420);
                map.put("data", serverInfo);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(gson.toJson(serverInfo));
                response.setStatus(420);
                return false;
            } catch (Exception e){
                e.printStackTrace();
                log.error(e.getMessage());
                return false;
            }

        } else {
            return true;
        }
    }
}
