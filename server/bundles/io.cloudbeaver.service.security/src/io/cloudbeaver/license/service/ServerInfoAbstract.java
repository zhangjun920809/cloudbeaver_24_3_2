package io.cloudbeaver.license.service;

import io.cloudbeaver.license.entity.ServerInfo;
import org.jkiss.dbeaver.Log;

import java.net.SocketException;
import java.util.List;

public abstract class ServerInfoAbstract {

    /* 变量 */
    private static final Log log = Log.getLog(ServerInfoAbstract.class);
    // IP
    protected abstract List<String> Ip();
    // MAC
    protected abstract List<String> Mac() throws SocketException;
    // CPU
    protected abstract String Cpu();
    // 主板
    protected abstract String MainBoard();


    // 获取服务器硬件信息
    public ServerInfo getServerInfo(){
        ServerInfo licenseParam = new ServerInfo();
        try {
            licenseParam.setIps(this.Ip());
            licenseParam.setMacs(this.Mac());
            licenseParam.setCpus(this.Cpu());
            licenseParam.setBoards(this.MainBoard());
        }catch (Exception e){
            log.error("Failed to obtain server hardware information!",e);
        }
        return licenseParam;
    }

}
