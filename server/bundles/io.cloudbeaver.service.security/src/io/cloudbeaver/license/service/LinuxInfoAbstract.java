package io.cloudbeaver.license.service;

import io.cloudbeaver.license.utils.Utils;
import org.jkiss.dbeaver.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.List;
import java.util.stream.Collectors;

/*
 * @Author: tianyong
 * @Date: 2020/7/13 18:11
 * @Description: linux平台参数信息
 */
public class LinuxInfoAbstract extends ServerInfoAbstract {

    /* 变量 */
    private static final Log log = Log.getLog(LinuxInfoAbstract.class);

    //如果需要从carbon.xml和demployment.yaml中读取ip,请使用下面的 ‘从项目配置文件中读取ip’ 方法
    // 获取linux平台IP,
    @Override
    protected List<String> Ip() {
        List<String> result = null;
        List<InetAddress> inetAddresses = Utils.getLocalAllInetAddress();
        if(inetAddresses != null && inetAddresses.size() > 0){
            result = inetAddresses.stream().map(InetAddress::getHostAddress).distinct().map(String::toLowerCase).collect(Collectors.toList());
        }
        return result;
    }

   //从项目配置文件中读取ip
   // 如果需要获取服务器ip,请使用上面 ‘获取linux平台IP’ 的方法
//   @Override
//    protected List<String> Ip() {
//       return ReadXMLFileUtil.getIp();
//   }

    // 获取linux平台MAC
    @Override
    protected List<String> Mac() throws SocketException{
        /*java.util.Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
        StringBuilder sb = new StringBuilder();
        ArrayList<String> tmpMacList=new ArrayList<>();
        while(en.hasMoreElements()){
            NetworkInterface iface = en.nextElement();
            List<InterfaceAddress> addrs = iface.getInterfaceAddresses();
            for(InterfaceAddress addr : addrs) {
                InetAddress ip = addr.getAddress();
                NetworkInterface network = NetworkInterface.getByInetAddress(ip);
                if(network==null){continue;}
                byte[] mac = network.getHardwareAddress();
                if(mac==null){continue;}
                sb.delete( 0, sb.length() );
                for (int i = 0; i < mac.length; i++) {sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));}
                tmpMacList.add(sb.toString());
            }        }
        if(tmpMacList.size()<=0){return tmpMacList;}
        *//***去重，别忘了同一个网卡的ipv4,ipv6得到的mac都是一样的，肯定有重复，下面这段代码是。。流式处理***//*
        List<String> unique = tmpMacList.stream().distinct().collect(Collectors.toList());
        return unique;*/
        List<String> result = null;
        // 1. 获取所有网络接口
        List<InetAddress> inetAddresses = Utils.getLocalAllInetAddress();
        if(inetAddresses != null && inetAddresses.size() > 0){
            // 2. 获取所有网络接口的Mac地址
            result = inetAddresses.stream().map(n->Utils.getMacByInetAddress(n)).distinct().collect(Collectors.toList());
        }
        return result;
    }


    // 使用dmidecode命令获取linux平台CPU序列号
    @Override
    protected String Cpu() {
        String serialNumber = "";
        BufferedReader reader = null;
        String[] shell = {"/bin/bash","-c","dmidecode -t processor | grep 'ID' | awk -F ':' '{print $2}' | head -n 1"};
        try {
            Process process = Runtime.getRuntime().exec(shell);
            process.getOutputStream().close();
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine().trim();
//            if(StringUtils.isNotBlank(line)){
            if(line != null && line.length() > 0){
                serialNumber = line;
            }
            reader.close();
        } catch (Exception e) {
            log.error("Failed to get Linux platform CPU serial number!",e);
        }
        return serialNumber;
    }


    // 使用dmidecode命令获取linux平台MAINBOARD序列号
    @Override
    protected String MainBoard() {
        String serialNumber = "";
        Process process = null;
        String[] shell = {"/bin/bash","-c","dmidecode | grep 'Serial Number' | awk -F ':' '{print $2}' | head -n 1"};
        try {
            process = Runtime.getRuntime().exec(shell);
            process.getOutputStream().close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine().trim();
//            if(StringUtils.isNotBlank(line)){
            if(line != null && line.length() > 0){
                serialNumber = line;
            }
            reader.close();
        } catch (IOException e) {
            log.error("Gets the Linux platform MAINBOARD serial number!",e);
        }
        return serialNumber;
    }
}
