package cn.edu.qut.mapper;

import cn.edu.qut.entity.TravelPreference;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TravelPreferenceMapper {

    @Insert("insert into travel_preference(nickname,days,provinces,categories,budget,companions,transport,focus) "
            + "values(#{nickname},#{days},#{provinces},#{categories},#{budget},#{companions},#{transport},#{focus})")
    int insert(TravelPreference pref);

    @Select("select * from travel_preference where id=#{id}")
    TravelPreference findById(Long id);

    @Select("select * from travel_preference order by id desc limit 10")
    List<TravelPreference> recent();
}
