package io.cloudbeaver.license.service;

import io.cloudbeaver.license.utils.Utils;
import org.jkiss.dbeaver.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class WindowInfoAbstract extends ServerInfoAbstract {


    /* 变量 */
    private static final Log log = Log.getLog(WindowInfoAbstract.class);


    // 获取window平台IP
    @Override
    protected List<String> Ip() {
        List<String> result = null;
        List<InetAddress> inetAddresses = Utils.getLocalAllInetAddress();
        if(inetAddresses != null && inetAddresses.size() > 0){
            result = inetAddresses.stream().map(InetAddress::getHostAddress).distinct().map(String::toLowerCase).collect(Collectors.toList());
        }
        return result;
    }
    //从项目配置文件中获取ip
//    @Override
//    protected List<String> Ip() {
//        return ReadXMLFileUtil.getIp();
//    }
    // 获取window平台MAC
    @Override
    protected List<String> Mac() throws SocketException {

        java.util.Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
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
        List<String> unique = tmpMacList.stream().distinct().collect(Collectors.toList());
        return unique;

        /*List<String> result = null;
        //1. 获取所有网络接口
        List<InetAddress> inetAddresses = Utils.getLocalAllInetAddress();
        if(inetAddresses != null && inetAddresses.size() > 0){
            //2. 获取所有网络接口的Mac地址
            result = inetAddresses.stream().map(n->Utils.getMacByInetAddress(n)).distinct().collect(Collectors.toList());
        }
        return result;*/
    }


    // 使用WMIC获取window平台CPU序列号
    @Override
    protected String Cpu() {
        String serialNumber = "";
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("wmic cpu get processorid");
            process.getOutputStream().close();
            Scanner scanner = new Scanner(process.getInputStream());
            if(scanner.hasNext()){
                scanner.next();
            }
            if(scanner.hasNext()){
                serialNumber = scanner.next().trim();
            }
            scanner.close();
        } catch (IOException e) {
            log.error("Failed to get the window platform CPU serial number!",e);
        }
        return serialNumber;
    }


    // 使用WMIC获取window平台MAINBOARD序列号
    @Override
    protected String MainBoard() {
        String serialNumber = "";
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("wmic baseboard get serialnumber");
            process.getOutputStream().close();
            Scanner scanner = new Scanner(process.getInputStream());
            if(scanner.hasNext()){
                scanner.next();
            }
            if(scanner.hasNext()){
                serialNumber = scanner.next().trim();
            }
            scanner.close();
        } catch (IOException e) {
            log.error("The window platform MAINBOARD serial number failed!",e);
        }
        return serialNumber;
    }
}
