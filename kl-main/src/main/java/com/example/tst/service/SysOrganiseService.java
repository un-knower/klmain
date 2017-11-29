package com.example.tst.service;

import com.baomidou.mybatisplus.plugins.Page;
import com.example.tst.entity.DataDict;
import com.example.tst.entity.SysOrganise;
import com.example.tst.entity.TreeModel;
import org.apache.ibatis.annotations.Param;

import java.util.Dictionary;
import java.util.List;

public interface SysOrganiseService {

    /**
     * 获取组织机构列表，可条件查询
     */
    Page<SysOrganise> getSysOrganiseList(Page page, @Param("typeId") String typeId, @Param("searchName") String searchName);


    /**
     * 获取组织机构树
     * @return
     */
    List<TreeModel> getSysOrganiseTreeList();

    /**
     * 新增或更新组织机构
     */
    String  updateOrAddSysOrganise(SysOrganise sysOrganise);


    /**
     * 单一或批量删除组织机构
     */
    int deleteSysOrganises(String[] ids);


    //移动节点
//    String moveOrder(String type,SysOrganise sysOrganise);

    //在字典中获取指定的typeId的数据
    List<DataDict> getDataDictByTypeId();

    //获取所有的组织机构
    List<SysOrganise> getAllSysOrganises();
}
