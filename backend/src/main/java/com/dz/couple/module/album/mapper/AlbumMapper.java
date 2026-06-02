package com.dz.couple.module.album.mapper;

import com.dz.couple.module.album.entity.Album;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

@Mapper
public interface AlbumMapper {
    @Select("select id, couple_id as coupleId, name, sort_no as sortNo, deletable, created_at as createdAt, updated_at as updatedAt " +
            "from t_album where couple_id = #{coupleId} order by sort_no asc, id asc")
    List<Album> listByCoupleId(@Param("coupleId") Long coupleId);

    @Select("select id, couple_id as coupleId, name, sort_no as sortNo, deletable, created_at as createdAt, updated_at as updatedAt " +
            "from t_album where couple_id = #{coupleId} and id = #{id} limit 1")
    Album findById(@Param("coupleId") Long coupleId, @Param("id") Long id);

    @Select("select id, couple_id as coupleId, name, sort_no as sortNo, deletable, created_at as createdAt, updated_at as updatedAt " +
            "from t_album where couple_id = #{coupleId} and name = #{name} limit 1")
    Album findByName(@Param("coupleId") Long coupleId, @Param("name") String name);

    @Insert("insert into t_album(couple_id, name, sort_no, deletable, created_at, updated_at) " +
            "values(#{coupleId}, #{name}, #{sortNo}, #{deletable}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Album a);

    @Update("update t_album set name = #{name}, sort_no = #{sortNo}, updated_at = #{updatedAt} where couple_id = #{coupleId} and id = #{id} and deletable = 1")
    int update(Album a);

    @Delete("delete from t_album where couple_id = #{coupleId} and id = #{id} and deletable = 1")
    int delete(@Param("coupleId") Long coupleId, @Param("id") Long id);

    @Select("select count(1) from t_album where couple_id = #{coupleId}")
    int countByCoupleId(@Param("coupleId") Long coupleId);

    @Select("select count(1) from t_photo where couple_id = #{coupleId} and deleted_flag = 0 and album_id = #{albumId}")
    int countPhotos(@Param("coupleId") Long coupleId, @Param("albumId") Long albumId);

    @Select("select count(1) from t_photo where couple_id = #{coupleId} and deleted_flag = 0")
    int countAllPhotos(@Param("coupleId") Long coupleId);

    @Select("select count(1) from t_photo where couple_id = #{coupleId} and deleted_flag = 1")
    int countDeletedPhotos(@Param("coupleId") Long coupleId);

    @Update("update t_photo set album_id = null where couple_id = #{coupleId} and album_id = #{albumId}")
    int detachPhotos(@Param("coupleId") Long coupleId, @Param("albumId") Long albumId);

    @Update("update t_album set updated_at = #{now} where couple_id = #{coupleId} and id = #{id}")
    int touch(@Param("coupleId") Long coupleId, @Param("id") Long id, @Param("now") Date now);
}
