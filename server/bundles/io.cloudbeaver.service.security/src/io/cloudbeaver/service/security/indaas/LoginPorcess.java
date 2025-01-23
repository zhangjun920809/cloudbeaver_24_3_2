package io.cloudbeaver.service.security.indaas;

import com.google.gson.Gson;
import io.cloudbeaver.service.security.CBEmbeddedSecurityController;
import io.cloudbeaver.service.security.EmbeddedSecurityControllerFactory;
import io.cloudbeaver.service.security.db.CBDatabase;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.auth.AuthPropertyEncryption;
import org.jkiss.dbeaver.model.exec.DBCException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LoginPorcess {
    public static final Log log = Log.getLog(LoginPorcess.class);


    /**  模型中心初始化时，同时在dri中创建用户
     *
     * @param username
     * @param password
     */
    public static void createDriAdminuser(String username,String password) throws DBCException{
        Connection connection = null;
        PreparedStatement preparedStatement =  null;
        try {
            //此处密码未加密
//            log.info("createDriAdminuser==password=="+password);
            // 先检查用户在dri中是否已存在
            if (checkUserIsExsit(username)){
                throw new DBCException("用户在DRI中已存在！");
            }
            //进行两次md5编码
            String clientPasswordHash = AuthPropertyEncryption.hash.encrypt("", password).toLowerCase();
            String encrtptPassword = AuthPropertyEncryption.hash.encrypt("", clientPasswordHash).toLowerCase();
            CBDatabase dataSource = EmbeddedSecurityControllerFactory.getUserInstance();
            connection = dataSource.openConnection();
            connection.setAutoCommit(true);
            String sql = "insert into  rdp_user(user_name,user_password,is_active) values(?,?,?);";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,username);
            preparedStatement.setString(2,encrtptPassword);
            preparedStatement.setInt(3,1);
            int i = preparedStatement.executeUpdate();
//            log.info("preparedStatement.executeUpdate()===result:"+i);

            // 查询新增的用户id,(不使用jdbc返回主键的功能，不同数据库可能用法不一致)
            String querysql = "select user_id from rdp_user where user_name = ? ";
            preparedStatement = connection.prepareStatement(querysql);
            preparedStatement.setString(1,username);
            ResultSet resultSet = preparedStatement.executeQuery();
            Integer userId = 0;
            while (resultSet.next()){
                userId = resultSet.getInt("user_id");
            }
            if (userId == 0){
                throw new DBCException("获取DRI用户id出错");
            }
            // 保存用户-角色中间表数据（admin角色）
            Integer adminRoleId = getAdminRoleId();
            saveAdminRoleInfo(userId,adminRoleId);

        } catch (Exception e) {
            throw new DBCException(e.getMessage(), e);
        } finally {
            closePreparedStatement(preparedStatement);
            closeConnection(connection);
        }
    }

    /**
     *  验证用户在dri中是否已存在
     * @param username
     * @return  true 表示已存在
     */
    private static Boolean checkUserIsExsit(String username) throws DBCException{
        Connection connection = null;
        PreparedStatement preparedStatement =  null;
        try {
            CBDatabase dataSource = EmbeddedSecurityControllerFactory.getUserInstance();
            connection = dataSource.openConnection();
            connection.setAutoCommit(true);
            String sql = "select user_id,user_name,user_password,is_active from rdp_user where user_name=?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,username);
            ResultSet resultSet = preparedStatement.executeQuery();
            if(resultSet.next()){
                return true;
            } else{
                return false;
            }

        } catch (Exception e) {
            throw new DBCException(e.getMessage(), e);
        } finally {
            closePreparedStatement(preparedStatement);
            closeConnection(connection);
        }
    }
    //新增，获取角色
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

    //新增,根据用户名和密码查询用户
    public static String getUserInfo(String username,String password){
        Connection connection = null;
        PreparedStatement preparedStatement =  null;
        String user = null;
        try {
            CBDatabase dataSource = EmbeddedSecurityControllerFactory.getUserInstance();
//            LOG.info(dataSource.getJdbcUrl());
            connection = dataSource.openConnection();
            connection.setAutoCommit(true);
            String sql = "select user_id,user_name,user_password,is_active from rdp_user where user_name=? and user_password = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,username);
            preparedStatement.setString(2,password);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()){
                user = resultSet.getString("user_name");
            }
            return user;

        } catch (Exception e) {
            log.error(e.getMessage(),e);
            return user;
        } finally {
            closePreparedStatement(preparedStatement);
            closeConnection(connection);
        }
    }

    /**
     *   查询dri中管理角色id  （admin角色）
     * @return
     * @throws DBCException
     */
    private static Integer getAdminRoleId() throws DBCException{
        Connection connection = null;
        PreparedStatement preparedStatement =  null;
        Integer roleId = 0;
        try {
            CBDatabase dataSource = EmbeddedSecurityControllerFactory.getUserInstance();
            connection = dataSource.openConnection();
            connection.setAutoCommit(true);
            String sql = "select role_id from rdp_role where role_name=?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,"admin");
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()){
                roleId = resultSet.getInt("role_id");
            }
            if (roleId == 0){
                throw new DBCException("DRI中，管理角色不存在！");
            }
            return roleId;

        } catch (Exception e) {
            throw new DBCException(e.getMessage(),e);
        } finally {
            closePreparedStatement(preparedStatement);
            closeConnection(connection);
        }
    }

    /**
     *  保存用户-角色中间表数据 （admin角色和用户）
     * @param userId
     * @param roleId
     * @return
     * @throws DBCException
     */
    private static void saveAdminRoleInfo(Integer userId,Integer roleId) throws DBCException{
        Connection connection = null;
        PreparedStatement preparedStatement =  null;
        try {
            CBDatabase dataSource = EmbeddedSecurityControllerFactory.getUserInstance();
            connection = dataSource.openConnection();
            connection.setAutoCommit(true);
            String sql = "insert into rdp_user_role (user_id,role_id) values(?,?)";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setInt(1,userId);
            preparedStatement.setInt(2,roleId);
            Integer i = preparedStatement.executeUpdate();
            while (i == 0){
                throw new DBCException("DRI中，保存用户和角色信息出错！");
            }
        } catch (Exception e) {
            throw new DBCException(e.getMessage(),e);
        } finally {
            closePreparedStatement(preparedStatement);
            closeConnection(connection);
        }
    }

    //新增,根据用户名和密码查询用户

    /**
     *  模型中心用户在登录时，验证dri中是否存在 （同时验证用户名和密码）
     * @param username
     * @param password  CB 测password进行了一次MD5加密(大写)
     * @return
     */
    public static Boolean checkUser(String username,String password){
        Connection connection = null;
        PreparedStatement preparedStatement =  null;
        String user = null;
        Integer isActive = null;
        try {
            log.info(username);
            log.info(password);
            //转成小写后，再次MD5编码一次
            String clientPasswordHash = AuthPropertyEncryption.hash.encrypt("", password.toLowerCase()).toLowerCase();
            CBDatabase dataSource = EmbeddedSecurityControllerFactory.getUserInstance();
            connection = dataSource.openConnection();
            connection.setAutoCommit(true);
            String sql = "select user_id,user_name,user_password,is_active from rdp_user where user_name=? and user_password = ?";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,username);
            preparedStatement.setString(2,clientPasswordHash);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()){
                user = resultSet.getString("user_name");
                isActive = resultSet.getInt("is_active");
            }
            if(user != null && isActive != null){
                return true;
            } else {
                return false;
            }

        } catch (Exception e) {
            log.error(e.getMessage(),e);
            return false;
        } finally {
            closePreparedStatement(preparedStatement);
            closeConnection(connection);
        }
    }
    //新增,根据用户名和密码查询用户
    public static HashMap<String, String> getToken(String username){
        Connection connection = null;
        PreparedStatement preparedStatement =  null;
        HashMap<String, String> result = new HashMap<>();
        try {
            CBDatabase dataSource = EmbeddedSecurityControllerFactory.getUserInstance();
//            LOG.info(dataSource.getJdbcUrl());
            connection = dataSource.openConnection();
            connection.setAutoCommit(true);
            String sql = "select session_id,username,token,isvalid,refreshId,expiryTime from rdp_session where username=? and isvalid =1 order by create_at desc limit 1";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,username);
            ResultSet resultSet = preparedStatement.executeQuery();
            DateTimeFormatter formtter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            while (resultSet.next()){
                String user = resultSet.getString("username");
                String sessionid = resultSet.getString("session_id");
                String token = resultSet.getString("token");
                String refreshId = resultSet.getString("refreshId");
                String expiryTime = resultSet.getString("expiryTime");
                ZonedDateTime now = ZonedDateTime.now();
                ZoneId zoneId = ZoneId.of("Asia/Shanghai");
                LocalDateTime parseExpiryTime = LocalDateTime.parse(expiryTime, formtter);
                ZonedDateTime parseExpiryTimeZonedDateTime = parseExpiryTime.atZone(zoneId);
                //如果session有效
                if (parseExpiryTimeZonedDateTime.isAfter(now)) {
                    result.put("username",user);
                    result.put("sessionid",sessionid);
                    result.put("token",token);
                    result.put("refreshId",refreshId);
                }
                //重新生成token
                //this.sessionId = UUID.randomUUID();
//                    ZonedDateTime createdAt = ZonedDateTime.now(zoneId);
//                    int userValue = username.hashCode();
//                    LocalSession session = new LocalSession(userValue, username, createdAt.plusSeconds(86400),
//                            createdAt.plusSeconds(50400));
//                    // 获取当前用户拥有的门户权限,
//                    PermissionDTO permission = getPermission(username);
//                    saveSession(session,permissionDTO);

            }

        } catch (Exception e) {
            log.error(e.getMessage(),e);

        } finally {
            closePreparedStatement(preparedStatement);
            closeConnection(connection);
        }
        return result;
    }

    public static PermissionDTO getPermission(String username){
        PermissionDTO permissionDTO = new PermissionDTO();
        // 判断是否有admin角色
        boolean hashAdminRole = hashAdminRole(username);
        if(hashAdminRole){
            permissionDTO.setHasManagerPermission(true);
            permissionDTO.setHasassetcenterPermission(true);
            permissionDTO.setHasMonitorPermission(true);
            permissionDTO.setHasEditorPermission(true);
            permissionDTO.setHasaModelcenterPermission(true);
        } else{
            List<Integer> resourceIdsByUserId = getResourceIdsByUserId(username);
            List<RoleResourceDAO> resourceByResouceId = getResourceByResouceId(resourceIdsByUserId);
            for (RoleResourceDAO roleResourceDAO : resourceByResouceId) {
                String resourceName = roleResourceDAO.getResourceName();
                if (resourceName.startsWith("管理中心")){
                    permissionDTO.setHasManagerPermission(true);
                } else if (resourceName.startsWith("资产中心")){
                    permissionDTO.setHasassetcenterPermission(true);
                } else if (resourceName.startsWith("监控中心")){
                    permissionDTO.setHasMonitorPermission(true);
                } else if (resourceName.startsWith("开发中心")){
                    permissionDTO.setHasEditorPermission(true);
                }else if (resourceName.startsWith("模型中心")){
                    permissionDTO.setHasaModelcenterPermission(true);
                }
            }
        }
        return permissionDTO;
    }
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
    public static void saveSession(LocalSession session ,PermissionDTO permission){
        Connection connection = null;
        PreparedStatement preparedStatement =  null;
        try {
            DateTimeFormatter formtter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            CBDatabase dataSource = EmbeddedSecurityControllerFactory.getUserInstance();
            connection = dataSource.openConnection();
            connection.setAutoCommit(true);
            String sql = "insert into  rdp_session(username,session_id,permission,userhash,expiryTime,refreshExpiryTime," +
                    "refreshId,token,isvalid) " +
                    "values(?,?,?,?,?,?,?,?,?) ";
            Gson gson1 = new Gson();
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,session.getUsername());
            preparedStatement.setString(2,session.getSessionId().toString());
            preparedStatement.setString(3, gson1.toJson(permission));

            preparedStatement.setInt(4,session.getUserHash());
            preparedStatement.setString(5,session.getExpiryTime().format(formtter));
            preparedStatement.setString(6,session.getRefreshExpiryTime().format(formtter));
            preparedStatement.setString(7,session.getRefreshId().toString());
            String sessionid = session.getSessionId().toString();
            String token = sessionid.substring(0, sessionid.length() / 2);
            preparedStatement.setString(8,token);
            preparedStatement.setInt(9,1);

            preparedStatement.executeUpdate();

        } catch (Exception e) {
            log.error(e.getMessage(),e);
        } finally {
            closePreparedStatement(preparedStatement);
            closeConnection(connection);
        }
    }

    public static List<RoleResourceDAO> getResourceByResouceId(List<Integer> listIds){
        Connection connection = null;
        PreparedStatement preparedStatement =  null;
        ArrayList<RoleResourceDAO> resourceList = new ArrayList<>();
        try {
            CBDatabase dataSource = EmbeddedSecurityControllerFactory.getUserInstance();
            connection = dataSource.openConnection();
            connection.setAutoCommit(true);

            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("select *  from rdp_resource where res_id in ( ");
            for (int i = 0; i < listIds.size(); i++) {
                sqlBuilder.append("?");
                if (i != listIds.size() -1){
                    sqlBuilder.append(",");
                }
            }
            sqlBuilder.append(") order by sort");
            String sql = sqlBuilder.toString();
            preparedStatement = connection.prepareStatement(sql);
            for (int i = 0; i < listIds.size(); i++) {
                preparedStatement.setInt(i+1,listIds.get(i));
            }
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()){
                RoleResourceDAO roleResourceDAO = new RoleResourceDAO();
                int res_id = resultSet.getInt("res_id");
                String res_name = resultSet.getString("res_name");
                int parent_id = resultSet.getInt("parent_id");
                int sort = resultSet.getInt("sort");
                String portals = resultSet.getString("portals");

                roleResourceDAO.setResourceId(res_id);
                roleResourceDAO.setResourceName(res_name);
                roleResourceDAO.setPortals(portals);
                roleResourceDAO.setParentId(parent_id);
                roleResourceDAO.setSort(sort);
                resourceList.add(roleResourceDAO);
            }
            return resourceList;

        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        } finally {
            closePreparedStatement(preparedStatement);
            closeConnection(connection);
        }
    }

    /**
     *  根据用户获取拥有的资源ids (已去重)
     * @param userName
     * @return
     */
    public static List<Integer> getResourceIdsByUserId(String userName){
        Connection connection = null;
        PreparedStatement preparedStatement =  null;
        try {
            CBDatabase dataSource = EmbeddedSecurityControllerFactory.getUserInstance();
//            LOG.info(dataSource.getJdbcUrl());
            connection = dataSource.openConnection();
            connection.setAutoCommit(true);

            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            String sql = "";
            if (databaseProductName.contains("DM")){
                sql = "select wm_concat(DISTINCT(res_id))  as res_ids from rdp_role as rr1 RIGHT JOIN " +
                        "                    (select DISTINCT(create_time) as create_time,rr.res_id,rr.res_name,res_type,parent_id,portals,sort,res_description,res_code " +
                        "                    ,rrr.role_id from rdp_role_resource as rrr left join rdp_resource as rr on rrr.res_id= rr.res_id where rrr.role_id in " +
                        "                    (select role_id from rdp_user as ru left join rdp_user_role as rur on ru.user_id = rur.user_id where ru.user_name = ?)) " +
                        "                    as result on rr1.role_id = result.role_id";
            } else if (databaseProductName.contains("Postgre")){
                sql = "select STRING_AGG(DISTINCT(res_id))  as res_ids from rdp_role as rr1 RIGHT JOIN " +
                        "                    (select DISTINCT(create_time) as create_time,rr.res_id,rr.res_name,res_type,parent_id,portals,sort,res_description,res_code " +
                        "                    ,rrr.role_id from rdp_role_resource as rrr left join rdp_resource as rr on rrr.res_id= rr.res_id where rrr.role_id in " +
                        "                    (select role_id from rdp_user as ru left join rdp_user_role as rur on ru.user_id = rur.user_id where ru.user_name = ?)) " +
                        "                    as result on rr1.role_id = result.role_id";
            } else{
                sql = "select GROUP_CONCAT(DISTINCT(res_id))  as res_ids from rdp_role as rr1 RIGHT JOIN " +
                        "                    (select DISTINCT(create_time) as create_time,rr.res_id,rr.res_name,res_type,parent_id,portals,sort,res_description,res_code " +
                        "                    ,rrr.role_id from rdp_role_resource as rrr left join rdp_resource as rr on rrr.res_id= rr.res_id where rrr.role_id in " +
                        "                    (select role_id from rdp_user as ru left join rdp_user_role as rur on ru.user_id = rur.user_id where ru.user_name = ?)) " +
                        "                    as result on rr1.role_id = result.role_id";
            }

            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,userName);
            ResultSet resultSet = preparedStatement.executeQuery();

            ArrayList<Integer> resIdList = new ArrayList<>();
            while (resultSet.next()){
                String res_id = resultSet.getString("res_ids");
                String[] split = res_id.split(",");
                for (int i = 0; i < split.length; i++) {
                    resIdList.add(Integer.valueOf(split[i]));
                }

            }
            // 添加根节点 （parentId = -1）
            sql = "select res_id from rdp_resource where parent_id = -1";
            preparedStatement = connection.prepareStatement(sql);
            ResultSet resultSet1 = preparedStatement.executeQuery();
            int res_id = 0 ;
            while (resultSet1.next()){
                res_id = resultSet1.getInt("res_id");
                resIdList.add(res_id);
            }

            // resIdList.add(res_id);
            return resIdList;

        } catch (Exception e) {
            log.error(e.getMessage(),e);
            return null;
        } finally {
            closePreparedStatement(preparedStatement);
            closeConnection(connection);
        }
    }

    /**
     *  判断用户是否有admin角色
     * @param username
     * @return
     */
    public static boolean hashAdminRole(String username) {
        List<Role> userRoles = getUserRoles(username);
        for (Role role : userRoles) {
            if(role.getDisplayName().equalsIgnoreCase("admin")){
                return true;
            }
        }
        return false;
    }

//    public User getUser(String name) {
//        if (usersMap.containsKey(name)){
//            return usersMap.get(name);
//        } else {
//            List<Role> roles = getRoleByUser(name);
//            User user = new User(name, new HashMap<>(), roles);
//            //加入缓存
//            usersMap.put(name,user);
//            return user;
//        }
//    }

    public static List<Role> getUserRoles(String name) {
        List<Role> roles = getRoleByUser(name);
        User user = new User(name, new HashMap<>(), roles);
        return roles;
    }

    public static List<Role > getRoleByUser(String username){
        Connection connection = null;
        PreparedStatement preparedStatement =  null;
        List<Role> reslutList = new ArrayList<>();
        try {
            CBDatabase dataSource = EmbeddedSecurityControllerFactory.getUserInstance();
            connection = dataSource.openConnection();
            connection.setAutoCommit(true);
            String sql = "select role_id,role_name from rdp_role  where role_id in ( select rur.role_id from rdp_user  as ru" +
                    " left join rdp_user_role as rur on  ru.user_id = rur.user_id where ru.user_name = ?)";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,username);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()){
                String id = resultSet.getString("role_id");
                String role_name = resultSet.getString("role_name");
                //LOG.info("role_name:"+role_name);
                Role role = new Role(id, role_name);
                reslutList.add(role);
            }
            return reslutList;

        } catch (Exception e) {
            log.error(e.getMessage(),e);
            return null;
        } finally {
            closePreparedStatement(preparedStatement);
            closeConnection(connection);
        }
    }

    public static Set<String> getRoleByUserSet(String username){
        Connection connection = null;
        PreparedStatement preparedStatement =  null;
        Set<String> set = new HashSet<>();
        try {
            CBDatabase dataSource = EmbeddedSecurityControllerFactory.getUserInstance();
            connection = dataSource.openConnection();
            connection.setAutoCommit(true);
            String sql = "select role_id,role_name from rdp_role  where role_id in ( select rur.role_id from rdp_user  as ru" +
                    " left join rdp_user_role as rur on  ru.user_id = rur.user_id where ru.user_name = ?)";
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1,username);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()){
                String role_name = resultSet.getString("role_name");
                set.add(role_name);
            }
            return set;

        } catch (Exception e) {
            log.error(e.getMessage(),e);
            return set;
        } finally {
            closePreparedStatement(preparedStatement);
            closeConnection(connection);
        }
    }
}
