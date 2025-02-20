package io.cloudbeaver.model;


import java.util.List;

/**
 * @author User
 * @date 2023/1/4 16:39
 */
public class BusinessDomainVO extends BusinessDomainDto {
    private List<BusinessDomainVO> child;
    /*// 用于标识是否拥有该资源的权限
    private Integer mark;*/

   /* public Integer getMark() {
        return mark;
    }

    public void setMark(Integer mark) {
        this.mark = mark;
    }*/

    public List<BusinessDomainVO> getChild() {
        return child;
    }

    public void setChild(List<BusinessDomainVO> child) {
        this.child = child;
    }
}
