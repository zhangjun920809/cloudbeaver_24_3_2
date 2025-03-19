package io.cloudbeaver.indaas.erd.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.erd.model.ERDAssociation;
import org.jkiss.dbeaver.erd.model.ERDContext;
import org.jkiss.dbeaver.erd.model.ERDDiagram;
import org.jkiss.dbeaver.erd.model.ERDElement;
import org.jkiss.dbeaver.model.meta.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WebERDDiagramInfo {
    @NotNull
    private final ERDDiagram diagram;
    @NotNull
    protected final ERDContext context;
    protected final boolean fullInfo;

    public WebERDDiagramInfo(@NotNull ERDDiagram diagram, @NotNull ERDContext context, boolean fullInfo) {
            this.diagram = diagram;
            this.context = context;
            this.fullInfo = fullInfo;
        }

        @Property
        @NotNull
        public List<WebERDEntityInfo> getEntities() {
            return (List)this.diagram.getEntities().stream().map((e) -> {
                return new WebERDEntityInfo(this.context, e, this.fullInfo);
            }).collect(Collectors.toList());
        }

        @Property
        @NotNull
        public List<WebERDAssociationInfo> getAssociations() {
            List<ERDElement<?>> allElements = new ArrayList();
            allElements.addAll(this.diagram.getEntities());
            allElements.addAll(this.diagram.getNotes());
            return (List)allElements.stream().flatMap((element) -> {
                return element.getAssociations().stream();
            }).map(this::createAssociation).collect(Collectors.toList());
        }

        @NotNull
        protected WebERDAssociationInfo createAssociation(ERDAssociation erdAssociation) {
            return new WebERDAssociationInfo(this.context, erdAssociation);
        }

        @Property
        public WebERDData getData() {
            return new WebERDData(this.context.getIcons());
        }

        public static record WebERDData(List<String> icons) {
            public WebERDData(List<String> icons) {
                this.icons = icons;
            }

            @Property
            public List<String> icons() {
                return this.icons;
            }
        }
}
