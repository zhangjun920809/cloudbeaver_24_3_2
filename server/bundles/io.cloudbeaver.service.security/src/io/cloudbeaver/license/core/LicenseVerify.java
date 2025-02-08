package io.cloudbeaver.license.core;

import com.data.provider.entity.CustomVerify;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.schlichtherle.license.LicenseContent;
import de.schlichtherle.license.LicenseManager;
import de.schlichtherle.license.NoLicenseInstalledException;
import io.cloudbeaver.license.entity.SubjectVerify;
import io.cloudbeaver.license.service.LinuxInfoAbstract;
import io.cloudbeaver.license.service.ServerInfoAbstract;
import io.cloudbeaver.license.service.WindowInfoAbstract;
import io.cloudbeaver.license.utils.Utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class LicenseVerify {
    private static Gson gson = new Gson();
    private String publicKeysStorePath ;
    private String licensePath;
    private String storePass = "hongyi2020";
    private String publicAlias = "publiccert";
    private String subject = "InDass";

    //获取配置文件路径
    public LicenseVerify () {
        // api，服务管理平台
        String  pro = System.getProperty("carbon.home");
        // dap平台源码启动时使用
        String  userDir = System.getProperty("user.dir");
        // dap 平台，以jar包启动时使用
        String  dapHome = System.getProperty("dap.home");
        //本地测试
        //String  pro = "C:\\Users\\User\\Desktop\\test";
        if (pro != null ) {
            if (pro.endsWith("Stream")) {
                int i = pro.lastIndexOf(File.separator);
                String substring = pro.substring(0, i);
                this.publicKeysStorePath = substring + File.separator+"license"+File.separator+"publicCerts.store";
                this.licensePath = substring + File.separator+"license"+File.separator+"license.lic";
            } else {
                //dri的license，统一放到上一级目录，如果上一级目录没有，则使用原有逻辑
                int i = pro.lastIndexOf(File.separator);
                String substring = pro.substring(0, i);
                this.publicKeysStorePath = substring + File.separator+"license"+File.separator+"publicCerts.store";
                this.licensePath = substring + File.separator+"license"+File.separator+"license.lic";
                String licenseDri = substring + File.separator+"license";
                Path licenseDriPath = Paths.get(licenseDri);
                //如果license目录不存在
                if(!Files.exists(licenseDriPath)){
                    this.publicKeysStorePath = pro + File.separator+"license"+File.separator+"publicCerts.store";
                    this.licensePath = pro + File.separator+"license"+File.separator+"license.lic";
                }
//                this.publicKeysStorePath = pro + File.separator+"license"+File.separator+"publicCerts.store";
//                this.licensePath = pro + File.separator+"license"+File.separator+"license.lic";
            }
        } else if (dapHome != null){
            this.publicKeysStorePath = dapHome + File.separator+"license"+File.separator+"publicCerts.store";
            this.licensePath = dapHome + File.separator+"license"+File.separator+"license.lic";
        } else  {
            if (userDir != null ){
                this.publicKeysStorePath = userDir + File.separator+"license"+File.separator+"publicCerts.store";
                this.licensePath = userDir + File.separator+"license"+File.separator+"license.lic";
            }
        }
    }

    /**
     *  定时校验证书
     */

    public void getSubjectVerify(){
        SubjectVerify sv = new SubjectVerify();
        sv.setSubject(subject);
        sv.setPublicAlias(publicAlias);
        sv.setStorePass(storePass);
        sv.setLicensePath(licensePath);
        sv.setPublicKeysStorePath(publicKeysStorePath);
        //校验证书
        ScheduledThreadPoolExecutor scheduled = new ScheduledThreadPoolExecutor(2);
        scheduled.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String date = sdf.format(new Date());
                try{
                    boolean flag = verify(sv);
                    if(!flag) {
                        System.exit(0);
                    }
                }catch (Exception e){
//                    log.error("证书巡检失败！",e);
                }finally {
                    System.out.println("check: " + date);
                }
            }
        }, 0, 1, TimeUnit.HOURS);
    }

    /**
     *  不使用定时任务验证license,在第三方的包中，再使用定时任务校验
     */
    public boolean otherVerify(){
        SubjectVerify sv = new SubjectVerify();
        sv.setSubject(subject);
        sv.setPublicAlias(publicAlias);
        sv.setStorePass(storePass);
        sv.setLicensePath(licensePath);
        sv.setPublicKeysStorePath(publicKeysStorePath);
        //校验证书
        return verify(sv);
    }

    /**
     *  不使用定时任务验证license,在第三方的包中，再使用定时任务校验
     */
    // 校验License证书，为了适配其他项目，新增一个返回map的验证方法，不影响原有逻辑
    public HashMap otherVerifyMap(){
        SubjectVerify sv = new SubjectVerify();
        sv.setSubject(subject);
        sv.setPublicAlias(publicAlias);
        sv.setStorePass(storePass);
        sv.setLicensePath(licensePath);
        sv.setPublicKeysStorePath(publicKeysStorePath);
        //校验证书
        return verifyMap(sv);
    }

    // 获取license的其他参数，用户后续验证dap,dri,
    public String  getParams(){
        SubjectVerify sv = new SubjectVerify();
        sv.setSubject(subject);
        sv.setPublicAlias(publicAlias);
        sv.setStorePass(storePass);
        sv.setLicensePath(licensePath);
        sv.setPublicKeysStorePath(publicKeysStorePath);
        HashMap<String, Object> result = new HashMap<>();
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            LicenseManager licenseManager = new CustomLicenseManager(Utils.initLicenseParam(sv));
            LicenseContent licenseContent = licenseManager.verify();
            CustomVerify expectedCheck = (CustomVerify) licenseContent.getExtra();
            result = gson.fromJson(gson.toJson(expectedCheck), new TypeToken<HashMap<String, Object>>() {}.getType());
            result.put("period",MessageFormat.format("：{0} - {1}",format.format(licenseContent.getNotBefore()),format.format(licenseContent.getNotAfter())));
//            return JSON.toJSONString(result, SerializerFeature.WriteMapNullValue);
            return gson.toJson(result);
        }catch (NoLicenseInstalledException e){
//            log.error("获取参数失败！",e);
            return "license未安装！";
        } catch (Exception e){
            return null;
        }

    }

    public String serverInfo(){
        // 获取操作系统类型
        String osName = System.getProperty("os.name").toLowerCase();
        // 根据当前操作系统获取相关系统参数
        ServerInfoAbstract serverInfoAbstract;
        serverInfoAbstract = osName.startsWith("windows") ? new WindowInfoAbstract() : new LinuxInfoAbstract();
//        return JSON.toJSONString(serverInfoAbstract.getServerInfo(),SerializerFeature.WriteMapNullValue);
        return gson.toJson(serverInfoAbstract.getServerInfo());
//        return serverInfoAbstract.getServerInfo();
    }
    // 校验License证书
    public boolean verify(SubjectVerify param){
        LicenseManager licenseManager = new CustomLicenseManager(Utils.initLicenseParam(param));
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            LicenseContent licenseContent = licenseManager.verify();
            return true;
        }catch (Exception e){
//            log.error("证书校验失败！",e);
            return false;
        }
    }

    // 校验License证书，为了适配其他项目，新增一个返回map的验证方法，不影响原有逻辑
    public HashMap verifyMap(SubjectVerify param){
        LicenseManager licenseManager = new CustomLicenseManager(Utils.initLicenseParam(param));
        LicenseContent licenseContent = null;
        HashMap<String, Object> map = new HashMap<>();
        String message = "";
        try {
            licenseContent = licenseManager.verify();
            message = "验证成功";
        }catch (NoLicenseInstalledException e){
//            log.error("证书未安装！",e);
            message = "license未安装";
        } catch (Exception e){
//            log.error("证书校验失败！",e);
            message = e.getMessage();
        }
        map.put("message",message);
        map.put("content",licenseContent);
        return map;
    }

}
