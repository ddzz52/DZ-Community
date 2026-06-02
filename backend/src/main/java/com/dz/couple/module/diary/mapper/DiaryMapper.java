package com.dz.couple.module.diary.mapper;

import com.dz.couple.module.diary.entity.Diary;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DiaryMapper {

    @Select("select id, couple_id as coupleId, author_id as authorId, content, mood, private_flag as privateFlag, created_at as createdAt, updated_at as updatedAt " +
            "from t_diary where couple_id = #{coupleId} order by created_at desc limit #{limit}")
    List<Diary> listRecent(@Param("coupleId") Long coupleId, @Param("limit") int limit);

    @Select("select count(1) from t_diary where couple_id = #{coupleId}")
    long countByCoupleId(@Param("coupleId") Long coupleId);

    @Select("<script>" +
            "select id, couple_id as coupleId, author_id as authorId, content, mood, private_flag as privateFlag, created_at as createdAt, updated_at as updatedAt " +
            "from t_diary where couple_id = #{coupleId} " +
            "and (private_flag = 0 or author_id = #{userId}) " +
            "<if test='keyword != null and keyword.trim() != \"\"'>and content like concat('%', #{keyword}, '%')</if> " +
            "order by created_at desc limit #{limit}" +
            "</script>")
    List<Diary> list(@Param("coupleId") Long coupleId, @Param("userId") Long userId, @Param("keyword") String keyword, @Param("limit") int limit);

    @Select("select id, couple_id as coupleId, author_id as authorId, content, mood, private_flag as privateFlag, created_at as createdAt, updated_at as updatedAt " +
            "from t_diary where couple_id = #{coupleId} and id = #{id} limit 1")
    Diary findById(@Param("coupleId") Long coupleId, @Param("id") Long id);

    @Insert("insert into t_diary(couple_id, author_id, content, mood, private_flag, created_at, updated_at) " +
            "values(#{coupleId}, #{authorId}, #{content}, #{mood}, #{privateFlag}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Diary d);

    @Update("update t_diary set content = #{content}, mood = #{mood}, private_flag = #{privateFlag}, updated_at = #{updatedAt} " +
            "where couple_id = #{coupleId} and id = #{id} and author_id = #{authorId}")
    int updateByAuthor(Diary d);

    @Delete("delete from t_diary where couple_id = #{coupleId} and id = #{id} and author_id = #{authorId}")
    int deleteByAuthor(@Param("coupleId") Long coupleId, @Param("id") Long id, @Param("authorId") Long authorId);
}
