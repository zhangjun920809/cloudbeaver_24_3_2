package io.cloudbeaver.indaas.erd.model;

import io.cloudbeaver.model.session.WebSession;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.model.ERDContext;
import org.jkiss.dbeaver.erd.model.ERDDiagram;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class WebERDUtils {
    private static final Log log = Log.getLog(WebERDUtils.class);

    public WebERDUtils() {
    }

    public static Map<String, Object> serializeDiagram(WebSession webSession, ERDDiagram diagram, boolean fullInfo) throws DBException {
        Collection<DBPDataSourceContainer> dataSources = diagram.getDataSources();
        if (dataSources.isEmpty()) {
            return Collections.emptyMap();
        } else {
            ERDContext context = buildContext(webSession, diagram);
            return context == null ? Collections.emptyMap() : diagram.toMap(context, fullInfo);
        }
    }

    @Nullable
    public static ERDContext buildContext(WebSession webSession, ERDDiagram diagram) {
        Collection<DBPDataSourceContainer> dataSources = diagram.getDataSources();
        if (dataSources.isEmpty()) {
            return diagram.getRootObjectContainer() != null ? new ERDContext(webSession.getProgressMonitor(), diagram.getRootObjectContainer().getDataSource().getContainer(), webSession.getNavigatorModel()) : null;
        } else {
            return new ERDContext(webSession.getProgressMonitor(), (DBPDataSourceContainer)dataSources.iterator().next(), webSession.getNavigatorModel());
        }
    }
}
