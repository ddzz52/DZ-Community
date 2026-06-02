package com.dz.couple.module.diary.mapper;

import com.dz.couple.module.diary.dto.IdCount;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

@Mapper
public interface DiaryLikeMapper {
    @Select("select count(1) from t_diary_like where couple_id = #{coupleId} and diary_id = #{diaryId} and user_id = #{userId}")
    int exists(@Param("coupleId") Long coupleId, @Param("diaryId") Long diaryId, @Param("userId") Long userId);

    @Insert("insert into t_diary_like(diary_id, user_id, couple_id, created_at) values(#{diaryId}, #{userId}, #{coupleId}, #{createdAt})")
    int insert(@Param("coupleId") Long coupleId, @Param("diaryId") Long diaryId, @Param("userId") Long userId, @Param("createdAt") Date createdAt);

    @Delete("delete from t_diary_like where couple_id = #{coupleId} and diary_id = #{diaryId} and user_id = #{userId}")
    int delete(@Param("coupleId") Long coupleId, @Param("diaryId") Long diaryId, @Param("userId") Long userId);

    @Select("<script>" +
            "select diary_id as id, count(1) as count from t_diary_like where couple_id = #{coupleId} and diary_id in " +
            "<foreach item='id' collection='diaryIds' open='(' separator=',' close=')'>#{id}</foreach> " +
            "group by diary_id" +
            "</script>")
    List<IdCount> countByDiaryIds(@Param("coupleId") Long coupleId, @Param("diaryIds") List<Long> diaryIds);

    @Select("<script>" +
            "select diary_id from t_diary_like where couple_id = #{coupleId} and user_id = #{userId} and diary_id in " +
            "<foreach item='id' collection='diaryIds' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Long> listLikedDiaryIds(@Param("coupleId") Long coupleId, @Param("userId") Long userId, @Param("diaryIds") List<Long> diaryIds);
}

