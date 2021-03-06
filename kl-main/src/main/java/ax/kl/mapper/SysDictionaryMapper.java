package ax.kl.mapper;

import ax.kl.entity.TreeModel;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.plugins.Page;
import ax.kl.entity.SysDataDict;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author: SuZhenpeng
 * Description:
 * Date: Created in 15:26 2017/11/17
 * @Modified By:
 */
@Repository
public interface SysDictionaryMapper extends BaseMapper<SysDataDict> {

    /**
     * 获取字典
     */
    List<SysDataDict> GetDictList(Page page, @Param("typeId") String typeId, @Param("dictSearchName") String dictSearchName);


    /**
     * 获取字典树
     */
    List<TreeModel> getDictTreeList();

    /**
     * 获取排序
     */
    int getMaxOrder();

    /**
     * 保存字典
     * @param dataDict
     * @return
     */
    int saveDict(SysDataDict dataDict);

    /**
     * 更新字典
     * @param dataDict
     * @return
     */
    int updateDict(SysDataDict dataDict);

    /**
     * 删除字典
     * @param ids 字典ID数组
     * @return
     */
    int deleteDicts(String [] ids);

    /**
     * 判断字典下是否有子字典
     * @param ids
     * @return
     */
    List<SysDataDict> getDictType(String[] ids);

    /**
     * 获取是否有排序序号
     * @param typeId
     * @param type
     * @return
     */
    List<SysDataDict> getOrder(@Param("typeId") String typeId, @Param("type") String type, @Param("dictOrder") String dictOrder);

    /**
     * 进行排序
     * @param dictId1
     * @param dictOrder1
     * @param dictId2
     * @param dictOrder2
     * @return
     */
    int upDateOrderSort(@Param("dictId1")String dictId1,@Param("dictOrder1")String dictOrder1,@Param("dictId2")String dictId2,@Param("dictOrder2")String dictOrder2);


    /**
     * 加载字典内容
     * @param typeId
     * @return
     */
    List<SysDataDict> getDataDictList(@Param("typeId") String typeId);

}
