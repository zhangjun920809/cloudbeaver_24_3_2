package io.cloudbeaver.service.security.indaas;

import io.cloudbeaver.service.security.EmbeddedSecurityControllerFactory;
import io.cloudbeaver.service.security.db.CBDatabase;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.security.user.SMUser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class IndaasService {
    public static final Log log = Log.getLog(LoginPorcess.class);
    public static HashMap<String,Object> getUserRole(String username){
        Connection connection = null;
        PreparedStatement preparedStatement =  null;
        List<String> rolelist = new ArrayList<>();
        HashMap<String,Object> reslutMap = new HashMap<>();
        try {
            CBDatabase dataSource = EmbeddedSecurityControllerFactory.getUserInstance();
//            LOG.info(dataSource.getJdbcUrl());
            connection = dataSource.openConnection();
            connection.setAutoCommit(true);
            String sql = "select role_name,data_level,table_permission from rdp_role  where role_id in ( select rur.role_id from rdp_user  as ru" +
                    " left join rdp_user_role as rur on  ru.user_id = rur.user_id where ru.user_name = ?)";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,username);
            ResultSet resultSet = preparedStatement.executeQuery();
            // 最大的表示最大的datalevel
            int maxLevel = 0;
            // 集合的元素最终元素表示拥有的table权限
            HashSet<String> permission = new HashSet<>();
            while (resultSet.next()){
                String role_name = resultSet.getString("role_name");
                int dataLevel = resultSet.getInt("data_level");
                if (dataLevel > maxLevel){
                    maxLevel = dataLevel;
                }
                String tablePermission = resultSet.getString("table_permission");
                if (tablePermission != null){
                    String[] split = tablePermission.split(",");
                    for (int i = 0; i < split.length; i++) {
                        permission.add(split[i]);
                    }
                }

                //LOG.info("role_name:"+role_name);
                rolelist.add(role_name);
            }
            reslutMap.put("rolelist",rolelist);
            reslutMap.put("dataLevel",maxLevel);
            reslutMap.put("tablePermission",permission);
            return reslutMap;

        } catch (Exception e) {
            log.error(e.getMessage(),e);
            return null;
        } finally {
            closePreparedStatement(preparedStatement);
            closeConnection(connection);
        }
    }

    public static SMUser getUserById(String username) throws DBException{
        Connection connection = null;
        PreparedStatement preparedStatement =  null;
        SMUser user;
        try {
//            CBDatabase dataSource = EmbeddedSecurityControllerFactory.getUserInstance();
////            LOG.info(dataSource.getJdbcUrl());
//            connection = dataSource.openConnection();
//            connection.setAutoCommit(true);
//            String sql = "select role_name,data_level,table_permission from rdp_role  where role_id in ( select rur.role_id from rdp_user  as ru" +
//                    " left join rdp_user_role as rur on  ru.user_id = rur.user_id where ru.user_name = ?)";
//            preparedStatement = connection.prepareStatement(sql);
//            preparedStatement.setString(1,username);
//            ResultSet resultSet = preparedStatement.executeQuery();
            // 最大的表示最大的datalevel
            int maxLevel = 0;
            // 集合的元素最终元素表示拥有的table权限
            HashSet<String> permission = new HashSet<>();
            user =  new SMUser(
                    username,
                    true,
                    "admin",
                    true
            );
            List<Role> roleByUser = LoginPorcess.getRoleByUser(username);
            // 提取 name 属性集合
            String[] roleNames = roleByUser.stream()
                    .map(Role::getDisplayName) // 使用 Role 对象的 getName 方法
                    .toArray(String[]::new);
            user.setUserTeams(roleNames);
            return user;

        } catch (Exception e) {
            throw new DBCException("Error while searching credentials", e);
        } finally {
            closePreparedStatement(preparedStatement);
            closeConnection(connection);
        }
    }



//    public SMUser getUserById(String userId) throws DBException {
//        try (Connection dbCon = database.openConnection()) {
//            SMUser user;
//            try (PreparedStatement dbStat = dbCon.prepareStatement(
//                    database.normalizeTableNames(
//                            "SELECT U.USER_ID,U.IS_ACTIVE,U.DEFAULT_AUTH_ROLE,S.IS_SECRET_STORAGE FROM " +
//                                    "{table_prefix}CB_USER U, {table_prefix}CB_AUTH_SUBJECT S " +
//                                    "WHERE U.USER_ID=? AND U.USER_ID=S.SUBJECT_ID")
//            )) {
//                dbStat.setString(1, userId);
//                try (ResultSet dbResult = dbStat.executeQuery()) {
//                    if (dbResult.next()) {
//                        user = fetchUser(dbResult);
//                    } else {
//                        return null;
//                    }
//                }
//            }
//            readSubjectMetas(dbCon, user);
//            // Teams
//            try (PreparedStatement dbStat = dbCon.prepareStatement(
//                    database.normalizeTableNames("SELECT TEAM_ID FROM {table_prefix}CB_USER_TEAM WHERE USER_ID=?"))
//            ) {
//                String defaultUserTeam = getDefaultUserTeam();
//                dbStat.setString(1, userId);
//                try (ResultSet dbResult = dbStat.executeQuery()) {
//                    Set<String> teamIDs = new LinkedHashSet<>();
//                    while (dbResult.next()) {
//                        teamIDs.add(dbResult.getString(1));
//                    }
//                    teamIDs.add(defaultUserTeam);
//                    user.setUserTeams(teamIDs.toArray(new String[0]));
//                }
//            }
//            return user;
//        } catch (SQLException e) {
//            throw new DBCException("Error while searching credentials", e);
//        }
//    }

    //新增 2
    public static   void closePreparedStatement(PreparedStatement preparedStatement){
        if (preparedStatement != null){
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    //新增 2
    public static  void closeConnection(Connection connection){
        if (connection != null){
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
