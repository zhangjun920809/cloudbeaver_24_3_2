package io.cloudbeaver.license.entity;

import java.io.Serializable;
import java.util.List;

public class ServerInfo implements Serializable {

    // 允许的IP
    private List<String> ips;
    // 允许的MAC
    private List<String> macs;
    // 允许的CPU
    private String cpus;
    // 允许的主板
    private String Boards;


    public List<String> getIps() {
        return ips;
    }

    public void setIps(List<String> ips) {
        this.ips = ips;
    }

    public List<String> getMacs() {
        return macs;
    }

    public void setMacs(List<String> macs) {
        this.macs = macs;
    }

    public String getCpus() {
        return cpus;
    }

    public void setCpus(String cpus) {
        this.cpus = cpus;
    }

    public String getBoards() {
        return Boards;
    }

    public void setBoards(String boards) {
        Boards = boards;
    }

    @Override
    public String toString() {
        return "ServerInfo{" +
                "ips=" + ips +
                ", macs=" + macs +
                ", cpus='" + cpus + '\'' +
                ", Boards='" + Boards + '\'' +
                '}';
    }
}
