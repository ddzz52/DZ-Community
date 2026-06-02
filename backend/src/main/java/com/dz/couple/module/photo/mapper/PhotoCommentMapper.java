package com.dz.couple.module.photo.mapper;

import com.dz.couple.module.photo.entity.PhotoComment;
import com.dz.couple.module.photo.dto.IdCount;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

@Mapper
public interface PhotoCommentMapper {

    @Select("select count(1) from t_photo_comment where photo_id = #{photoId}")
    long countByPhotoId(@Param("photoId") Long photoId);

    @Select("<script>" +
            "select photo_id as id, count(1) as count from t_photo_comment where photo_id in " +
            "<foreach item='id' collection='photoIds' open='(' separator=',' close=')'>#{id}</foreach> " +
            "group by photo_id" +
            "</script>")
    List<IdCount> countByPhotoIds(@Param("photoIds") List<Long> photoIds);

    @Select("select id, photo_id as photoId, couple_id as coupleId, author_id as authorId, content, created_at as createdAt " +
            "from t_photo_comment where photo_id = #{photoId} order by created_at asc limit #{limit}")
    List<PhotoComment> listByPhotoId(@Param("photoId") Long photoId, @Param("limit") int limit);

    @Select("select id, photo_id as photoId, couple_id as coupleId, author_id as authorId, content, created_at as createdAt " +
            "from t_photo_comment where id = #{id} limit 1")
    PhotoComment findById(@Param("id") Long id);

    @Insert("insert into t_photo_comment(photo_id, couple_id, author_id, content, created_at) values(#{photoId}, #{coupleId}, #{authorId}, #{content}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PhotoComment c);

    @Delete("delete from t_photo_comment where id = #{id}")
    int delete(@Param("id") Long id);
}
