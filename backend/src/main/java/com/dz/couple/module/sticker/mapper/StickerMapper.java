package com.dz.couple.module.sticker.mapper;

import com.dz.couple.module.sticker.entity.Sticker;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StickerMapper {
    @Select("select id, user_id as userId, couple_id as coupleId, url, created_at as createdAt " +
            "from t_sticker where user_id = #{userId} order by created_at desc limit #{limit}")
    List<Sticker> listByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    @Insert("insert into t_sticker(user_id, couple_id, url, created_at) values(#{userId}, #{coupleId}, #{url}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Sticker s);

    @Delete("delete from t_sticker where id = #{id} and user_id = #{userId}")
    int deleteById(@Param("userId") Long userId, @Param("id") Long id);
}

