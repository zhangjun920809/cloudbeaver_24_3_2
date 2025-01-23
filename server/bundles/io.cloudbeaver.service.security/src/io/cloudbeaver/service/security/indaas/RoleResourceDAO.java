package io.cloudbeaver.service.security.indaas;

/**
 * @author User
 * @date 2022/2/10 13:39
 */
public class RoleResourceDAO {
    private int resourceId;
    private String resourceName;
    private String roleName;
//    private int type;
    private int parentId;
    private int sort;
    private String portals;
//    private String description;
//    private String resourceCode;

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public int getResourceId() {
        return resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

/*    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }*/


    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    public String getPortals() {
        return portals;
    }

    public void setPortals(String portals) {
        this.portals = portals;
    }

    /*public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResourceCode() {
        return resourceCode;
    }

    public void setResourceCode(String resourceCode) {
        this.resourceCode = resourceCode;
    }
*/
    @Override
    public String toString() {
        return "RoleResourceDAO{" +
                "resourceId=" + resourceId +
                ", resourceName='" + resourceName + '\'' +
                ", roleName='" + roleName + '\'' +
                ", parentId=" + parentId +
                ", portals='" + portals + '\'' +
                '}';
    }
}
