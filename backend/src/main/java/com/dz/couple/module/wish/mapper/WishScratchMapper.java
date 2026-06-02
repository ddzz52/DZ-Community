package com.dz.couple.module.wish.mapper;

import com.dz.couple.module.wish.dto.WishScratchVO;
import com.dz.couple.module.wish.entity.WishScratchCard;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

@Mapper
public interface WishScratchMapper {

    @Insert("insert into t_wish_scratch_card(couple_id, content, status, reveal_mode, created_by, created_at, scratched_by, scratched_at, scratched_by_1, scratched_at_1, scratched_by_2, scratched_at_2) " +
            "values(#{coupleId}, #{content}, #{status}, #{revealMode}, #{createdBy}, #{createdAt}, #{scratchedBy}, #{scratchedAt}, #{scratchedBy1}, #{scratchedAt1}, #{scratchedBy2}, #{scratchedAt2})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(WishScratchCard c);

    @Select("select id, couple_id as coupleId, content, status, reveal_mode as revealMode, created_by as createdBy, created_at as createdAt, scratched_by as scratchedBy, scratched_at as scratchedAt, " +
            "scratched_by_1 as scratchedBy1, scratched_at_1 as scratchedAt1, scratched_by_2 as scratchedBy2, scratched_at_2 as scratchedAt2 " +
            "from t_wish_scratch_card where couple_id = #{coupleId} and id = #{id} limit 1")
    WishScratchCard findById(@Param("coupleId") Long coupleId, @Param("id") Long id);

    @Select("<script>" +
            "select c.id as id, c.status as status, c.reveal_mode as revealMode, " +
            "case when c.status = 1 or c.reveal_mode = 0 then c.content else null end as content, " +
            "c.scratched_by_1 as scratchedBy1, c.scratched_at_1 as scratchedAt1, c.scratched_by_2 as scratchedBy2, c.scratched_at_2 as scratchedAt2, " +
            "c.created_by as createdBy, u1.nickname as createdByNickname, c.created_at as createdAt, " +
            "c.scratched_by as scratchedBy, u2.nickname as scratchedByNickname, c.scratched_at as scratchedAt " +
            "from t_wish_scratch_card c " +
            "left join t_user u1 on u1.id = c.created_by " +
            "left join t_user u2 on u2.id = c.scratched_by " +
            "where c.couple_id = #{coupleId} " +
            "<if test='status != null'> and c.status = #{status} </if>" +
            "order by c.status asc, " +
            "case when c.status = 0 then c.created_at else c.scratched_at end desc, " +
            "c.id desc " +
            "</script>")
    List<WishScratchVO> list(@Param("coupleId") Long coupleId, @Param("status") Integer status);

    @Update("update t_wish_scratch_card set status = 1, scratched_by = #{userId}, scratched_at = #{at} " +
            "where couple_id = #{coupleId} and id = #{id} and status = 0")
    int markScratched(@Param("coupleId") Long coupleId, @Param("id") Long id, @Param("userId") Long userId, @Param("at") Date at);

    @Update("update t_wish_scratch_card set " +
            "scratched_by_1 = case " +
            "  when scratched_by_1 is null then #{userId} " +
            "  when scratched_by_1 = #{userId} then scratched_by_1 " +
            "  else scratched_by_1 end, " +
            "scratched_at_1 = case " +
            "  when scratched_by_1 is null then #{at} " +
            "  when scratched_by_1 = #{userId} then scratched_at_1 " +
            "  else scratched_at_1 end, " +
            "scratched_by_2 = case " +
            "  when scratched_by_2 is null and scratched_by_1 is not null and scratched_by_1 <> #{userId} then #{userId} " +
            "  when scratched_by_2 = #{userId} then scratched_by_2 " +
            "  else scratched_by_2 end, " +
            "scratched_at_2 = case " +
            "  when scratched_by_2 is null and scratched_by_1 is not null and scratched_by_1 <> #{userId} then #{at} " +
            "  when scratched_by_2 = #{userId} then scratched_at_2 " +
            "  else scratched_at_2 end " +
            "where couple_id = #{coupleId} and id = #{id} and status = 0")
    int markScratchedBoth(@Param("coupleId") Long coupleId, @Param("id") Long id, @Param("userId") Long userId, @Param("at") Date at);

    @Update("update t_wish_scratch_card set status = 1, scratched_by = #{userId}, scratched_at = #{at} " +
            "where couple_id = #{coupleId} and id = #{id} and status = 0 and scratched_by_1 is not null and scratched_by_2 is not null")
    int unlockIfBothScratched(@Param("coupleId") Long coupleId, @Param("id") Long id, @Param("userId") Long userId, @Param("at") Date at);

    @Select("select c.id as id, c.status as status, c.reveal_mode as revealMode, " +
            "case when c.status = 1 or c.reveal_mode = 0 then c.content else null end as content, " +
            "c.scratched_by_1 as scratchedBy1, c.scratched_at_1 as scratchedAt1, c.scratched_by_2 as scratchedBy2, c.scratched_at_2 as scratchedAt2, " +
            "c.created_by as createdBy, u1.nickname as createdByNickname, c.created_at as createdAt, " +
            "c.scratched_by as scratchedBy, u2.nickname as scratchedByNickname, c.scratched_at as scratchedAt " +
            "from t_wish_scratch_card c " +
            "left join t_user u1 on u1.id = c.created_by " +
            "left join t_user u2 on u2.id = c.scratched_by " +
            "where c.couple_id = #{coupleId} and c.id = #{id} limit 1")
    WishScratchVO findVoById(@Param("coupleId") Long coupleId, @Param("id") Long id);
}
