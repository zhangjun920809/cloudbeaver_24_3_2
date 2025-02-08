package io.cloudbeaver.license.utils;

import com.google.gson.Gson;
import de.schlichtherle.license.*;
import io.cloudbeaver.license.entity.CustomKeyStore;
import io.cloudbeaver.license.entity.SubjectVerify;
import org.jkiss.dbeaver.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.prefs.Preferences;

public class Utils {


    /* 变量 */
    private static final Log log = Log.getLog(Utils.class);
    private static  Gson gson = new Gson();
//    /* jackson对象 */
//    public static final ObjectMapper MAPPER = new ObjectMapper();


    // 校验生成证书的参数信息
    public static synchronized void validateCreate(final LicenseContent content) throws LicenseContentException {
        // 声明变量
        final Date now = new Date();
        final Date notBefore = content.getNotBefore();
        final Date notAfter = content.getNotAfter();
        final String consumerType = content.getConsumerType();

        if (null != notAfter && now.after(notAfter)){
            log.error("The certificate cannot expire earlier than the current time");
            throw new LicenseContentException("The certificate cannot expire earlier than the current time");
        }
        if (null != notBefore && null != notAfter && notAfter.before(notBefore)){
            log.error("The effective time of the certificate should not be later than the expiration time of the certificate");
            throw new LicenseContentException("The effective time of the certificate should not be later than the expiration time of the certificate");
        }
        if (null == consumerType){
            log.error("The user type cannot be empty");
            throw new LicenseContentException("The user type cannot be empty");
        }
    }


    // 初始化证书生成参数
    public static LicenseParam initLicenseParam(SubjectVerify param){
        Preferences preferences = Preferences.userNodeForPackage(SubjectVerify.class);
        CipherParam cipherParam = new DefaultCipherParam(param.getStorePass());
        KeyStoreParam publicStoreParam = new CustomKeyStore(SubjectVerify.class
                ,param.getPublicKeysStorePath()
                ,param.getPublicAlias()
                ,param.getStorePass()
                ,null);
        return new DefaultLicenseParam(param.getSubject()
                ,preferences
                ,publicStoreParam
                ,cipherParam);
    }


    // 获取当前服务器所有符合条件的InetAddress
    public static List<InetAddress> getLocalAllInetAddress(){
        List<InetAddress> result = new ArrayList<>(4);
        try {
            // 遍历所有的网络接口
            for (Enumeration networkInterfaces = NetworkInterface.getNetworkInterfaces(); networkInterfaces.hasMoreElements(); ) {
                NetworkInterface ni = (NetworkInterface) networkInterfaces.nextElement();
                // 在所有的接口下再遍历IP
                for (Enumeration addresses = ni.getInetAddresses(); addresses.hasMoreElements(); ) {
                    InetAddress address = (InetAddress) addresses.nextElement();
                    //排除LoopbackAddress、SiteLocalAddress、LinkLocalAddress、MulticastAddress类型的IP地址
                    if(!address.isLoopbackAddress() && !address.isLinkLocalAddress() && !address.isMulticastAddress()){
                        result.add(address);
                    }
                }
            }
        } catch (SocketException e) {
            log.error("Failed to get all eligible InetAddresses from the current server!",e);
        }
        return result;
    }


    // 获取某个网络地址对应的Mac地址
    public static String getMacByInetAddress(InetAddress inetAddr){
        try {
            byte[] mac = NetworkInterface.getByInetAddress(inetAddr).getHardwareAddress();
            StringBuffer sb = new StringBuffer();
            for(int i=0;i<mac.length;i++){
                if(i != 0) {
                    sb.append("-");
                }
                //将十六进制byte转化为字符串
                String temp = Integer.toHexString(mac[i] & 0xff);
                if(temp.length() == 1){
                    sb.append("0" + temp);
                }else{
                    sb.append(temp);
                }
            }
            return sb.toString().toUpperCase();
        } catch (SocketException e) {
            log.error("Failed to get the Mac address corresponding to a network address!",e);
        }
        return null;
    }

    /*
     * @Author: tianyong
     * @Date: 2020/7/21 16:19
     * @Description: 对象转换成json字符串
     */
    public static String objectToJson(Object data) {
        return gson.toJson(data);
//        return JSON.toJSONString(data);
    }



}
