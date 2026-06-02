package com.dz.couple.module.notification.mapper;

import com.dz.couple.module.notification.entity.Notification;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

@Mapper
public interface NotificationMapper {
    @Select("select id, user_id as userId, couple_id as coupleId, type, title, content, ref_id as refId, ref_date as refDate, read_flag as readFlag, silent_flag as silentFlag, archived_flag as archivedFlag, created_at as createdAt, updated_at as updatedAt " +
            "from t_notification where user_id = #{userId} " +
            "and archived_flag = 0 " +
            "and (#{unreadOnly} = 0 or read_flag = 0) " +
            "order by created_at desc limit #{limit}")
    List<Notification> listByUserId(@Param("userId") Long userId, @Param("unreadOnly") int unreadOnly, @Param("limit") int limit);

    @Select("select count(1) from t_notification where user_id = #{userId} and archived_flag = 0 and read_flag = 0")
    int countUnreadAll(@Param("userId") Long userId);

    @Select("select count(1) from t_notification where user_id = #{userId} and archived_flag = 0 and read_flag = 0 and type = #{type}")
    int countUnreadByType(@Param("userId") Long userId, @Param("type") String type);

    @Insert("insert into t_notification(user_id, couple_id, type, title, content, ref_id, ref_date, read_flag, silent_flag, archived_flag, created_at, updated_at) " +
            "values(#{userId}, #{coupleId}, #{type}, #{title}, #{content}, #{refId}, #{refDate}, #{readFlag}, #{silentFlag}, #{archivedFlag}, now(), now()) " +
            "on duplicate key update title = values(title), content = values(content), updated_at = now()")
    int upsert(Notification n);

    @Insert("insert into t_notification(user_id, couple_id, type, title, content, ref_id, ref_date, read_flag, silent_flag, archived_flag, created_at, updated_at) " +
            "values(#{userId}, #{coupleId}, #{type}, #{title}, #{content}, #{refId}, #{refDate}, #{readFlag}, #{silentFlag}, #{archivedFlag}, now(), now())")
    int insert(Notification n);

    @Update("update t_notification set read_flag = 1, updated_at = now() where id = #{id} and user_id = #{userId}")
    int markRead(@Param("userId") Long userId, @Param("id") Long id);

    @Update("update t_notification set read_flag = 1, updated_at = now() where user_id = #{userId} and read_flag = 0")
    int markReadAll(@Param("userId") Long userId);

    @Update("update t_notification set archived_flag = 1, updated_at = now() where user_id = #{userId} and archived_flag = 0 and read_flag = 1")
    int archiveRead(@Param("userId") Long userId);

    @Update("update t_notification set archived_flag = 1, updated_at = now() where user_id = #{userId} and archived_flag = 0 and created_at < #{cutoff}")
    int archiveOlderThan(@Param("userId") Long userId, @Param("cutoff") Date cutoff);

    @Select("select 1 from t_notification where user_id = #{userId} and type = #{type} and ref_id = #{refId} and ref_date = #{refDate} limit 1")
    Integer existsByDedupe(@Param("userId") Long userId, @Param("type") String type, @Param("refId") Long refId, @Param("refDate") Date refDate);
}
