package com.dz.couple.module.diary.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

@Mapper
public interface DiaryPinMapper {
    @Select("select count(1) from t_diary_pin where couple_id = #{coupleId} and diary_id = #{diaryId} and user_id = #{userId}")
    int exists(@Param("coupleId") Long coupleId, @Param("diaryId") Long diaryId, @Param("userId") Long userId);

    @Insert("insert into t_diary_pin(diary_id, user_id, couple_id, pinned_at) values(#{diaryId}, #{userId}, #{coupleId}, #{pinnedAt})")
    int insert(@Param("coupleId") Long coupleId, @Param("diaryId") Long diaryId, @Param("userId") Long userId, @Param("pinnedAt") Date pinnedAt);

    @Delete("delete from t_diary_pin where couple_id = #{coupleId} and diary_id = #{diaryId} and user_id = #{userId}")
    int delete(@Param("coupleId") Long coupleId, @Param("diaryId") Long diaryId, @Param("userId") Long userId);

    @Select("<script>" +
            "select diary_id from t_diary_pin where couple_id = #{coupleId} and user_id = #{userId} and diary_id in " +
            "<foreach item='id' collection='diaryIds' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Long> listPinnedDiaryIds(@Param("coupleId") Long coupleId, @Param("userId") Long userId, @Param("diaryIds") List<Long> diaryIds);
}

