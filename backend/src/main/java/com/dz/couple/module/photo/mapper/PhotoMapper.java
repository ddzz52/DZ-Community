package com.dz.couple.module.photo.mapper;

import com.dz.couple.module.photo.entity.Photo;
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
public interface PhotoMapper {

    @Select("select id, couple_id as coupleId, uploader_id as uploaderId, album_id as albumId, url, thumb_url as thumbUrl, title, location, mood, tags, cover_flag as coverFlag, cover_set_at as coverSetAt, shot_at as shotAt, " +
            "deleted_flag as deletedFlag, deleted_at as deletedAt, deleted_from_album_id as deletedFromAlbumId, created_at as createdAt " +
            "from t_photo where couple_id = #{coupleId} and deleted_flag = 0 order by coalesce(shot_at, created_at) desc limit #{limit}")
    List<Photo> listRecent(@Param("coupleId") Long coupleId, @Param("limit") int limit);

    @Select("select id, couple_id as coupleId, uploader_id as uploaderId, album_id as albumId, url, thumb_url as thumbUrl, title, location, mood, tags, cover_flag as coverFlag, cover_set_at as coverSetAt, shot_at as shotAt, " +
            "deleted_flag as deletedFlag, deleted_at as deletedAt, deleted_from_album_id as deletedFromAlbumId, created_at as createdAt " +
            "from t_photo where couple_id = #{coupleId} and deleted_flag = 0 and cover_flag = 1 order by cover_set_at desc, created_at desc limit 1")
    Photo findCover(@Param("coupleId") Long coupleId);

    @Select("select count(1) from t_photo where couple_id = #{coupleId} and deleted_flag = 0")
    long countByCoupleId(@Param("coupleId") Long coupleId);

    @Select("<script>" +
            "select id, couple_id as coupleId, uploader_id as uploaderId, album_id as albumId, url, thumb_url as thumbUrl, title, location, mood, tags, cover_flag as coverFlag, cover_set_at as coverSetAt, shot_at as shotAt, " +
            "deleted_flag as deletedFlag, deleted_at as deletedAt, deleted_from_album_id as deletedFromAlbumId, created_at as createdAt " +
            "from t_photo where couple_id = #{coupleId} " +
            "<if test='deletedOnly != null and deletedOnly'>and deleted_flag = 1</if> " +
            "<if test='deletedOnly == null or !deletedOnly'>and deleted_flag = 0</if> " +
            "<if test='albumId != null'>and album_id = #{albumId}</if> " +
            "<if test='from != null'>and coalesce(shot_at, created_at) &gt;= #{from}</if> " +
            "<if test='to != null'>and coalesce(shot_at, created_at) &lt;= #{to}</if> " +
            "order by coalesce(shot_at, created_at) " +
            "<choose>" +
            "<when test='orderAsc != null and orderAsc'>asc</when>" +
            "<otherwise>desc</otherwise>" +
            "</choose> " +
            "limit #{limit}" +
            "</script>")
    List<Photo> list(@Param("coupleId") Long coupleId, @Param("albumId") Long albumId, @Param("from") Date from, @Param("to") Date to, @Param("deletedOnly") Boolean deletedOnly, @Param("orderAsc") Boolean orderAsc, @Param("limit") int limit);

    @Insert("insert into t_photo(couple_id, uploader_id, album_id, url, thumb_url, title, location, mood, tags, cover_flag, cover_set_at, shot_at, deleted_flag, deleted_at, deleted_from_album_id, created_at) " +
            "values(#{coupleId}, #{uploaderId}, #{albumId}, #{url}, #{thumbUrl}, #{title}, #{location}, #{mood}, #{tags}, #{coverFlag}, #{coverSetAt}, #{shotAt}, 0, null, null, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Photo p);

    @Update("update t_photo set album_id = #{albumId}, title = #{title}, location = #{location}, mood = #{mood}, tags = #{tags}, shot_at = #{shotAt} " +
            "where couple_id = #{coupleId} and id = #{id}")
    int update(Photo p);

    @Update("update t_photo set album_id = #{albumId}, url = #{url}, thumb_url = #{thumbUrl} where couple_id = #{coupleId} and id = #{id}")
    int updateStorage(@Param("coupleId") Long coupleId, @Param("id") Long id, @Param("albumId") Long albumId, @Param("url") String url, @Param("thumbUrl") String thumbUrl);

    @Update("update t_photo set url = #{url}, thumb_url = #{thumbUrl} where couple_id = #{coupleId} and id = #{id}")
    int updateUrls(@Param("coupleId") Long coupleId, @Param("id") Long id, @Param("url") String url, @Param("thumbUrl") String thumbUrl);

    @Update("update t_photo set cover_flag = 0, cover_set_at = null where couple_id = #{coupleId} and cover_flag = 1")
    int clearCover(@Param("coupleId") Long coupleId);

    @Update("update t_photo set cover_flag = 1, cover_set_at = #{now} where couple_id = #{coupleId} and id = #{id}")
    int setCover(@Param("coupleId") Long coupleId, @Param("id") Long id, @Param("now") Date now);

    @Update("update t_photo set cover_flag = 0, cover_set_at = null where couple_id = #{coupleId} and id = #{id}")
    int unsetCover(@Param("coupleId") Long coupleId, @Param("id") Long id);

    @Delete("delete from t_photo where couple_id = #{coupleId} and id = #{id}")
    int delete(@Param("coupleId") Long coupleId, @Param("id") Long id);

    @Select("select id, couple_id as coupleId, uploader_id as uploaderId, album_id as albumId, url, thumb_url as thumbUrl, title, location, mood, tags, cover_flag as coverFlag, cover_set_at as coverSetAt, shot_at as shotAt, " +
            "deleted_flag as deletedFlag, deleted_at as deletedAt, deleted_from_album_id as deletedFromAlbumId, created_at as createdAt " +
            "from t_photo where couple_id = #{coupleId} and id = #{id} limit 1")
    Photo findById(@Param("coupleId") Long coupleId, @Param("id") Long id);

    @Select("<script>" +
            "select id, couple_id as coupleId, uploader_id as uploaderId, album_id as albumId, url, thumb_url as thumbUrl, title, location, mood, tags, cover_flag as coverFlag, cover_set_at as coverSetAt, shot_at as shotAt, " +
            "deleted_flag as deletedFlag, deleted_at as deletedAt, deleted_from_album_id as deletedFromAlbumId, created_at as createdAt " +
            "from t_photo where couple_id = #{coupleId} and id in " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Photo> listByIds(@Param("coupleId") Long coupleId, @Param("ids") List<Long> ids);

    @Update("update t_photo set deleted_flag = 1, deleted_at = #{now}, deleted_from_album_id = album_id, album_id = #{trashAlbumId} " +
            "where couple_id = #{coupleId} and id = #{id} and deleted_flag = 0")
    int softDelete(@Param("coupleId") Long coupleId, @Param("id") Long id, @Param("trashAlbumId") Long trashAlbumId, @Param("now") Date now);

    @Update("update t_photo set deleted_flag = 0, deleted_at = null, album_id = deleted_from_album_id, deleted_from_album_id = null " +
            "where couple_id = #{coupleId} and id = #{id} and deleted_flag = 1")
    int restore(@Param("coupleId") Long coupleId, @Param("id") Long id);
}
