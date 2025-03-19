package io.cloudbeaver.indaas.erd;

import io.cloudbeaver.DBWebException;
import io.cloudbeaver.indaas.erd.model.WebERDDiagramInfo;
import io.cloudbeaver.indaas.erd.model.WebERDUtils;
import io.cloudbeaver.model.session.WebSession;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.model.ERDContentProviderDefault;
import org.jkiss.dbeaver.erd.model.ERDContext;
import org.jkiss.dbeaver.erd.model.ERDUtils;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.erd.model.ERDDiagram;

import java.util.*;

public class IndaasDiagram implements DBWServiceERD{

    private static final Log log = Log.getLog(IndaasDiagram.class);

    public IndaasDiagram() {
    }

    public Map<String, Object> generateEntityDiagram(WebSession webSession, List<String> nodeIds) throws DBWebException {
        try {
            ERDDiagram diagram = generateDiagram(webSession, nodeIds);
            return WebERDUtils.serializeDiagram(webSession, diagram, true);
        } catch (DBException var4) {
            throw new DBWebException("Couldn't generate ER diagram content", var4);
        }
    }

    public WebERDDiagramInfo generateEntityDiagramExtended(@NotNull WebSession webSession, @NotNull List<String> nodeIds) throws DBWebException {
        try {
            ERDDiagram diagram = generateDiagram(webSession, nodeIds);
            ERDContext erdContext = WebERDUtils.buildContext(webSession, diagram);
            if (erdContext == null) {
                throw new DBWebException("Error building ERD context");
            } else {
                return new WebERDDiagramInfo(diagram, erdContext, true);
            }
        } catch (DBException var5) {
            throw new DBWebException("Couldn't generate ER diagram content", var5);
        }
    }
    private static ERDDiagram generateDiagram(@NotNull WebSession webSession, @NotNull List<String> nodeIds) throws DBException {
        List<DBSObject> rootObjects = new ArrayList();
        DBRProgressMonitor monitor = webSession.getProgressMonitor();
        DBNModel navigatorModel = webSession.getNavigatorModelOrThrow();
        Iterator var6 = nodeIds.iterator();

        while(var6.hasNext()) {
            String nodeId = (String)var6.next();
            DBNNode node = navigatorModel.getNodeByPath(monitor, nodeId);
            if (node instanceof DBNDatabaseNode databaseNode) {
                rootObjects.add(databaseNode.getObject());
            }
        }

        ERDDiagram diagram = new ERDDiagram((DBSObject)null, "Web ERD", new ERDContentProviderDefault());

        Object root;
        for(Iterator var11 = rootObjects.iterator(); var11.hasNext(); diagram.fillEntities(monitor, ERDUtils.collectDatabaseTables(monitor, (DBSObject)root, diagram, true, false), (DBSObject)root)) {
            root = (DBSObject)var11.next();
            if (root instanceof DBPDataSourceContainer dataSourceContainer) {
                if (!dataSourceContainer.isConnected()) {
                    dataSourceContainer.connect(monitor, true, true);
                }

                root = ((DBSObject)root).getDataSource();
            }
        }

        Object var13;
        if (rootObjects.size() == 1 && (var13 = rootObjects.get(0)) instanceof DBSObjectContainer) {
            DBSObjectContainer objectContainer = (DBSObjectContainer)var13;
            diagram.setRootObjectContainer(objectContainer);
        }

        return diagram;
    }
    private static ERDDiagram generateDiagram_2(
            @NotNull WebSession webSession,
            @NotNull List<String> nodeIds) throws DBException
    {
        final List<DBSObject> rootObjects = new ArrayList<>();
        final DBRProgressMonitor monitor = webSession.getProgressMonitor();
        final DBNModel navigatorModel = webSession.getNavigatorModelOrThrow();

        // 收集根节点对象
        for (String nodeId : nodeIds) {
            final DBNNode node = navigatorModel.getNodeByPath(monitor, nodeId);
            if (node instanceof DBNDatabaseNode databaseNode) {
                rootObjects.add(databaseNode.getObject());
            }
        }

        // 创建 ERD 图表
        final ERDDiagram diagram = new ERDDiagram(null, "Web ERD", new ERDContentProviderDefault());

        // 填充实体
        for (DBSObject root : rootObjects) {
            DBSObject effectiveRoot = root;

            // 确保数据源连接
            if (effectiveRoot instanceof DBPDataSourceContainer dataSourceContainer) {
                if (!dataSourceContainer.isConnected()) {
                    dataSourceContainer.connect(monitor, true, true);
                }
                effectiveRoot = dataSourceContainer.getDataSource();
            }

            // 收集并填充表
            final Collection<? extends DBSEntity> tables = ERDUtils.collectDatabaseTables(
                    monitor,
                    effectiveRoot,
                    diagram,
                    true,   // 可能表示 includeRecursive
                    false   // 可能表示 excludeViews
            );
            diagram.fillEntities(monitor, (Collection<DBSEntity>)tables, effectiveRoot);
        }

        // 设置根容器（如果是单个容器对象）
        if (rootObjects.size() == 1) {
            final DBSObject firstRoot = rootObjects.get(0);
            if (firstRoot instanceof DBSObjectContainer container) {
                diagram.setRootObjectContainer(container);
            }
        }

        return diagram;
    }
}
