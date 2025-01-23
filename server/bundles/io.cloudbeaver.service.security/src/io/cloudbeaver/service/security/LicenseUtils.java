package io.cloudbeaver.service.security;

//import com.data.provider.core.LicenseInstall;
//import com.data.provider.core.LicenseVerify;
//import com.google.gson.Gson;
//import de.schlichtherle.license.LicenseContent;
//import org.jkiss.dbeaver.Log;
//
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.concurrent.ScheduledThreadPoolExecutor;
//import java.util.concurrent.TimeUnit;


public class LicenseUtils {
//    private static final Log logger = Log.getLog(LicenseUtils.class);
//    public static boolean installIsSuccess = false;
//    public static boolean checkIssuccess = false;
//    public static Date expireDate = null;
//    public static Long expireDays = null;
//    public static Gson gson =  new Gson();
//    public static HashMap<String,Object> installMessage = new HashMap<>();
//
//    public LicenseUtils() {
//        installMessage.put("message","未安装");
//        installMessage.put("result",false);
//    }
//    /**
//     *  安装license
//     */
//    public static void installLisence(){
//        LicenseInstall licenseInstall = new LicenseInstall();
////        LicenseVerify licenseVerify = new LicenseVerify();
//        LicenseContent init = licenseInstall.init();
//        if (init == null){
//            installIsSuccess = false;
//        } else {
//            installIsSuccess = true;
//        }
////        licenseVerify.getSubjectVerify();
//    }
//
//    public static boolean installLisencNew(){
//        LicenseInstall licenseInstall = new LicenseInstall();
//        HashMap hashMap = licenseInstall.initMap();
//        Object content = hashMap.get("content");
//        String message = (String)hashMap.get("message");
//
//        if (content == null){
//            installIsSuccess = false;
//        } else {
//            installIsSuccess = true;
//            LicenseContent lic = (LicenseContent)content;
//            //证书过期日期
//            expireDate = lic.getNotAfter();
//            Date date = new Date();
//            long diffInMillies = expireDate.getTime() - date.getTime();
//            //证书离过期天数
//            expireDays = TimeUnit.MILLISECONDS.toDays(diffInMillies);
////            logger.info("证书还有"+expireDays +"天过期");
//        }
//
//        if(message.contains("/license/license.lic")){
//            updateInstallMessage("您尚未安装License授权，请安装有效License授权。");
//        } else if(message.startsWith("安装成功")){
//            updateInstallMessage("安装成功!");
//        } else if(message.startsWith("设备验证失败")){
//            updateInstallMessage("您的License授权信息和该设备信息不符，请安装相符的License授权，或者部署至正确的服务器设备。");
//        } else if(message.startsWith("License Certificate has expired")){
//            updateInstallMessage("您的License授权已过期，如需继续使用，请安装有效的Liecnse。");
//        } else{
//            updateInstallMessage("证书无效");
//        }
//        return installIsSuccess;
//    }
//
//
//    private static void updateInstallMessage(String message){
//        installMessage.put("message",message);
//    }
//
//    /**
//     *  获取license中的参数
//     * @return
//     * @throws Exception
//     */
//    public static HashMap<String, Object> getLicenseParams() throws Exception{
//        HashMap<String, Object> hashMap =  new HashMap<>();
//        try{
//            //获取参数前，需先验证license是否安装成功
//            if(!installIsSuccess){
//                return hashMap;
//            }
//            LicenseVerify licenseVerify = new LicenseVerify();
//            String params = licenseVerify.getParams();
////            ;{:macs [42-3B-2D-E7-37-3A], :spare2 0, :dashboardCount 30, :ips [172.16.1.27], :spare1 0, :ipCheck false, :taskCount 20, :userCount 10, :cpuCheck false,
////                ; :macCheck false, :boards VMware-56 4d e8 0e 52 73 6a 95-b2 38 64 ac e9 57 5a 43, :boardCheck false, :cpus F2 06 03 00 FF FB CB 17}
//            if(params != null){
//                hashMap = gson.fromJson(params, hashMap.getClass());
//                hashMap.remove("ipCheck");
//                hashMap.remove("cpuCheck");
//                hashMap.remove("macCheck");
//                hashMap.remove("boardCheck");
//            }
//        }catch(Exception e){
//            e.printStackTrace();
//            throw new Exception(e.getMessage());
//        }
//        return hashMap;
//    }
//
//    /**
//     * 获取服务器信息，包括cpu,ip 和当前用户，表盘数量
//     * @return
//     */
//    public static HashMap<String, Object> getServerInfo() throws Exception{
//        HashMap<String, Object> result =  new HashMap<>();
//        LicenseVerify licenseVerify = new LicenseVerify();
//        String s = licenseVerify.serverInfo();
//        if(s != null){
//            result = gson.fromJson(s, result.getClass());
//        }
//        // 添加用户数量
//        result.put("userCount",getUserCount());
//        result.put("taskCount",getTaskCount());
//        return result;
//    }
//
//    /**
//     *  验证用户和任务数量
//     * @return
//     * @throws Exception
//     */
//
//    private static boolean checkUserAndTask() throws Exception{
//        long userCount = getUserCount();
//        long taskcount = getTaskCount();
//        HashMap<String, Object> licenseParams = getLicenseParams();
//        int userCountLicense = (int)licenseParams.get("userCount");
//        int taskCountLicense = (int)licenseParams.get("taskCount");
//
//        return (userCountLicense >= userCount && taskCountLicense >= taskcount);
//    }
//
//    /**
//     *  验证license,包括license本身 和用户数量
//     */
//    public static void checkLicense() {
//        try{
//            //为了应对随时上传的license,验证之前需先执行安装操作
//            boolean result = installLisencNew();
//            if(result){
//                LicenseVerify licenseVerify = new LicenseVerify();
//                HashMap checkRestult = licenseVerify.otherVerifyMap();
//                Object content = checkRestult.get("content");
//                String message = (String)checkRestult.get("message");
//                boolean checkUserTaskRestul = checkUserAndTask();
//                if (content != null && checkUserTaskRestul){
//                    checkIssuccess = true;
//                } else{
//                    checkIssuccess = false;
//                }
////            设备和过期 ，在安装时已经验证过
//                String message1 = (String)installMessage.get("message");
//                if(message1.startsWith("您尚未安装License授权")){
//                    installMessage.put("result",true);
//                } else{
//                    if(!checkUserTaskRestul){
//                        updateInstallMessage("您的License授权许可已超限，请升级您的License，或者登录管理用户和表盘数量以符合许可。");
//                    }
//                }
//                logger.info("验证结果："+checkIssuccess);
//            }
//
//        }catch(Exception e){
////            e.printStackTrace();
//            logger.error(e.getMessage());
//            checkIssuccess = false;
//        }
//
//    }
//
////    /**
////     *  验证是否是工作单元
////     */
////    private static boolean checkIsWorker(){
////        // 如果不为null。这表示不是worker节点
////        HikariDataSource indaasEditor = getDataSource("INDAAS_EDITOR");
////        HikariDataSource indaasUserManager = getDataSource("INDAAS_USER_MANAGER");
////        if(indaasEditor == null || indaasUserManager == null ){
////            return true;
////        } else{
////            return  false;
////        }
////    }
//
//    /**
//     *  定时验证
//     */
//    public static  void scheduleTask() throws Exception{
//        //校验证书
//        ScheduledThreadPoolExecutor scheduled = new ScheduledThreadPoolExecutor(2);
//        scheduled.scheduleAtFixedRate(new Runnable() {
//            @Override
//            public void run() {
//                //验证前需要执行安装操作，应对随时上传的license
//                checkLicense();
//            }
//        }, 20, 180, TimeUnit.SECONDS);
//    }
//
//    public static long getUserCount(){
//        PreparedStatement preparedStatement = null;
//        Connection connection = null;
//        long result = 0;
//        try {
////            connection = EmbeddedSecurityControllerFactory.getUserInstance().openConnection();
////            connection = getDataSource("INDAAS_USER_MANAGER").getConnection();
//            connection.setAutoCommit(true);
//            String sql = "select count(*) total from rdp_user";
//            preparedStatement = connection.prepareStatement(sql);
//            ResultSet resultSet = preparedStatement.executeQuery();
//            while (resultSet.next()){
//                result = resultSet.getLong("total");
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
////            throw  new Exception(e.getMessage());
//        } finally {
//            closePreparedStatement(preparedStatement);
//            closeConnection(connection);
//        }
//        return result;
//    }
//
//    public static long getTaskCount(){
//        PreparedStatement preparedStatement = null;
//        Connection connection = null;
//        long result = 0;
//        try {
////            connection = EmbeddedSecurityControllerFactory.getUserInstance().openConnection();
//            connection.setAutoCommit(true);
//            String sql = "select count(*) total from datatask";
//            preparedStatement = connection.prepareStatement(sql);
//            ResultSet resultSet = preparedStatement.executeQuery();
//            while (resultSet.next()){
//                result = resultSet.getLong("total");
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
////            throw  new Exception(e.getMessage());
//        } finally {
//            closePreparedStatement(preparedStatement);
//            closeConnection(connection);
//        }
//        return result;
//    }
//
//    //新增 2
//    public static  void closeConnection(Connection connection){
//        if (connection != null){
//            try {
//                connection.close();
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    //新增 2
//    public static  void closePreparedStatement(PreparedStatement preparedStatement){
//        if (preparedStatement != null){
//            try {
//                preparedStatement.close();
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//        }
//    }
}
