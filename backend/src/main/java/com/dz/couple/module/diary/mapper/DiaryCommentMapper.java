package com.dz.couple.module.diary.mapper;

import com.dz.couple.module.diary.dto.IdCount;
import com.dz.couple.module.diary.entity.DiaryComment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

@Mapper
public interface DiaryCommentMapper {
    @Select("select id, diary_id as diaryId, couple_id as coupleId, author_id as authorId, content, created_at as createdAt " +
            "from t_diary_comment where couple_id = #{coupleId} and diary_id = #{diaryId} order by created_at asc limit #{limit}")
    List<DiaryComment> listByDiaryId(@Param("coupleId") Long coupleId, @Param("diaryId") Long diaryId, @Param("limit") int limit);

    @Insert("insert into t_diary_comment(diary_id, couple_id, author_id, content, created_at) " +
            "values(#{diaryId}, #{coupleId}, #{authorId}, #{content}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DiaryComment c);

    @Delete("delete from t_diary_comment where couple_id = #{coupleId} and id = #{id} and author_id = #{authorId}")
    int deleteByAuthor(@Param("coupleId") Long coupleId, @Param("id") Long id, @Param("authorId") Long authorId);

    @Select("<script>" +
            "select diary_id as id, count(1) as count from t_diary_comment where couple_id = #{coupleId} and diary_id in " +
            "<foreach item='id' collection='diaryIds' open='(' separator=',' close=')'>#{id}</foreach> " +
            "group by diary_id" +
            "</script>")
    List<IdCount> countByDiaryIds(@Param("coupleId") Long coupleId, @Param("diaryIds") List<Long> diaryIds);
}

