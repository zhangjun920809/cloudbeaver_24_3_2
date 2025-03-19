package io.cloudbeaver.indaas.erd.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.erd.model.ERDContext;
import org.jkiss.dbeaver.erd.model.ERDEntity;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;
import java.util.stream.Collectors;

public class WebERDEntityInfo {
    @NotNull
    private final ERDContext context;
    @NotNull
    private final ERDEntity entity;
    private final boolean fullInfo;

    public WebERDEntityInfo(@NotNull ERDContext context, @NotNull ERDEntity entity, boolean fullInfo) {
            this.context = context;
            this.entity = entity;
            this.fullInfo = fullInfo;
        }

        @Property
        public int getId() {
            return this.context.addElementInfo(this.entity);
        }

        @Property
        @NotNull
        public String getName() {
            return this.entity.getName();
        }

        @Property
        @Nullable
        public String getAlias() {
            return this.entity.getAlias();
        }

        @Property
        @Nullable
        public String getFqn() {
            DBSEntity dbsEntity = (DBSEntity)this.entity.getObject();
            if (dbsEntity != null && this.fullInfo && dbsEntity instanceof DBPQualifiedObject qualifiedObject) {
                return qualifiedObject.getFullyQualifiedName(DBPEvaluationContext.UI);
            } else {
                return null;
            }
        }

        @Property
        @Nullable
        public Integer getIconIndex() {
            return this.fullInfo ? this.context.getIconIndex(DBValueFormatting.getObjectImage((DBPObject)this.entity.getObject())) : null;
        }

        @Property
        @Nullable
        public String getNodeId() {
            DBNDatabaseNode node = this.context.getNavigatorModel().getNodeByObject(this.context.getMonitor(), (DBSObject)this.entity.getObject(), true);
            return node != null ? node.getNodeItemPath() : null;
        }

        @Property
        @Nullable
        public String getNodeUri() {
            DBNDatabaseNode node = this.context.getNavigatorModel().getNodeByObject(this.context.getMonitor(), (DBSObject)this.entity.getObject(), true);
            return node != null ? node.getNodeUri() : null;
        }

        @Property
        @NotNull
        public List<WebERDEntityAttributeInfo> getAttributes() {
            return (List)this.entity.getAttributes().stream().map((attr) -> {
                return new WebERDEntityAttributeInfo(this.context, attr, this.fullInfo);
            }).collect(Collectors.toList());
        }
}
