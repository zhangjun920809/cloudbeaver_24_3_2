package io.cloudbeaver.indaas.erd.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.erd.model.ERDContext;
import org.jkiss.dbeaver.erd.model.ERDEntityAttribute;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.utils.CommonUtils;

public class WebERDEntityAttributeInfo {
    @NotNull
    private final ERDContext context;
    @NotNull
    private final ERDEntityAttribute attribute;
    private final boolean fullInfo;

    public WebERDEntityAttributeInfo(@NotNull ERDContext context, @NotNull ERDEntityAttribute attribute, boolean fullInfo) {
            this.context = context;
            this.attribute = attribute;
            this.fullInfo = fullInfo;
        }

        @Property
        @NotNull
        public String getName() {
            return this.attribute.getName();
        }

        @Property
        @Nullable
        public String getAlias() {
            return !CommonUtils.isEmpty(this.attribute.getAlias()) ? this.attribute.getAlias() : null;
        }

        @Property
        @Nullable
        public String getDataKind() {
            DBSEntityAttribute entityAttribute = (DBSEntityAttribute)this.attribute.getObject();
            return entityAttribute != null && this.fullInfo ? entityAttribute.getDataKind().name() : null;
        }

        @Property
        @Nullable
        public String getTypeName() {
            DBSEntityAttribute entityAttribute = (DBSEntityAttribute)this.attribute.getObject();
            return entityAttribute != null && this.fullInfo ? entityAttribute.getTypeName() : null;
        }

        @Property
        @Nullable
        public Boolean isOptional() {
            DBSEntityAttribute entityAttribute = (DBSEntityAttribute)this.attribute.getObject();
            return entityAttribute != null && this.fullInfo ? !entityAttribute.isRequired() : null;
        }

        @Property
        @Nullable
        public Integer getIconIndex() {
            DBSEntityAttribute entityAttribute = (DBSEntityAttribute)this.attribute.getObject();
            return entityAttribute != null && this.fullInfo ? this.context.getIconIndex(DBValueFormatting.getObjectImage(entityAttribute)) : null;
        }

        @Property
        @Nullable
        public String getFullTypeName() {
            DBSEntityAttribute entityAttribute = (DBSEntityAttribute)this.attribute.getObject();
            return entityAttribute != null && this.fullInfo ? entityAttribute.getFullTypeName() : null;
        }

        @Property
        @Nullable
        public String getDefaultValue() {
            DBSEntityAttribute entityAttribute = (DBSEntityAttribute)this.attribute.getObject();
            return entityAttribute != null && this.fullInfo ? entityAttribute.getDefaultValue() : null;
        }

        @Property
        @Nullable
        public String getDescription() {
            DBSEntityAttribute entityAttribute = (DBSEntityAttribute)this.attribute.getObject();
            return entityAttribute != null && this.fullInfo ? CommonUtils.nullIfEmpty(entityAttribute.getDescription()) : null;
        }

        @Property
        public boolean isChecked() {
            return this.attribute.isChecked();
        }

        @Property
        @Nullable
        public Boolean isInPrimaryKey() {
            return this.fullInfo ? this.attribute.isInPrimaryKey() : null;
        }

        @Property
        @Nullable
        public Boolean isInForeignKey() {
            return this.fullInfo ? this.attribute.isInForeignKey() : null;
        }
}
