package com.dz.couple.module.couple.mapper;

import com.dz.couple.module.couple.entity.Couple;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CoupleMapper {

    @Insert("insert into t_couple(signature, invite_code, created_at, updated_at) values(null, #{inviteCode}, now(), now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Couple couple);

    @Select("select id, signature, about_text as aboutText, invite_code as inviteCode, created_at as createdAt, updated_at as updatedAt from t_couple where id = #{id}")
    Couple findById(@Param("id") Long id);

    @Select("select id from t_couple order by id asc")
    List<Long> listAllIds();

    @Select("select id, signature, about_text as aboutText, invite_code as inviteCode, created_at as createdAt, updated_at as updatedAt from t_couple where invite_code = #{code} limit 1")
    Couple findByInviteCode(@Param("code") String code);

    @Select("select c.id, c.signature, c.about_text as aboutText, c.invite_code as inviteCode, c.created_at as createdAt, c.updated_at as updatedAt " +
            "from t_couple c left join t_user u on u.couple_id = c.id " +
            "group by c.id having count(u.id) < 2 order by c.id asc limit 1")
    Couple findAvailable();

    @Update("update t_couple set signature = #{signature}, updated_at = now() where id = #{id}")
    int updateSignature(@Param("id") Long id, @Param("signature") String signature);

    @Update("update t_couple set about_text = #{aboutText}, updated_at = now() where id = #{id}")
    int updateAboutText(@Param("id") Long id, @Param("aboutText") String aboutText);
}
