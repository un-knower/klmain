package ax.kl.mapper;

import ax.kl.entity.EquipInfo;
import ax.kl.entity.EquipType;
import ax.kl.entity.TreeModel;
import com.baomidou.mybatisplus.plugins.Page;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 设备类型mapper
 * @author wangbiao
 */
@Repository
public interface EquipInfoMapper {

    /**
     * 设备信息列表
     * @return
     */
    List<EquipInfo> getEquipInfoList(@Param("unitId")String unitId);

    /**
     * 验证设备唯一编码是否存在
     * @param uniqueCode
     * @return
     */
    int validateEquipCode(@Param("uniqueCode")String uniqueCode);


    /**
     * 删
     * @param ids
     * @return
     */
    int deleteEquipInfo(String[] ids);

    /**
     * 增
     * @param equipInfo
     * @return
     */
    int insertEquipInfo(EquipInfo equipInfo);

    /**
     * 改
     * @return
     */
    int updateEquipInfo(EquipInfo equipInfo);


    /**
     * 插入list集合数据
     * @param list
     * @return
     */
    int insertList(@Param("list")List<EquipInfo> list);

    /**
     *获取所有设备类型
     * @return
     */
    List<Map<String,String>> getEquipType();

    /**
     *获取所有设备使用状态
     * @return
     */
    List<Map<String,String>> getEquipStatus();
}
