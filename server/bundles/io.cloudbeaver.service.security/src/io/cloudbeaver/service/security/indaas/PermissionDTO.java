package io.cloudbeaver.service.security.indaas;

/**
 * @author User
 * @date 2024/12/18 13:54
 */
public class PermissionDTO {
    private boolean hasEditorPermission;
    private boolean hasMonitorPermission;
    private boolean hasManagerPermission;
    private boolean hasassetcenterPermission;
    private boolean hasaModelcenterPermission;

    public boolean isHasaModelcenterPermission() {
        return hasaModelcenterPermission;
    }

    public void setHasaModelcenterPermission(boolean hasaModelcenterPermission) {
        this.hasaModelcenterPermission = hasaModelcenterPermission;
    }

    public boolean isHasEditorPermission() {
        return hasEditorPermission;
    }

    public void setHasEditorPermission(boolean hasEditorPermission) {
        this.hasEditorPermission = hasEditorPermission;
    }

    public boolean isHasMonitorPermission() {
        return hasMonitorPermission;
    }

    public void setHasMonitorPermission(boolean hasMonitorPermission) {
        this.hasMonitorPermission = hasMonitorPermission;
    }

    public boolean isHasManagerPermission() {
        return hasManagerPermission;
    }

    public void setHasManagerPermission(boolean hasManagerPermission) {
        this.hasManagerPermission = hasManagerPermission;
    }

    public boolean isHasassetcenterPermission() {
        return hasassetcenterPermission;
    }

    public void setHasassetcenterPermission(boolean hasassetcenterPermission) {
        this.hasassetcenterPermission = hasassetcenterPermission;
    }
}
