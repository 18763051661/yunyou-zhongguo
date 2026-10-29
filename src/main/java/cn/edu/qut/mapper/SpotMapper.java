package cn.edu.qut.mapper;

import cn.edu.qut.entity.ScenicSpot;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface SpotMapper {

    @Select("select * from scenic_spot where id = #{id}")
    ScenicSpot findById(Long id);

    @Select("select * from scenic_spot where status = 1")
    List<ScenicSpot> findAllOnShelf();

    /**
     * 动态条件分页查询(全国)。sort 由 Service 白名单映射为 ORDER BY, 杜绝注入。
     */
    @Select("<script>"
            + "select * from scenic_spot "
            + "<where>"
            + "  status = 1 "
            + "  <if test='categoryId != null'> and category_id = #{categoryId} </if>"
            + "  <if test='province != null and province != \"\"'> and province = #{province} </if>"
            + "  <if test='region != null and region != \"\"'> and region = #{region} </if>"
            + "  <if test='freeOnly != null and freeOnly == true'> and price = 0 </if>"
            + "  <if test='keyword != null and keyword != \"\"'>"
            + "    and (name like concat('%', #{keyword}, '%') "
            + "      or alias like concat('%', #{keyword}, '%') "
            + "      or province like concat('%', #{keyword}, '%') "
            + "      or city like concat('%', #{keyword}, '%') "
            + "      or summary like concat('%', #{keyword}, '%') "
            + "      or tags like concat('%', #{keyword}, '%')) "
            + "  </if>"
            + "  <if test='tag != null and tag != \"\"'> and tags like concat('%', #{tag}, '%') </if>"
            + "  <if test='maxDifficulty != null'> and difficulty &lt;= #{maxDifficulty} </if>"
            + "</where>"
            + " order by "
            + "<choose>"
            + "  <when test='sort == \"rating\"'> rating desc, popularity desc </when>"
            + "  <when test='sort == \"priceAsc\"'> price asc, popularity desc </when>"
            + "  <when test='sort == \"priceDesc\"'> price desc, popularity desc </when>"
            + "  <when test='sort == \"durationAsc\"'> duration_minutes asc, popularity desc </when>"
            + "  <when test='sort == \"difficultyAsc\"'> difficulty asc, popularity desc </when>"
            + "  <when test='sort == \"province\"'> province asc, popularity desc </when>"
            + "  <otherwise> popularity desc, rating desc </otherwise>"
            + "</choose>"
            + " limit #{offset}, #{size}"
            + "</script>")
    List<ScenicSpot> findPage(@Param("categoryId") Long categoryId,
                              @Param("province") String province,
                              @Param("region") String region,
                              @Param("freeOnly") Boolean freeOnly,
                              @Param("keyword") String keyword,
                              @Param("tag") String tag,
                              @Param("maxDifficulty") Integer maxDifficulty,
                              @Param("sort") String sort,
                              @Param("offset") int offset,
                              @Param("size") int size);

    @Select("<script>"
            + "select count(*) from scenic_spot "
            + "<where>"
            + "  status = 1 "
            + "  <if test='categoryId != null'> and category_id = #{categoryId} </if>"
            + "  <if test='province != null and province != \"\"'> and province = #{province} </if>"
            + "  <if test='region != null and region != \"\"'> and region = #{region} </if>"
            + "  <if test='freeOnly != null and freeOnly == true'> and price = 0 </if>"
            + "  <if test='keyword != null and keyword != \"\"'>"
            + "    and (name like concat('%', #{keyword}, '%') "
            + "      or alias like concat('%', #{keyword}, '%') "
            + "      or province like concat('%', #{keyword}, '%') "
            + "      or city like concat('%', #{keyword}, '%') "
            + "      or summary like concat('%', #{keyword}, '%') "
            + "      or tags like concat('%', #{keyword}, '%')) "
            + "  </if>"
            + "  <if test='tag != null and tag != \"\"'> and tags like concat('%', #{tag}, '%') </if>"
            + "  <if test='maxDifficulty != null'> and difficulty &lt;= #{maxDifficulty} </if>"
            + "</where>"
            + "</script>")
    long countPage(@Param("categoryId") Long categoryId,
                   @Param("province") String province,
                   @Param("region") String region,
                   @Param("freeOnly") Boolean freeOnly,
                   @Param("keyword") String keyword,
                   @Param("tag") String tag,
                   @Param("maxDifficulty") Integer maxDifficulty);

    @Select("select * from scenic_spot where status = 1 order by popularity desc limit #{n}")
    List<ScenicSpot> findTopPopular(int n);

    @Select("select * from scenic_spot where status = 1 order by rating desc, rating_count desc limit #{n}")
    List<ScenicSpot> findTopRating(int n);

    /** 云游盲盒: 随机抽取一个上架景点 */
    @Select("select * from scenic_spot where status = 1 order by rand() limit 1")
    ScenicSpot findRandom();

    /** 省份聚合: 省份、景点数、平均评分、区域归属(用于全国导览与足迹地图) */
    @Select("select province as province, max(region) as region, count(*) as count, "
            + "round(avg(rating),2) as avgRating, "
            + "sum(case when price = 0 then 1 else 0 end) as freeCount "
            + "from scenic_spot where status = 1 group by province order by count desc, province")
    List<Map<String, Object>> provinceAggregate();

    @Update("update scenic_spot set rating = (rating * rating_count + #{rating}) / (rating_count + 1), "
            + "rating_count = rating_count + 1 where id = #{spotId}")
    int applyNewRating(@Param("spotId") Long spotId, @Param("rating") int rating);

    // ==================== 管理端 (含未上架景点) ====================

    @Select("<script>"
            + "select * from scenic_spot "
            + "<where>"
            + "  <if test='keyword != null and keyword != \"\"'>"
            + "    (name like concat('%', #{keyword}, '%') "
            + "      or province like concat('%', #{keyword}, '%') "
            + "      or city like concat('%', #{keyword}, '%'))"
            + "  </if>"
            + "</where>"
            + " order by id desc limit #{offset}, #{size}"
            + "</script>")
    List<ScenicSpot> findAdminPage(@Param("keyword") String keyword,
                                   @Param("offset") int offset,
                                   @Param("size") int size);

    @Select("<script>"
            + "select count(*) from scenic_spot "
            + "<where>"
            + "  <if test='keyword != null and keyword != \"\"'>"
            + "    (name like concat('%', #{keyword}, '%') "
            + "      or province like concat('%', #{keyword}, '%') "
            + "      or city like concat('%', #{keyword}, '%'))"
            + "  </if>"
            + "</where>"
            + "</script>")
    long countAdminPage(@Param("keyword") String keyword);

    @Insert("insert into scenic_spot (name, category_id, province, city, region, alias, summary, description, "
            + "highlights, tags, image_prompt, rating, rating_count, popularity, price, open_time, "
            + "duration_minutes, difficulty, lng, lat, address, suitable_for, tips, status, created_at) "
            + "values (#{s.name}, #{s.categoryId}, #{s.province}, #{s.city}, #{s.region}, #{s.alias}, #{s.summary}, "
            + "#{s.description}, #{s.highlights}, #{s.tags}, #{s.imagePrompt}, "
            + "ifnull(#{s.rating}, 4.5), ifnull(#{s.ratingCount}, 0), ifnull(#{s.popularity}, 0), "
            + "ifnull(#{s.price}, 0), #{s.openTime}, ifnull(#{s.durationMinutes}, 120), ifnull(#{s.difficulty}, 1), "
            + "#{s.lng}, #{s.lat}, #{s.address}, #{s.suitableFor}, #{s.tips}, ifnull(#{s.status}, 1), now())")
    @Options(useGeneratedKeys = true, keyProperty = "s.id")
    int insert(@Param("s") ScenicSpot s);

    @Update("update scenic_spot "
            + "set name = #{s.name}, category_id = #{s.categoryId}, province = #{s.province}, city = #{s.city}, "
            + "region = #{s.region}, alias = #{s.alias}, summary = #{s.summary}, description = #{s.description}, "
            + "tags = #{s.tags}, price = #{s.price}, open_time = #{s.openTime}, lng = #{s.lng}, lat = #{s.lat}, "
            + "address = #{s.address}, status = #{s.status} "
            + "where id = #{s.id}")
    int update(@Param("s") ScenicSpot s);

    @Delete("delete from scenic_spot where id = #{id}")
    int deleteById(Long id);

    /** AI 规划 RAG: 某省份的热门景点(简版字段, 供 AI 挑选) */
    @Select("select id, name, province, price, rating from scenic_spot "
            + "where status = 1 and province = #{province} "
            + "order by rating desc, popularity desc limit 6")
    List<Map<String, Object>> summaryByProvince(@Param("province") String province);

    /** AI 规划 RAG: 全国热门景点简版 */
    @Select("select id, name, province, price, rating from scenic_spot "
            + "where status = 1 order by popularity desc, rating desc limit #{n}")
    List<Map<String, Object>> topPopularBrief(@Param("n") int n);
}
