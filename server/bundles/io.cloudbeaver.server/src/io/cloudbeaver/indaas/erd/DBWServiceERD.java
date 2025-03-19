package io.cloudbeaver.indaas.erd;

import io.cloudbeaver.DBWebException;
import io.cloudbeaver.WebAction;
import io.cloudbeaver.indaas.erd.model.WebERDDiagramInfo;
import io.cloudbeaver.model.session.WebSession;
import io.cloudbeaver.service.DBWService;
import org.jkiss.code.NotNull;

import java.util.List;
import java.util.Map;

public interface DBWServiceERD extends DBWService {
    @WebAction
    Map<String, Object> generateEntityDiagram(WebSession var1, List<String> var2) throws DBWebException;

    WebERDDiagramInfo generateEntityDiagramExtended(@NotNull WebSession var1, @NotNull List<String> var2) throws DBWebException;
}
