package com.example.tst.service;

import com.baomidou.mybatisplus.plugins.Page;
import com.example.tst.entity.SysRole;

/**
 * @Author: SuZhenpeng
 * Description:
 * Date: Created in 9:53 2017/11/23
 * @Modified By:
 */

public interface SysRoleService {

    /**
     * 加载表格
     * @param page
     * @param searchName
     * @return
     */
    Page<SysRole> GetRoleList(Page page,String searchName);


    /**
     * 保存角色
     * @param sysRole
     * @return
     */
    int saveOrUpdateRole(SysRole sysRole);

    /**
     * 删除角色
     * @param ids
     * @return
     */
    int delRole(String ids);

}
