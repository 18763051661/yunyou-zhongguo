package cn.edu.qut.mapper;

import cn.edu.qut.entity.OperationLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 操作日志 Mapper(MyBatis 注解 SQL)。
 */
@Mapper
public interface OperationLogMapper {

    @Insert("insert into operation_log(operator, action, target, detail, ip, created_at) "
            + "values(#{operator}, #{action}, #{target}, #{detail}, #{ip}, #{createdAt})")
    int insert(OperationLog log);

    @Select("select * from operation_log order by id desc limit #{offset}, #{size}")
    List<OperationLog> page(@Param("offset") int offset, @Param("size") int size);

    @Select("select count(*) from operation_log")
    long count();
}
