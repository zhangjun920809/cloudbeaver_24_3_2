package io.cloudbeaver.service.security.indaas;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.cloudbeaver.service.security.EmbeddedSecurityControllerFactory;
import io.cloudbeaver.service.security.db.CBDatabase;
import org.jkiss.dbeaver.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class DriDatasourceService {
    public static final Log log = Log.getLog(DriDatasourceService.class);
    public static final Gson gson = new Gson();

    public static void createDriDatasource(String datasourceName, JsonObject jsonObject, String url,String user){
        Connection connection = null;
        PreparedStatement preparedStatement =  null;
        List<String> rolelist = new ArrayList<>();
        try {
            CBDatabase dataSource = EmbeddedSecurityControllerFactory.getDbDbInstance();
            connection = dataSource.openConnection();
            String sql =" insert into indaas_database (name,details,create_user,engine,status,jdbcurl,db_uuid) values(?,?,?,?,?,?,?)";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,datasourceName);
            preparedStatement.setString(2,gson.toJson(jsonObject));
            preparedStatement.setString(3,user);
            preparedStatement.setString(4,"mysql");
            preparedStatement.setString(5,"active");
            preparedStatement.setString(6,url);
//            preparedStatement.setString(7,databaseDto.getBusinessSource());
            preparedStatement.setString(7, UUID.randomUUID().toString());
            boolean result = preparedStatement.executeUpdate() > 0 ? true : false;
            log.info("数据源添加操作执行完毕---" + result);

        } catch (Exception e) {
            log.error(e.getMessage(),e);
        } finally {
            LoginPorcess.closePreparedStatement(preparedStatement);
            LoginPorcess.closeConnection(connection);
        }
    }
}
