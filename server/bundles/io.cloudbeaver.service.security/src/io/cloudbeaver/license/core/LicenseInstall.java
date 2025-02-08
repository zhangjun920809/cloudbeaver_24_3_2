package io.cloudbeaver.license.core;


import de.schlichtherle.license.LicenseContent;
import de.schlichtherle.license.LicenseManager;
import io.cloudbeaver.license.entity.SubjectVerify;
import io.cloudbeaver.license.utils.Utils;
import org.jkiss.dbeaver.Log;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;

public class LicenseInstall {

    /* 成员变量 */
    private static final Log log = Log.getLog(LicenseInstall.class);


    private String publicKeysStorePath;
    private String licensePath;

    private String storePass = "hongyi2020";
    private String publicAlias = "publiccert";
    private String subject = "InDass";

    public LicenseInstall() {
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


    // 主要函数
    public LicenseContent init(){
//        log.info("开始安装证书");
        SubjectVerify subjectVerify = getSubjectVerify();
        //安装证书
        LicenseContent install = this.install(subjectVerify);
        return install;
    }

    public HashMap initMap(){
        SubjectVerify subjectVerify = getSubjectVerify();
        //安装证书
        HashMap map = this.installMap(subjectVerify);
        return map;
    }

    // 获取license参数
    public SubjectVerify getSubjectVerify(){
        SubjectVerify param = new SubjectVerify();
        param.setSubject(subject);
        param.setPublicAlias(publicAlias);
        param.setStorePass(storePass);
        param.setLicensePath(licensePath);
        param.setPublicKeysStorePath(publicKeysStorePath);
        return param;
    }

        public synchronized LicenseContent install(SubjectVerify param){
        LicenseContent result = null;
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try{
            LicenseManager licenseManager = CustomLicenseManager.getInstance(Utils.initLicenseParam(param));
            licenseManager.uninstall();
            result = licenseManager.install(new File(param.getLicensePath()));
            log.warn(MessageFormat.format("证书安装成功，证书有效期：{0} - {1}",format.format(result.getNotBefore()),format.format(result.getNotAfter())));
        }catch (Exception e){
            e.printStackTrace();
            log.error("证书安装失败！");
        }
        return result;
    }

    // 安装证书操作,为了适配其他项目，新增一个返回map的安装方法，不影响原有逻辑
    public synchronized HashMap installMap(SubjectVerify param){
        LicenseContent result = null;
        HashMap<String, Object> map = new HashMap<>();
        String message = "";
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try{
            LicenseManager licenseManager =CustomLicenseManager.getInstance(Utils.initLicenseParam(param));
            licenseManager.uninstall();
            result = licenseManager.install(new File(param.getLicensePath()));
            log.warn(MessageFormat.format("证书安装成功，证书有效期：{0} - {1}",format.format(result.getNotBefore()),format.format(result.getNotAfter())));
//            System.out.println(MessageFormat.format("Certificate installed successfully, certificate validity period：{0} - {1}",format.format(result.getNotBefore()),format.format(result.getNotAfter())));
            message = "安装成功";
        }catch (Exception e){
//            e.printStackTrace();
            log.error("证书安装失败！"+ e.getMessage());
            message = e.getMessage();
//            System.exit(0);
        }
        map.put("message",message);
        map.put("content",result);
        return map;
    }



}
