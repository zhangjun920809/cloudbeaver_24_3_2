package io.cloudbeaver.indaas.erd.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.model.ERDAssociation;
import org.jkiss.dbeaver.erd.model.ERDContext;
import org.jkiss.dbeaver.erd.model.ERDEntityAttribute;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.stream.Collectors;

public class WebERDAssociationInfo {
    private static final Log log = Log.getLog(ERDAssociation.class);
    @NotNull
    protected final ERDContext context;
    @NotNull
    protected final ERDAssociation association;

    public WebERDAssociationInfo(@NotNull ERDContext context, @NotNull ERDAssociation association) {
        this.context = context;
        this.association = association;
    }

    @Property
    @NotNull
    public String getName() {
        return ((DBSEntityAssociation)this.association.getObject()).getName();
    }

    @Property
    @Nullable
    public String getFqn() {
        Object var2;
        if ((var2 = this.association.getObject()) instanceof DBPQualifiedObject) {
            DBPQualifiedObject qualifiedObject = (DBPQualifiedObject)var2;
            return qualifiedObject.getFullyQualifiedName(DBPEvaluationContext.UI);
        } else {
            return null;
        }
    }

    @Property
    @NotNull
    public String getType() {
        return ((DBSEntityAssociation)this.association.getObject()).getConstraintType().getId();
    }

    @Property
    @Nullable
    public Integer getPrimaryEntity() {
        int pkInfo = this.context.getElementInfo(this.association.getSourceEntity());
        if (pkInfo == -1) {
            log.error("Cannot find PK table '" + this.association.getSourceEntity().getName() + "' in info map");
            return null;
        } else {
            return pkInfo;
        }
    }

    @Property
    @Nullable
    public Integer getForeignEntity() {
        int fkInfo = this.context.getElementInfo(this.association.getTargetEntity());
        if (fkInfo == -1) {
            log.error("Cannot find FK table '" + this.association.getSourceEntity().getName() + "' in info map");
            return null;
        } else {
            return fkInfo;
        }
    }

    @Property
    @NotNull
    public List<String> getPrimaryAttributes() {
        List<ERDEntityAttribute> attrs = this.association.getSourceAttributes();
        return CommonUtils.isEmpty(attrs) ? List.of() : (List)attrs.stream().map(ERDEntityAttribute::getName).collect(Collectors.toList());
    }

    @Property
    @NotNull
    public List<String> getForeignAttributes() {
        List<ERDEntityAttribute> attrs = this.association.getTargetAttributes();
        return CommonUtils.isEmpty(attrs) ? List.of() : (List)attrs.stream().map(ERDEntityAttribute::getName).collect(Collectors.toList());
    }
}
