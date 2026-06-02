package com.dz.couple.module.account.mapper;

import com.dz.couple.module.account.dto.AccountCategoryVO;
import com.dz.couple.module.account.dto.AccountMonthStatsResponse;
import com.dz.couple.module.account.dto.AccountYearStatsResponse;
import com.dz.couple.module.account.entity.Account;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Mapper
public interface AccountMapper {

    @Select("select id, couple_id as coupleId, user_id as userId, category, amount, occurred_at as occurredAt, remark, created_at as createdAt, updated_at as updatedAt " +
            "from t_account where couple_id = #{coupleId} and id = #{id} limit 1")
    Account findById(@Param("coupleId") Long coupleId, @Param("id") Long id);

    @Insert("insert into t_account(couple_id, user_id, category, amount, occurred_at, remark, created_at, updated_at) " +
            "values(#{coupleId}, #{userId}, #{category}, #{amount}, #{occurredAt}, #{remark}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Account a);

    @Update("update t_account set category = #{category}, amount = #{amount}, occurred_at = #{occurredAt}, remark = #{remark}, updated_at = #{updatedAt} " +
            "where couple_id = #{coupleId} and id = #{id}")
    int update(Account a);

    @Delete("delete from t_account where couple_id = #{coupleId} and id = #{id}")
    int delete(@Param("coupleId") Long coupleId, @Param("id") Long id);

    @Select("<script>" +
            "select id, couple_id as coupleId, user_id as userId, category, amount, occurred_at as occurredAt, remark, created_at as createdAt, updated_at as updatedAt " +
            "from t_account where couple_id = #{coupleId} " +
            "<if test='category != null and category.trim() != \"\"'> and category = #{category} </if>" +
            "<if test='from != null'> and occurred_at <![CDATA[ >= ]]> #{from} </if>" +
            "<if test='to != null'> and occurred_at <![CDATA[ < ]]> #{to} </if>" +
            "<choose>" +
            "  <when test='beforeAt != null and beforeId != null'>" +
            "    and (occurred_at <![CDATA[ < ]]> #{beforeAt} or (occurred_at = #{beforeAt} and id <![CDATA[ < ]]> #{beforeId}))" +
            "  </when>" +
            "  <when test='beforeId != null'>" +
            "    and id <![CDATA[ < ]]> #{beforeId}" +
            "  </when>" +
            "</choose>" +
            "order by occurred_at desc, id desc limit #{limit}" +
            "</script>")
    List<Account> list(@Param("coupleId") Long coupleId,
                       @Param("category") String category,
                       @Param("from") Date from,
                       @Param("to") Date to,
                       @Param("beforeAt") Date beforeAt,
                       @Param("beforeId") Long beforeId,
                       @Param("limit") int limit);

    @Select("<script>" +
            "select ifnull(sum(amount), 0) from t_account where couple_id = #{coupleId} " +
            "and occurred_at <![CDATA[ >= ]]> #{from} and occurred_at <![CDATA[ < ]]> #{to}" +
            "</script>")
    BigDecimal sumAmount(@Param("coupleId") Long coupleId, @Param("from") Date from, @Param("to") Date to);

    @Select("<script>" +
            "select category, ifnull(sum(amount), 0) as amount from t_account where couple_id = #{coupleId} " +
            "and occurred_at <![CDATA[ >= ]]> #{from} and occurred_at <![CDATA[ < ]]> #{to} " +
            "group by category order by amount desc" +
            "</script>")
    List<AccountMonthStatsResponse.CategoryItem> sumByCategory(@Param("coupleId") Long coupleId, @Param("from") Date from, @Param("to") Date to);

    @Select("<script>" +
            "select user_id as userId, ifnull(sum(amount), 0) as amount from t_account where couple_id = #{coupleId} " +
            "and occurred_at <![CDATA[ >= ]]> #{from} and occurred_at <![CDATA[ < ]]> #{to} " +
            "group by user_id order by amount desc" +
            "</script>")
    List<AccountMonthStatsResponse.UserItem> sumByUser(@Param("coupleId") Long coupleId, @Param("from") Date from, @Param("to") Date to);

    @Select("<script>" +
            "select month(occurred_at) as month, ifnull(sum(amount), 0) as amount from t_account where couple_id = #{coupleId} " +
            "and occurred_at <![CDATA[ >= ]]> #{from} and occurred_at <![CDATA[ < ]]> #{to} " +
            "group by month(occurred_at) order by month asc" +
            "</script>")
    List<AccountYearStatsResponse.MonthItem> sumByMonth(@Param("coupleId") Long coupleId, @Param("from") Date from, @Param("to") Date to);

    @Select("<script>" +
            "select category, count(1) as count, max(occurred_at) as lastAt from t_account where couple_id = #{coupleId} " +
            "group by category order by lastAt desc limit #{limit}" +
            "</script>")
    List<AccountCategoryVO> listCategories(@Param("coupleId") Long coupleId, @Param("limit") int limit);
}

