package com.dz.couple.module.memo.mapper;

import com.dz.couple.module.memo.dto.MemoCategoryVO;
import com.dz.couple.module.memo.entity.MemoCategory;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MemoCategoryMapper {
    @Select("select id, couple_id as coupleId, name, sort_no as sortNo, system_flag as systemFlag, created_by as createdBy, created_at as createdAt, updated_at as updatedAt " +
            "from t_memo_category where couple_id = #{coupleId} order by sort_no asc, id asc")
    List<MemoCategoryVO> listByCoupleId(@Param("coupleId") Long coupleId);

    @Select("select count(1) from t_memo_category where couple_id = #{coupleId}")
    long countByCoupleId(@Param("coupleId") Long coupleId);

    @Select("select id, couple_id as coupleId, name, sort_no as sortNo, system_flag as systemFlag, created_by as createdBy, created_at as createdAt, updated_at as updatedAt " +
            "from t_memo_category where couple_id = #{coupleId} and id = #{id} limit 1")
    MemoCategory findById(@Param("coupleId") Long coupleId, @Param("id") Long id);

    @Select("select id, couple_id as coupleId, name, sort_no as sortNo, system_flag as systemFlag, created_by as createdBy, created_at as createdAt, updated_at as updatedAt " +
            "from t_memo_category where couple_id = #{coupleId} and name = #{name} limit 1")
    MemoCategory findByName(@Param("coupleId") Long coupleId, @Param("name") String name);

    @Insert("insert into t_memo_category(couple_id, name, sort_no, system_flag, created_by, created_at, updated_at) " +
            "values(#{coupleId}, #{name}, #{sortNo}, #{systemFlag}, #{createdBy}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(MemoCategory c);

    @Update("update t_memo_category set name = #{name}, updated_at = #{updatedAt} where couple_id = #{coupleId} and id = #{id} and system_flag = 0")
    int updateName(@Param("coupleId") Long coupleId, @Param("id") Long id, @Param("name") String name, @Param("updatedAt") java.util.Date updatedAt);

    @Delete("delete from t_memo_category where couple_id = #{coupleId} and id = #{id} and system_flag = 0")
    int delete(@Param("coupleId") Long coupleId, @Param("id") Long id);
}
