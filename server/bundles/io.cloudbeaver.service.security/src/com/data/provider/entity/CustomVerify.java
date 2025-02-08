package com.data.provider.entity;

import java.io.Serializable;
import java.util.List;


public class CustomVerify implements Serializable {

    // 允许的IP
    private List<String> ips;
    // 允许的MAC
    private List<String> macs;
    // 允许的CPU
    private String cpus;
    // 允许的主板
    private String boards;
    // 是否认证IP
    private boolean ipCheck;
    // 是否认证MAC
    private boolean macCheck;
    // 是否认证CPU
    private boolean cpuCheck;
    // 是否认证主板
    private boolean boardCheck;
    //用户数量
    private int userCount;
    //任务数量
    private int taskCount;
    // 表盘数量
    private int dashboardCount;
    // 以下为备用参数，后续根据需求使用
    private int spare1;
    private int spare2;
    private String spare3;
    private String spare4;

    public int getUserCount() {
        return userCount;
    }

    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }

    public int getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
    }

    public int getDashboardCount() {
        return dashboardCount;
    }

    public void setDashboardCount(int dashboardCount) {
        this.dashboardCount = dashboardCount;
    }

    public int getSpare1() {
        return spare1;
    }

    public void setSpare1(int spare1) {
        this.spare1 = spare1;
    }

    public int getSpare2() {
        return spare2;
    }

    public void setSpare2(int spare2) {
        this.spare2 = spare2;
    }

    public String getSpare3() {
        return spare3;
    }

    public void setSpare3(String spare3) {
        this.spare3 = spare3;
    }

    public String getSpare4() {
        return spare4;
    }

    public void setSpare4(String spare4) {
        this.spare4 = spare4;
    }

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
        return boards;
    }

    public void setBoards(String boards) {
        this.boards = boards;
    }

    public boolean isIpCheck() {
        return ipCheck;
    }

    public void setIpCheck(boolean ipCheck) {
        this.ipCheck = ipCheck;
    }

    public boolean isMacCheck() {
        return macCheck;
    }

    public void setMacCheck(boolean macCheck) {
        this.macCheck = macCheck;
    }

    public boolean isCpuCheck() {
        return cpuCheck;
    }

    public void setCpuCheck(boolean cpuCheck) {
        this.cpuCheck = cpuCheck;
    }

    public boolean isBoardCheck() {
        return boardCheck;
    }

    public void setBoardCheck(boolean boardCheck) {
        this.boardCheck = boardCheck;
    }

}
