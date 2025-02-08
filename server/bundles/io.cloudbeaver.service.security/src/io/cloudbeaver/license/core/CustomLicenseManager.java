package io.cloudbeaver.license.core;

import com.data.provider.entity.CustomVerify;
import de.schlichtherle.license.*;
import de.schlichtherle.xml.GenericCertificate;
import io.cloudbeaver.license.entity.ServerInfo;
import io.cloudbeaver.license.service.LinuxInfoAbstract;
import io.cloudbeaver.license.service.WindowInfoAbstract;
import io.cloudbeaver.license.utils.Utils;
import org.jkiss.dbeaver.Log;

import java.beans.XMLDecoder;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class CustomLicenseManager extends LicenseManager {


    // 定义变量

    private static volatile LicenseManager instance;
    private static final Log log = Log.getLog(CustomLicenseManager.class);
    private static final String XML_CHARSET = "UTF-8";
    private static final int DEFAULT_BUFSIZE = 8 * 1024;

    // 有参数构造
    public CustomLicenseManager(LicenseParam param) {super(param);}


    // 单例设计模式生成对象
    public static LicenseManager getInstance(LicenseParam param){
        if(instance == null){
            synchronized (CustomLicenseManager.class){
                if(instance == null){
                    instance = new CustomLicenseManager(param);
                }
            }
        }
        return instance;
    }


    // 重写LicenseManager的create方法
    @Override
    protected synchronized byte[] create(LicenseContent content,LicenseNotary notary) throws Exception {
        this.initialize(content);
        // 验证自定义变量
        Utils.validateCreate(content);
        final GenericCertificate certificate = notary.sign(content);
        return getPrivacyGuard().cert2key(certificate);
    }


    // 重写LicenseManager的install方法
    @Override
    protected synchronized LicenseContent install(final byte[] key,final LicenseNotary notary) throws Exception {
        final GenericCertificate certificate = getPrivacyGuard().key2cert(key);
        notary.verify(certificate);
        final LicenseContent content = (LicenseContent)this.load(certificate.getEncoded());
        this.validate(content);
        setLicenseKey(key);
        setCertificate(certificate);
        return content;
    }


    // 重写LicenseManager的verify方法
    @Override
    protected synchronized LicenseContent verify(final LicenseNotary notary) throws Exception {
        final byte[] key = getLicenseKey();
        if (null == key){
            throw new NoLicenseInstalledException("I'm sorry!The matching digital verification certificate could not be found : " + getLicenseParam().getSubject());
        }
        GenericCertificate certificate = getPrivacyGuard().key2cert(key);
        notary.verify(certificate);
        final LicenseContent content = (LicenseContent)this.load(certificate.getEncoded());
        // 校验额外license参数
        this.validate(content);
        setCertificate(certificate);
        return content;
    }


    // 重写XMLDecoder解析XML
    private Object load(String encoded){
        BufferedInputStream inputStream = null;
        XMLDecoder decoder = null;
        try {
            inputStream = new BufferedInputStream(new ByteArrayInputStream(encoded.getBytes(XML_CHARSET)));
            decoder = new XMLDecoder(new BufferedInputStream(inputStream, DEFAULT_BUFSIZE),null,null);
            return decoder.readObject();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            try {
                if(decoder != null){
                    decoder.close();
                }
                if(inputStream != null){
                    inputStream.close();
                }
            } catch (Exception e) {
                log.error("XMLDecoder解析XML失败",e);
            }
        }
        return null;
    }


    // 重写validate方法，增加ip地址、mac地址、cpu序列号等其他信息的校验
    @Override
    protected synchronized void validate(final LicenseContent content) throws LicenseContentException {
        super.validate(content);
        CustomVerify expectedCheck = (CustomVerify) content.getExtra();
        String os = System.getProperty("os.name").toLowerCase();
        ServerInfo serverInfo = os.startsWith("windows") ? new WindowInfoAbstract().getServerInfo() : new LinuxInfoAbstract().getServerInfo();
        if(expectedCheck != null && serverInfo != null){
            if(expectedCheck.isIpCheck() && !checkIpAddress(expectedCheck.getIps(),serverInfo.getIps())){
                log.error("证书无效，当前服务器的IP不在授权范围内");
                throw new LicenseContentException("设备验证失败！IP");
            }
            if(expectedCheck.isMacCheck() && !checkIpAddress(expectedCheck.getMacs(),serverInfo.getMacs())){
                log.error("证书无效，当前服务器的Mac地址不在授权范围内");
                throw new LicenseContentException("设备验证失败！Macs");
            }
            if(expectedCheck.isBoardCheck() && !checkSerial(expectedCheck.getBoards(),serverInfo.getBoards())){
                log.error("证书无效，当前服务器主板序列号不在授权范围内");
                throw new LicenseContentException("设备验证失败！Boards");
            }
            if(expectedCheck.isCpuCheck() && !checkSerial(expectedCheck.getCpus(),serverInfo.getCpus())){
                log.error("证书无效，当前服务器的CPU序列号不在授权范围内");
                throw new LicenseContentException("设备验证失败！CPU");
            }
        }else{
            log.error("Unable to get server hardware information");
            throw new LicenseContentException("Unable to get server hardware information");
        }
    }


    // 校验当前服务器的IP/Mac地址是否在可被允许的IP范围内
    private boolean checkIpAddress(List<String> expectedList, List<String> serverList){
        if(expectedList != null && expectedList.size() > 0){
            if(serverList != null && serverList.size() > 0){
                for(String expected : expectedList){
                    if(serverList.contains(expected.trim())){
                        return true;
                    }
                }
            }
            return false;
        }else {
            return true;
        }
    }

    // 校验当前服务器硬件（主板、CPU等）序列号是否在可允许范围内
    private boolean checkSerial(String expectedSerial,String serverSerial){
//        if(StringUtil.isNotBlank(expectedSerial)){
        if(expectedSerial != null && expectedSerial.length() >0){
//            if(StringUtils.isNotBlank(serverSerial)){
            if(serverSerial != null && serverSerial.length() > 0){
                return expectedSerial.equals(serverSerial);
            }
            return false;
        }else{
            return true;
        }
    }

}
