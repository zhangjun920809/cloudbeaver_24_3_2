package io.cloudbeaver.indaas.erd.model;

import io.cloudbeaver.DBWebException;
import io.cloudbeaver.indaas.erd.DBWServiceERD;
import io.cloudbeaver.indaas.erd.IndaasDiagram;
import io.cloudbeaver.model.session.WebSession;
import io.cloudbeaver.server.WebAppSessionManager;
import io.cloudbeaver.server.WebAppUtils;
import io.cloudbeaver.service.DBWBindingContext;
import io.cloudbeaver.service.WebServiceBindingBase;

import java.util.Collections;
import java.util.List;

public class WebServiceBindingERD extends WebServiceBindingBase<DBWServiceERD> {

    private static final String SCHEMA_FILE_NAME = "schema/service.erd.graphqls";

    public WebServiceBindingERD() {
        super(DBWServiceERD.class, new IndaasDiagram(), "schema/service.erd.graphqls");
    }

    //接口定义在此处会报错，所以功能移致WebServiceBindingCore类中实现
    public void bindWiring(DBWBindingContext model) throws DBWebException {
//        model.getQueryType()
//        .dataFetcher("generateEntityDiagram", env ->getService(env).generateEntityDiagram(getWebSession(env), env.getArgument("objectNodeIds")))
//        .dataFetcher("generateEntityDiagramExtended", env -> getService(env).generateEntityDiagramExtended(getWebSession(env), env.getArgument("objectNodeIds")));
    }
}