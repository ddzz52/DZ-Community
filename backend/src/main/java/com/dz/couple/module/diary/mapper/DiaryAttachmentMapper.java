package com.dz.couple.module.diary.mapper;

import com.dz.couple.module.diary.entity.DiaryAttachment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DiaryAttachmentMapper {
    @Select("<script>" +
            "select id, diary_id as diaryId, couple_id as coupleId, url, thumb_url as thumbUrl, sort_no as sortNo, created_at as createdAt " +
            "from t_diary_attachment where couple_id = #{coupleId} and diary_id in " +
            "<foreach item='id' collection='diaryIds' open='(' separator=',' close=')'>#{id}</foreach> " +
            "order by diary_id asc, sort_no asc" +
            "</script>")
    List<DiaryAttachment> listByDiaryIds(@Param("coupleId") Long coupleId, @Param("diaryIds") List<Long> diaryIds);

    @Select("select id, diary_id as diaryId, couple_id as coupleId, url, thumb_url as thumbUrl, sort_no as sortNo, created_at as createdAt " +
            "from t_diary_attachment where couple_id = #{coupleId} and diary_id = #{diaryId} order by sort_no asc")
    List<DiaryAttachment> listByDiaryId(@Param("coupleId") Long coupleId, @Param("diaryId") Long diaryId);

    @Insert("insert into t_diary_attachment(diary_id, couple_id, url, thumb_url, sort_no, created_at) " +
            "values(#{diaryId}, #{coupleId}, #{url}, #{thumbUrl}, #{sortNo}, #{createdAt})")
    int insert(DiaryAttachment a);

    @Delete("delete from t_diary_attachment where couple_id = #{coupleId} and diary_id = #{diaryId}")
    int deleteByDiaryId(@Param("coupleId") Long coupleId, @Param("diaryId") Long diaryId);
}

