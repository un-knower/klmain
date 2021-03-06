package ax.kl.service.impl;

import ax.kl.common.DESEncryptTools;
import ax.kl.common.PublicTools;
import ax.kl.common.TreeUtil;
import ax.kl.entity.LoginInfo;
import ax.kl.entity.SysOrganise;
import ax.kl.entity.TreeModel;
import ax.kl.service.SysBusinessUserService;
import com.baomidou.mybatisplus.plugins.Page;
import ax.kl.entity.SysBusinessUser;
import ax.kl.mapper.SysBusinessUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Service
public class SysBusinessUserServiceImpl implements SysBusinessUserService {

    @Autowired
    private SysBusinessUserMapper sysBusinessUserMapper;




    //显示人员列表
    @Override
    public Page<SysOrganise> getBusinessUserList(Page page, String typeCode, String searchName) {

//        List<String> ids=new ArrayList<>();
//        //查找此typeCode下的子机构（包含自己）
//        ids=businessUserMapper.getChildByTypeCode(ids);
//        //将获取的节点ID集合转成数组
//        String[] arrId= (String[]) ids.toArray();
        //获取对应的人员列表
        page.setRecords(sysBusinessUserMapper.getBusinessUserList(page,typeCode,searchName));
        return page;
    }

    @Override
    @Transactional
    public String updateOrAddBusinessUser(HttpServletRequest request,SysBusinessUser businessUser) {
        //如果传来的对象没有userId，说明是添加数据
        if("".equals(businessUser.getUserId()) ||businessUser.getUserId()==null){
            //UUID码来创建ID
            String userId= UUID.randomUUID().toString();
            businessUser.setUserId(userId);
            SysBusinessUser user= (SysBusinessUser) request.getSession().getAttribute("user");
            businessUser.setCreateDeptId(user==null?"0000":user.getDeptId());
            businessUser.setCreateUserId(user==null?"0000":user.getUserId());

            //添加数据
            sysBusinessUserMapper.insertBusinessUser(businessUser);
            String pwd = PublicTools.byteArr2HexStr(DESEncryptTools.encrypt("888888".getBytes()));
            businessUser.setPassWord(pwd);
            sysBusinessUserMapper.saveUser(businessUser);

            return userId;
        }else{
            //有UserId，说明是更新数据
            sysBusinessUserMapper.updateBusinessUser(businessUser);
            String pwd = PublicTools.byteArr2HexStr(DESEncryptTools.encrypt("888888".getBytes()));
            businessUser.setPassWord(pwd);
            sysBusinessUserMapper.saveUser(businessUser);
            return "";
        }
    }

    @Override
    @Transactional
    public void deleteBusinessUser(String[] idLists) {
        //直接删除
        sysBusinessUserMapper.deleteBusinessUser(idLists);
    }

    @Override
    public boolean checkLoginName(String loginName) {

        if(sysBusinessUserMapper.checkLoginName(loginName).size()>0){
            return false;
        }else{
            return true;
        }

    }

    /**
     * 根据ID获取用户信息
     * @param buserId
     * @return
     */
    public LoginInfo getUserInfo(String buserId){
        LoginInfo loginInfo = null;
        List<LoginInfo> list = this.sysBusinessUserMapper.getUserInfo(buserId);
        if(list!=null && list.size()>0){
            loginInfo = list.get(0);
        }
        return loginInfo;
    }

    @Override
    public List<TreeModel> getSysOrganiseTreeList() {
        return TreeUtil.getTree(this.sysBusinessUserMapper.getSysOrganiseTreeList());
    }


}
