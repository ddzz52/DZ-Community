package com.dz.couple.module.photo.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

@Mapper
public interface PhotoLikeMapper {

    @Select("select count(1) from t_photo_like where photo_id = #{photoId}")
    long countByPhotoId(@Param("photoId") Long photoId);

    @Select("<script>" +
            "select photo_id as id, count(1) as count from t_photo_like where photo_id in " +
            "<foreach item='id' collection='photoIds' open='(' separator=',' close=')'>#{id}</foreach> " +
            "group by photo_id" +
            "</script>")
    List<com.dz.couple.module.photo.dto.IdCount> countByPhotoIds(@Param("photoIds") List<Long> photoIds);

    @Select("select 1 from t_photo_like where photo_id = #{photoId} and user_id = #{userId} limit 1")
    Integer exists(@Param("photoId") Long photoId, @Param("userId") Long userId);

    @Insert("insert into t_photo_like(photo_id, user_id, couple_id, created_at) values(#{photoId}, #{userId}, #{coupleId}, #{createdAt})")
    int insert(@Param("photoId") Long photoId, @Param("userId") Long userId, @Param("coupleId") Long coupleId, @Param("createdAt") Date createdAt);

    @Delete("delete from t_photo_like where photo_id = #{photoId} and user_id = #{userId}")
    int delete(@Param("photoId") Long photoId, @Param("userId") Long userId);

    @Select("<script>" +
            "select photo_id from t_photo_like where user_id = #{userId} and couple_id = #{coupleId} and photo_id in " +
            "<foreach item='id' collection='photoIds' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Long> listLikedPhotoIds(@Param("userId") Long userId, @Param("coupleId") Long coupleId, @Param("photoIds") List<Long> photoIds);
}
