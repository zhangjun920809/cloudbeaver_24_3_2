package io.cloudbeaver.service.security.indaas;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.cloudbeaver.model.BusinessDomainDto;
import io.cloudbeaver.model.BusinessDomainVO;
import io.cloudbeaver.service.security.EmbeddedSecurityControllerFactory;
import io.cloudbeaver.service.security.LicenseUtils;
import io.cloudbeaver.service.security.db.CBDatabase;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.BeanUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class DriDatasourceService {
    public static final Log log = Log.getLog(DriDatasourceService.class);
    public static final Gson gson = new Gson();

    public static void createDriDatasource(String datasourceName, JsonObject jsonObject, String url,String user,int businessId,String driver){
        Connection connection = null;
        PreparedStatement preparedStatement =  null;
        List<String> rolelist = new ArrayList<>();
        try {
            CBDatabase dataSource = EmbeddedSecurityControllerFactory.getDbDbInstance();
            connection = dataSource.openConnection();
            String sql =" insert into indaas_database (name,details,create_user,engine,status,jdbcurl,db_uuid) values(?,?,?,?,?,?,?)";
            preparedStatement = connection.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1,datasourceName);
            preparedStatement.setString(2,gson.toJson(jsonObject));
            preparedStatement.setString(3,user);
            preparedStatement.setString(4,driver);
            preparedStatement.setString(5,"active");
            preparedStatement.setString(6,url);
//            preparedStatement.setString(7,databaseDto.getBusinessSource());
            preparedStatement.setString(7, UUID.randomUUID().toString());
            boolean result = preparedStatement.executeUpdate() > 0 ? true : false;
//            log.info("数据源添加操作执行完毕---" + result);
//获取id
            ResultSet generatedKeys = preparedStatement.getGeneratedKeys();
            int id = 0;
            while(generatedKeys.next()){
                id = generatedKeys.getInt(1);
            }
            //保存到业务域中间表
            savaToBusinessResource(id,businessId,"datasource");
//            log.info("中间表保存完成");
        } catch (Exception e) {
            log.error(e.getMessage(),e);
        } finally {
            LoginPorcess.closePreparedStatement(preparedStatement);
            LoginPorcess.closeConnection(connection);
        }
    }

    public static List<BusinessDomainVO> getBusinessInfo(HttpServletRequest request, HttpServletResponse response) throws IOException {
        List<BusinessDomainDto> resources = getAllBusiness();
        List<BusinessDomainVO> resourcevo= getBusinessDomainTree(-1,resources);

        HashMap<String, Object> map = new HashMap<>();
        map.put("message", "获取业务域信息完成！");
        map.put("code", 200);
        map.put("data", resourcevo);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(gson.toJson(map));
        return  resourcevo;
    }
    private static List<BusinessDomainDto> getAllBusiness(){
        PreparedStatement preparedStatement = null;
        Connection connection = null;
        List<BusinessDomainDto> business = new ArrayList<>();
        try {
            CBDatabase dataSource = EmbeddedSecurityControllerFactory.getDbDbInstance();
            connection = dataSource.openConnection();
            connection.setAutoCommit(true);
            String sql =" select bd_id,bd_name,bd_desc,parent_id,creat_user,create_time,sort from indaas_business_domain order by sort ";
            preparedStatement = connection.prepareStatement(sql);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()){
                int id = resultSet.getInt("bd_id");
                String bdName = resultSet.getString("bd_name");
                String bdDesc = resultSet.getString("bd_desc");
                int parentId = resultSet.getInt("parent_id");

                BusinessDomainDto jsonObject = new BusinessDomainDto();
                jsonObject.setId(id);
                jsonObject.setName(bdName);
                jsonObject.setDesc(bdDesc);
                jsonObject.setParentId(parentId);
                business.add(jsonObject);
            }
            return business;
        } catch (Exception e) {
            log.error("获取业务域信息失败！",e);
            return business;
        } finally {
            LoginPorcess.closePreparedStatement(preparedStatement);
            LoginPorcess.closeConnection(connection);
        }

    }

    private static List<BusinessDomainVO> getBusinessDomainTree(int parentId, List<BusinessDomainDto> businessDomainDtos) {
        List<BusinessDomainVO> result = new ArrayList<>();
        Optional.ofNullable(businessDomainDtos).orElse(new ArrayList<>())
                .stream()
                .filter(mean->(mean.getParentId() == parentId))
                .forEach(mean->{
                    BusinessDomainVO businessDomainVO = new BusinessDomainVO();
                    try {
//                        BeanUtils.copyProperties(businessDomainVO,mean);
                        businessDomainVO.setId(mean.getId());
                        businessDomainVO.setName(mean.getName());
                        businessDomainVO.setDesc(mean.getDesc());
                        businessDomainVO.setParentId(mean.getParentId());
                    }  catch (Exception e) {
                        e.printStackTrace();
                    }
                    List<BusinessDomainVO> child = getBusinessDomainTree(mean.getId(),businessDomainDtos);
                    if (child != null ){
                        businessDomainVO.setChild(child);
                    }
                    result.add(businessDomainVO);
                });
        return result;
    }

    private static void savaToBusinessResource(int id, int businessId, String type) throws  Exception{
        PreparedStatement pre = null;
        Connection connection = null;
        if(businessId <= 0){
            return;
        }
        try {
            CBDatabase dataSource = EmbeddedSecurityControllerFactory.getDbDbInstance();
            connection = dataSource.openConnection();
            connection.setAutoCommit(true);
            //判断业务域是否被使用
            String verdictSql = "insert into indaas_business_resource(bd_id,resource_id,resource_type) values(?,?,?)";
            pre = connection.prepareStatement(verdictSql);
            pre.setInt(1,businessId);
            pre.setInt(2,id);
            pre.setString(3,type);
            pre.executeUpdate();
        } catch (Exception e) {
            log.error("保存中间表信息失败！",e);
            throw new Exception();
        } finally {
            LoginPorcess.closePreparedStatement(pre);
            LoginPorcess.closeConnection(connection);
        }
    }
}
