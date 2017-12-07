package ax.kl.mapper;

import ax.kl.entity.ChemicalsInfo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 化学品信息
 * @author wangbiao
 * Date 2017/12/07
 */
@Repository
public interface ChemicalsInfoMapper {

    /**
     * 获取化学品列表
     * @param chemName 化学品名称
     * @param equipName 设备名称
     * @param companyName 企业名称
     * @return
     */
    List<ChemicalsInfo> getChemicalsList(@Param("chemName")String chemName,@Param("equipName")String equipName,@Param("companyName")String companyName);
}
