package com.dz.couple.module.admin.mapper;

import com.dz.couple.module.admin.entity.SystemAnnouncement;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SystemAnnouncementMapper {

    @Insert("insert into t_system_announcement(content, active, created_by, created_at, updated_at) " +
            "values(#{content}, #{active}, #{createdBy}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SystemAnnouncement a);

    @Select("select id, content, active, created_by as createdBy, created_at as createdAt, updated_at as updatedAt " +
            "from t_system_announcement where active = 1 order by id desc limit 3")
    List<SystemAnnouncement> listActive();

    @Select("select id, content, active, created_by as createdBy, created_at as createdAt, updated_at as updatedAt " +
            "from t_system_announcement order by id desc")
    List<SystemAnnouncement> listAll();

    @Update("update t_system_announcement set active = #{active}, updated_at = now() where id = #{id}")
    int toggleActive(@Param("id") Long id, @Param("active") Integer active);

    @Update("update t_system_announcement set content = #{content}, updated_at = now() where id = #{id}")
    int updateContent(@Param("id") Long id, @Param("content") String content);

    @Delete("delete from t_system_announcement where id = #{id}")
    int deleteById(@Param("id") Long id);
}
