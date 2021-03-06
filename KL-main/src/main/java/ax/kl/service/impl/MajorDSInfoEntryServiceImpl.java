package ax.kl.service.impl;

import ax.kl.entity.*;
import ax.kl.mapper.MajorDSInfoEntryMapper;
import ax.kl.mapper.ProcessUnitMapper;
import ax.kl.service.MajorDSInfoEntryService;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.plugins.Page;
import org.apache.commons.collections4.map.HashedMap;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 重大危险源信息录入
 * @author Created by mxl
 * @version 创建时间：${date} ${time}
 */

@Service
public class MajorDSInfoEntryServiceImpl implements MajorDSInfoEntryService {
    @Autowired
    MajorDSInfoEntryMapper  majorDSInfoEntryMapper;

    @Autowired
    ProcessUnitMapper processUnitMapper;

    /**
     * 通过ID获取危险源信息
     * @param sourceId
     * @return
     */
    @Override
    public List<DangerSourceInfo> getSourceInfo(String sourceId) {
        return majorDSInfoEntryMapper.getSourceInfo(sourceId);
    }

    /**
     * 通过id获取装置设施周围环境
     * @param sourceId
     * @return
     */
    @Override
    public List<FacilitiesCondition> getSourceEquipList(String sourceId) {
        return majorDSInfoEntryMapper.getSourceEquipList(sourceId);
    }

    /**
     * 通过id获取法律保护区信息
     * @param sourceId
     * @return
     */
    @Override
    public List<LegalProtection> getSourceLegalList(String sourceId) {
        return majorDSInfoEntryMapper.getSourceLegalList(sourceId);
    }


    /**
     * 通过ID删除危险源信息
     * @param idLists
     */
    @Override
    @Transactional
    public void delSourceInfo(String[] idLists) {
        //直接删除
        majorDSInfoEntryMapper.delSourceInfo(idLists);
    }

    /**
     * 保存数据信息
     * @param cmd
     * @return
     */
    @Transactional
    @Override
    public String  saveOrUpdateData(String  cmd){
        JSONObject jsstr = JSONObject.parseObject(cmd);
        DangerSourceInfo form=JSONObject.toJavaObject(jsstr.getJSONObject("form"),DangerSourceInfo.class);
        List<FacilitiesCondition> processTable=JSONObject.parseArray(jsstr.getString("processTable"),FacilitiesCondition.class);
        List<LegalProtection> certTable=JSONObject.parseArray(jsstr.getString("certTable"),LegalProtection.class);
        List<CompanyChemical> chemicalTable=JSONObject.parseArray(jsstr.getString("chemicalTable"),CompanyChemical.class);
        if("".equals(form.getSourceId()) ||form.getSourceId()==null){
            String SourceId= UUID.randomUUID().toString();
            form.setSourceId(SourceId);
            this.majorDSInfoEntryMapper.saveData(form);
            String AccidentType=form.getAccidentType();
            if(AccidentType!=null) {
                String[] industry = AccidentType.split(",");
                this.majorDSInfoEntryMapper.saveSGLXData(industry, SourceId);
            }
            if(processTable.size()>0) {
                this.majorDSInfoEntryMapper.saveEquipData(processTable, SourceId);
            }
            if(certTable.size()>0) {
                this.majorDSInfoEntryMapper.saveLegalData(certTable, SourceId);
            }
            if(chemicalTable.size()>0) {
                this.majorDSInfoEntryMapper.saveChemicalData(chemicalTable, SourceId);
            }
            return SourceId;
        }else{
            String sourceId = form.getSourceId();
            String AccidentType=form.getAccidentType();
            form.setSourceId(sourceId);
            if(AccidentType!=null) {
                String[] industry = AccidentType.split(",");
                this.majorDSInfoEntryMapper.updateData(form);
                this.majorDSInfoEntryMapper.saveSGLXData(industry, sourceId);
            }
            if(processTable.size()>0) {
                this.majorDSInfoEntryMapper.saveEquipData(processTable, sourceId);
            }
            if(certTable.size()>0) {
                this.majorDSInfoEntryMapper.saveLegalData(certTable, sourceId);
            }
            if(chemicalTable.size()>0) {
                this.majorDSInfoEntryMapper.saveChemicalData(chemicalTable, sourceId);
            }
            return "";
        }
    }


    /**
     * 通过ID获取危险源化学品信息
     * @param sourceId
     * @return
     */
    @Override
    public List<CompanyChemical> getChemicalList(String sourceId) {
        return majorDSInfoEntryMapper.getChemicalList(sourceId);
    }


    /**
     * 获取化学品列表
     * @param param 过滤条件
     * @return
     */
    @Override
    public Page<ChemicalCataLog> getChemicalInfoByCompany(Page page, Map<String, String> param) {
        page.setRecords(majorDSInfoEntryMapper.getChemicalInfoByCompany(page,param.get("chemName"),param.get("cas"),param.get("companyId")));
        return page;
    }

    /**
     * 文件导入
     * @param file
     * @return
     */
    @Transactional
    @Override
    public String inputFile(MultipartFile file) {
        InputStream is;
        POIFSFileSystem fs;
        Workbook book =null;
        String result ="";
        //保存所用公司
        Map<String,String> company=getCompany();
        //存放危险源名称，公司名称，危险源Id
        Map<String,String> dCIs=new HashedMap<String,String>();

        Map<String,String> dS=getDangerSource();
        //存放每页导入的所有数据
        Map<String,Object> all=new HashMap<String,Object>();

        //保存重大危险源成功条数
        int dn =0;
        //保存装置设施成功条数
        int fn=0;
        //保存法律保护区成功条数
        int ln =0;
        //保存危险源相关化学品成功条数
        int cn=0;
        //保存重大危险源信息
        List<DangerSourceInfo> dSs =new ArrayList<DangerSourceInfo>();
        //保存危险源与事故类型联系
        List<AccidentType> aTs=new ArrayList<AccidentType>();
        //保存装置设施信息
        List<FacilitiesCondition> fCs =new ArrayList<FacilitiesCondition>();
        //保存法律保护区
        List<LegalProtection> lPs=new ArrayList<LegalProtection>();
        //保存危险源相关化学品
        List<DangerSourceChemical> dSCs=new ArrayList<DangerSourceChemical>();
        try {
            is = file.getInputStream();
            /**判断Excel版本*/
            if (file.getOriginalFilename().indexOf("xlsx")>-1){
                book = new XSSFWorkbook(is);
            }
            else {
                fs = new POIFSFileSystem(is);
                book = new HSSFWorkbook(fs);
            }
            if (book.getNumberOfSheets() ==0) {
                return "获取工作簿失败，请重新上传";
            }
            //导入第一页的重大危险源信息
            Sheet sheet = book.getSheetAt(0);
            int rowSum=sheet.getLastRowNum();
            if(rowSum>=2){
                all=importDangerSourceInfo(sheet,dn,dSs,aTs,company,dCIs);
                if(!"".equals(all.get("result"))){
                    return all.get("result").toString();
                }
                dn=Integer.parseInt(all.get("dn").toString());
                dSs=(List<DangerSourceInfo>)all.get("dSs");
                dCIs=(Map<String,String>)all.get("dCIs");
                aTs=(List<AccidentType>)all.get("aTs");

            }
            //导入第二页的装置设施周围环境信息
            Sheet sheet1 = book.getSheetAt(1);
            int rowSum1=sheet1.getLastRowNum();
            if(rowSum1>=2){
                all=importFacilitiesCondition(sheet1,fn,fCs,company,dCIs,dS);
                if(!"".equals(all.get("result"))){
                    return all.get("result").toString();
                }
                fn=Integer.parseInt(all.get("fn").toString());
                fCs=(List<FacilitiesCondition>)all.get("fCs");

            }
            //导入第三页的法律保护区信息
            Sheet sheet2 = book.getSheetAt(2);
            int rowSum2=sheet2.getLastRowNum();
            if(rowSum2>=2){
                all=importLegalProtection(sheet2,ln,lPs,company,dCIs,dS);
                if(!"".equals(all.get("result"))){
                    return all.get("result").toString();
                }
                ln=Integer.parseInt(all.get("ln").toString());
                lPs=(List<LegalProtection>)all.get("lPs");
            }
            //导入第四页的危险源相关化学品信息
            Sheet sheet3 = book.getSheetAt(3);
            int rowSum3=sheet3.getLastRowNum();
            if(rowSum3>=2){
                all=importChemical(sheet3,cn,dSCs,company,dCIs,dS);
                if(!"".equals(all.get("result"))){
                    return all.get("result").toString();
                }
                cn=Integer.parseInt(all.get("cn").toString());
                dSCs=(List<DangerSourceChemical>)all.get("dSCs");
            }
            if(rowSum<2&&rowSum1<2&&rowSum2<2&&rowSum3<2){
                return "文件中无数据，请填写数据再进行录入";
            }

        }catch (Exception e){
            return "导入失败"+e.getMessage();
        }
        //添加数据
        if (dSs.size()>0){
            //添加事故类型信息
            this.majorDSInfoEntryMapper.insertAccidentType(aTs);
            //添加重大危险源信息
            dn += this.majorDSInfoEntryMapper.insertDangerSourceInfo(dSs);
        }
        if (fCs.size()>0){
            //保存装置设施周围环境信息
            fn += this.majorDSInfoEntryMapper.insertFacilitiesCondition(fCs);
        }
        if (lPs.size()>0){
            //保存法律保护区信息
            ln += this.majorDSInfoEntryMapper.insertLegalProtection(lPs);
        }
        if (dSCs.size()>0){
            //保存危险源相关化学品信息
            cn += this.majorDSInfoEntryMapper.insertDangerSourceChemical(dSCs);
        }

        return "成功插入重大危险源信息"+dn+"条。<br>成功插入装置设施周围环境信息"+fn+
                "条。<br>成功插入法律保护区信息"+ln+"条。<br>成功插入危险源相关化学品信息"+cn+"条";
    }

    /**
     * 导入重大危险源信息
     *
     */
    private Map<String,Object> importDangerSourceInfo(Sheet sheet,int n,List<DangerSourceInfo> dSs,List<AccidentType> aTs,Map<String,String> company,Map<String,String> dCIs){
        //存放要传出数据
        Map<String,Object> all=new HashMap<String,Object>();
        //存放企业Id
        String companyId="";
        //存放危险源Id
        String sourceName="";
        //存放文件中危险源的唯一编码
        List<String> dUniqueCode=new ArrayList<>();
        //获取所有事故类型
        Map<String,String> aT=getAccidentType();
        //获取所有危险源等级
        Map<String,String> dR=getDangerSourceRank();
        //获取所有状态
        Map<String,String> dS=getStatus();
        //获取此文件中最后一条数据行数
        int rowCount = sheet.getLastRowNum()+1;
        //从第二行开始读取表头
        Row firstRow = sheet.getRow(1);
        //获取列数
        int columCount = firstRow.getLastCellNum();
        Map<String,Integer> colum =new HashMap<String, Integer>();
        for (int i=0;i<columCount;i++){
            //获取第一行i列的内容
            String columText = firstRow.getCell(i).getStringCellValue();
            if (columText!=null){
                colum.put(columText,i);
            }
        }

        //从第三行开始进行数据遍历
        for (int i=2;i<rowCount;i++)
        {
            Row row = sheet.getRow(i);
            //如果此行是空的就遍历下一行
            if (row==null){
                continue;
            }
            DangerSourceInfo dangerSourceInfo =new DangerSourceInfo();
            AccidentType accidentType=new AccidentType();
            String value ="-";

            //从之前存储在集合中的键值对进行对比
            Cell cell =row.getCell(colum.get("序号"));
            //如果有序号列
            if (cell!=null){
                value=getCellValue(cell);
            }

            cell =row.getCell(colum.get("企业名称"));
            if (cell!=null){
                //获取其值
                value=getCellValue(cell);
                //与获取的所有企业的集合元素进行比对取出CompanyId
                if (value!=null&&company.containsKey(value)){
                    value=company.get(value);
                    companyId=value;
                }else {
                    all.put("result","导入失败：第1页第"+ (i+1) + "行企业名称未找到指定对象，请核对后再次导入");
                    return all;
                }
            }else{
                all.put("result","导入失败：第1页第"+ (i+1) + "行企业名称不能为空，请核对后再次导入");
                return all;
            }
            dangerSourceInfo.setCompanyId(companyId);

            cell =row.getCell(colum.get("重大危险源名称"));
            if (cell!=null){
                value=getCellValue(cell);
            }else{
                all.put("result","导入失败：第1页第"+ (i+1) + "行重大危险源名称不能为空，请核对后再次导入");
                return all;
            }
            sourceName=value;
            dangerSourceInfo.setSourceName(value);

            cell =row.getCell(colum.get("危险源唯一编码"));
            if (cell!=null){
                value=getCellValue(cell);
                //如果没有值那就有问题，跳出循环
                if ("".equals(value)||value==null){
                    break;
                }
                if(dUniqueCode.size()!=0){
                    if(dUniqueCode.contains(value)){
                        all.put("result","导入失败第1页第"+ (i+1)+ "行含有重复的危险源唯一编码，请重新输入后再次导入");
                        return all;
                    }
                }
                dUniqueCode.add(value);

                if(this.processUnitMapper.validateUniqueCode(value)!=0){
                    all.put("result","导入失败：第1页第"+ (i+1)+ "行危险源唯一编码在数据库中已存在，请重新输入后再次导入");
                    return all;
                }
            }else{
                all.put("result","导入失败：第1页第"+ (i+1) + "行危险源唯一编码不能为空，请核对后再次导入");
                return all;
            }
            dangerSourceInfo.setUniqueCode(value);

            //设置主键随机码
            String uid=UUID.randomUUID().toString();
            dangerSourceInfo.setSourceId(uid);


            cell =row.getCell(colum.get("事故类型"));
            if (cell!=null){
                //通过各种方法获取序号列此行的值
                value=getCellValue(cell);
                //与获取的所有事故类型并比对
                if(aT.containsKey(value)){
                    value=aT.get(value);
                    accidentType.setSourceId(uid);
                    accidentType.setTypeId(value);
                    aTs.add(accidentType);
                }else {
                    if(value.contains("_")){
                        //分割字符串
                        String[] types=value.split("_");
                        for(String t:types){
                            if(!t.equals("")){
                                if(aT.containsKey(t)){
                                    value=aT.get(t);
                                    accidentType.setSourceId(uid);
                                    accidentType.setTypeId(value);
                                    aTs.add(accidentType);
                                }

                            }
                        }
                    }
                }
                if(aTs.size()==0){
                    all.put("result","导入失败：第1页第"+ (i+1) + "行事故类型未找到指定对象,如果是多选，请用'_'分割，请核对后再次导入");
                    return all;
                }
            }else{
                all.put("result","导入失败：第1页第"+ (i+1) + "行事故类型不能为空，请核对后再次导入");
                return all;
            }

            cell =row.getCell(colum.get("R值"));
            if (cell!=null){
                value=getCellValue(cell);
                //如果没有值那就有问题，跳出循环
                if (!isNumeric(value)){
                    all.put("result","导入失败：第1页第"+ (i+1)+ "行R值不是数字，请核对后再次导入");
                    return all;
                }
            }else{
                all.put("result","导入失败：第1页第"+ (i+1) + "行R值不能为空，请核对后再次导入");
                return all;
            }
            dangerSourceInfo.setRValue(value);

            cell =row.getCell(colum.get("危险源等级"));
            if (cell!=null){
                value=getCellValue(cell);
                //与获取的所有危险源等级并比对
                if (value!=null&&dR.containsKey(value)){
                    value=dR.get(value);
                }else {
                    all.put("result","导入失败：第1页第"+ (i+1) + "行危险源等级未找到指定对象，请核对后再次导入");
                    return all;
                }
            }else{
                all.put("result","导入失败：第1页第"+ (i+1) + "行危险源等级不能为空，请核对后再次导入");
                return all;
            }
            dangerSourceInfo.setRank(value);

            cell =row.getCell(colum.get("备案编号"));
            if (cell!=null){
                value=getCellValue(cell);
            }else{
                value="";
            }
            dangerSourceInfo.setRecordNo(value);

            cell =row.getCell(colum.get("登记日期"));
            if (cell!=null){
                value=getCellValue(cell);
                if(!checkDate(value)){
                    all.put("result","导入失败：第1页第"+ (i+1) + "行登记日期未按正确格式输入，格式为yyyy-mm-dd");
                    return all;
                }
            }else{
                all.put("result","导入失败：第1页第"+ (i+1) + "行登记日期不能为空，请核对后再次导入");
                return all;
            }
            dangerSourceInfo.setRecordDate(value);

            cell =row.getCell(colum.get("有效期"));
            if (cell!=null){
                value=getCellValue(cell);
                if(!checkDate(value)){
                    all.put("result","导入失败：第1页第"+ (i+1) + "行有效期未按正确格式输入，格式为yyyy-mm-dd");
                    return all;
                }
            }else{
                all.put("result","导入失败：第1页第"+ (i+1) + "行有效期不能为空，请核对后再次导入");
                return all;
            }
            dangerSourceInfo.setValidity(value);

            cell =row.getCell(colum.get("经度"));
            if (cell!=null){
                value=getCellValue(cell);
            }else{
                all.put("result","导入失败：第1页第"+ (i+1) + "行经度不能为空，请核对后再次导入");
                return all;
            }
            dangerSourceInfo.setLongt(value);

            cell =row.getCell(colum.get("纬度"));
            if (cell!=null){
                value=getCellValue(cell);
            }else{
                all.put("result","导入失败：第1页第"+ (i+1) + "行纬度不能为空，请核对后再次导入");
                return all;
            }
            dangerSourceInfo.setLat(value);

            cell =row.getCell(colum.get("状态"));
            if (cell!=null){
                value=getCellValue(cell);
                //与获取的所有状态并比对
                if (value!=null&&dS.containsKey(value)){
                    value=dS.get(value);
                }else {
                    all.put("result","导入失败：第1页第"+ (i+1) + "行状态未找到指定对象，请核对后再次导入");
                    return all;
                }
            }else{
                all.put("result","导入失败：第1页第"+ (i+1) + "行状态不能为空，请核对后再次导入");
                return all;
            }
            dangerSourceInfo.setStatus(value);

            cell =row.getCell(colum.get("可能引发事故死亡人数"));
            if (cell!=null){
                value=getCellValue(cell);
            }else{
                all.put("result","导入失败：第1页第"+ (i+1) + "行可能引发事故死亡人数不能为空，请核对后再次导入");
                return all;
            }
            dangerSourceInfo.setDeathToll(value);

            cell =row.getCell(colum.get("厂区边界外500米范围人数估值"));
            if (cell!=null){
                value=getCellValue(cell);
            }else{
                all.put("result","导入失败：第1页第"+ (i+1) + "行厂区边界外500米范围人数估值不能为空，请核对后再次导入");
                return all;
            }
            dangerSourceInfo.setOutPersonCount(value);


            dSs.add(dangerSourceInfo);
            //为了供后面三个表使用
            dCIs.put(sourceName+","+companyId,uid);
            //如果条数太多，那就先保存100条
            if (dSs.size()>100){
                //添加重大危险源信息
                n += this.majorDSInfoEntryMapper.insertDangerSourceInfo(dSs);
                //新建空集合
                dSs = new ArrayList<DangerSourceInfo>();
            }
            if(aTs.size()>100){
                //添加事故类型信息
                this.majorDSInfoEntryMapper.insertAccidentType(aTs);
                //新建空集合
                aTs = new ArrayList<AccidentType>();
            }
        }
        all.put("result","");
        all.put("aTs",aTs);
        all.put("dSs",dSs);
        all.put("dn",n);
        all.put("dCIs",dCIs);
        return all;
    }

    /**
     * 导入装置设施周围环境信息
     *
     */
    private Map<String,Object> importFacilitiesCondition(Sheet sheet,int n,List<FacilitiesCondition> fCs,Map<String,String> company,Map<String,String> dCIs,Map<String,String> dS){

        Map<String,Object> all=new HashMap<String,Object>();
        //企业ID
        String companyId="";
        //获取此文件中最后一条数据行数
        int rowCount = sheet.getLastRowNum()+1;
        //从第二行开始读取表头
        Row firstRow = sheet.getRow(1);
        //获取列数
        int columCount = firstRow.getLastCellNum();
        Map<String,Integer> colum =new HashMap<String, Integer>();
        for (int i=0;i<columCount;i++){
            //获取第一行i列的内容
            String columText = firstRow.getCell(i).getStringCellValue();
            if (columText!=null){
                colum.put(columText,i);
            }
        }

        //从第三行开始进行数据遍历
        for (int i=2;i<rowCount;i++)
        {
            Row row = sheet.getRow(i);
            //如果此行是空的就遍历下一行
            if (row==null){
                continue;
            }
            FacilitiesCondition facilitiesCondition=new FacilitiesCondition();
            String value ="-";
            //从之前存储在集合中的键值对进行对比
            Cell cell =row.getCell(colum.get("序号"));
            //如果有序号列
            if (cell!=null){
                value=getCellValue(cell);
            }

            cell =row.getCell(colum.get("企业名称"));
            if (cell!=null){
                //获取其值
                value=getCellValue(cell);
                //与获取的所有企业的集合元素进行比对取出CompanyId
                if (value!=null&&company.containsKey(value)){
                    value=company.get(value);
                    companyId=value;
                }else {
                    all.put("result","导入失败：第2页第"+ (i+1) + "行企业名称未找到指定对象，请核对后再次导入");
                    return all;
                }
            }else{
                all.put("result","导入失败：第2页第"+ (i+1) + "行企业名称不能为空，请核对后再次导入");
                return all;
            }

            cell =row.getCell(colum.get("重大危险源名称"));
            if (cell!=null){
                value=getCellValue(cell);
                if (value!=null){
                    if(!dS.containsKey(value)){
                        String str=value+","+companyId;
                        if(dCIs.containsKey(str)){
                            facilitiesCondition.setSourceId(dCIs.get(str));
                        }
                    }else{
                        //判断此危险源是不是前一列公司的危险源
                        DangerSourceInfo dangerSourceInfo=this.processUnitMapper.check(value,companyId);
                        if(dangerSourceInfo!=null){
                            facilitiesCondition.setSourceId(dangerSourceInfo.getSourceId());
                        }
                    }
                    if(facilitiesCondition.getSourceId()==null){
                        all.put("result","导入失败：第2页第"+ (i+1) + "行重大危险源名称非此企业名称下危险源，请核对后再次导入");
                        return all;
                    }
                }else {
                    all.put("result","导入失败：第2页第"+ (i+1) + "行重大危险源名称未找到指定对象，请核对后再次导入");
                    return all;
                }
            }else{
                all.put("result","导入失败：第2页第"+ (i+1) + "行重大危险源名称不能为空，请核对后再次导入");
                return all;
            }

            cell =row.getCell(colum.get("装置设施名称"));
            if (cell!=null){
                value=getCellValue(cell);
            }else{
                all.put("result","导入失败：第2页第"+ (i+1) + "行装置设施名称不能为空，请核对后再次导入");
                return all;
            }
            facilitiesCondition.setFacilities(value);

            cell =row.getCell(colum.get("周边环境名称"));
            if (cell!=null){
                value=getCellValue(cell);
            }else{
                all.put("result","导入失败：第2页第"+ (i+1) + "行周边环境名称不能为空，请核对后再次导入");
                return all;
            }
            facilitiesCondition.setEnvironment(value);

            cell =row.getCell(colum.get("实际距离（米）"));
            if (cell!=null){
                value=getCellValue(cell);
                //如果没有值那就有问题，跳出循环
                if (!isNumeric(value)){
                    all.put("result","导入失败：第2页第"+ (i+1) + "行实际距离（米）不是数字，请核对后再次导入");
                    return all;
                }
            }else{
                all.put("result","导入失败：第2页第"+ (i+1) + "行实际距离（米）不能为空，请核对后再次导入");
                return all;
            }
            facilitiesCondition.setRealDistance(Double.parseDouble(value));

            cell =row.getCell(colum.get("标准要求（米）"));
            if (cell!=null){
                value=getCellValue(cell);
                //如果没有值那就有问题，跳出循环
                if (!isNumeric(value)){
                    all.put("result","导入失败：第2页第"+ (i+1) + "行标准要求（米）不是数字，请核对后再次导入");
                    return all;
                }
            }else{
                all.put("result","导入失败：第2页第"+ (i+1) + "行标准要求（米）不能为空，请核对后再次导入");
                return all;
            }
            facilitiesCondition.setStandardDistance(Double.parseDouble(value));

            cell =row.getCell(colum.get("与标准符合性"));
            if (cell!=null){
                value=getCellValue(cell);
                if(!value.equals("符合")&&!value.equals("不符合")){
                    all.put("result","导入失败：第2页第"+ (i+1) + "行与标准符合性未按正确格式输入，请核对后再次导入");
                    return all;
                }

            }else{
                all.put("result","导入失败：第2页第"+ (i+1) + "行与标准符合性不能为空，请核对后再次导入");
                return all;
            }
            facilitiesCondition.setConformance(value);

           facilitiesCondition.setTargetId(UUID.randomUUID().toString());

            fCs.add(facilitiesCondition);
            //如果条数太多，那就先保存100条
            if (fCs.size()>100){
                n += this.majorDSInfoEntryMapper.insertFacilitiesCondition(fCs);
                //新建空集合
                fCs = new ArrayList<FacilitiesCondition>();
            }
        }
        all.put("result","");
        all.put("fCs",fCs);
        all.put("fn",n);
        return all;
    }

    /**
     * 导入法律保护区信息
     *
     */
    private Map<String,Object> importLegalProtection(Sheet sheet,int n,List<LegalProtection> lPs,Map<String,String> company,Map<String,String> dCIs,Map<String,String> dS){

        Map<String,Object> all=new HashMap<String,Object>();
        //企业ID
        String companyId="";
        //获取此文件中最后一条数据行数
        int rowCount = sheet.getLastRowNum()+1;
        //从第二行开始读取表头
        Row firstRow = sheet.getRow(1);
        //获取列数
        int columCount = firstRow.getLastCellNum();
        Map<String,Integer> colum =new HashMap<String, Integer>();
        for (int i=0;i<columCount;i++){
            //获取第一行i列的内容
            String columText = firstRow.getCell(i).getStringCellValue();
            if (columText!=null){
                colum.put(columText,i);
            }
        }

        //从第三行开始进行数据遍历
        for (int i=2;i<rowCount;i++)
        {
            Row row = sheet.getRow(i);
            //如果此行是空的就遍历下一行
            if (row==null){
                continue;
            }
            LegalProtection legalProtection=new LegalProtection();
            String value ="-";
            //从之前存储在集合中的键值对进行对比
            Cell cell =row.getCell(colum.get("序号"));
            //如果有序号列
            if (cell!=null){
                value=getCellValue(cell);
            }

            cell =row.getCell(colum.get("企业名称"));
            if (cell!=null){
                value=getCellValue(cell);
                //与获取的所有企业的集合元素进行比对取出CompanyId
                if (value!=null&&company.containsKey(value)){
                    value=company.get(value);
                    companyId=value;
                }else {
                    all.put("result","导入失败：第3页第"+ (i+1) + "行企业名称未找到指定对象，请核对后再次导入");
                    return all;
                }
            }else{
                all.put("result","导入失败：第3页第"+ (i+1) + "行企业名称不能为空，请核对后再次导入");
                return all;
            }

            cell =row.getCell(colum.get("重大危险源名称"));
            if (cell!=null){
                value=getCellValue(cell);
                if (value!=null){
                    if(!dS.containsKey(value)){
                        String str=value+","+companyId;
                        if(dCIs.containsKey(str)){
                            legalProtection.setSourceId(dCIs.get(str));
                        }
                    }else{
                        //判断此危险源是不是前一列公司的危险源
                        DangerSourceInfo dangerSourceInfo=this.processUnitMapper.check(value,companyId);
                        if(dangerSourceInfo!=null){
                            legalProtection.setSourceId(dangerSourceInfo.getSourceId());
                        }
                    }
                    if(legalProtection.getSourceId()==null){
                        all.put("result","导入失败：第2页第"+ (i+1) + "行重大危险源名称非此企业名称下危险源，请核对后再次导入");
                        return all;
                    }
                }else {
                    all.put("result","导入失败：第2页第"+ (i+1) + "行重大危险源名称未找到指定对象，请核对后再次导入");
                    return all;
                }
            }else{
                all.put("result","导入失败：第2页第"+ (i+1) + "行重大危险源名称不能为空，请核对后再次导入");
                return all;
            }

            cell =row.getCell(colum.get("保护区"));
            if (cell!=null){
                value=getCellValue(cell);
            }else{
                all.put("result","导入失败：第3页第"+ (i+1) + "行保护区不能为空，请核对后再次导入");
                return all;
            }
            legalProtection.setProtectArea(value);

            cell =row.getCell(colum.get("周边环境说明"));
            if (cell!=null){
                value=getCellValue(cell);
            }else{
                all.put("result","导入失败：第3页第"+ (i+1) + "行周边环境说明不能为空，请核对后再次导入");
                return all;
            }
            legalProtection.setEnvironment(value);

            cell =row.getCell(colum.get("与规定符合性"));
            if (cell!=null){
                value=getCellValue(cell);
                if(!value.equals("符合")&&!value.equals("不符合")){
                    all.put("result","导入失败：第3页第"+ (i+1) + "行与规定符合性未按正确格式输入，请核对后再次导入");
                    return all;
                }
            }else{
                all.put("result","导入失败：第3页第"+ (i+1) + "行与规定符合性不能为空，请核对后再次导入");
                return all;
            }
            legalProtection.setConformance(value);

            legalProtection.setTargetId(UUID.randomUUID().toString());

            lPs.add(legalProtection);
            //如果条数太多，那就先保存100条
            if (lPs.size()>100){
                n += this.majorDSInfoEntryMapper.insertLegalProtection(lPs);
                //新建空集合
                lPs = new ArrayList<LegalProtection>();
            }
        }
        all.put("result","");
        all.put("lPs",lPs);
        all.put("ln",n);
        return all;
    }

    /**
     * 导入危险源相关化学品信息
     *
     */
    private Map<String,Object> importChemical(Sheet sheet,int n,List<DangerSourceChemical> dSCs,Map<String,String> company,Map<String,String> dCIs,Map<String,String> dS){
        Map<String,Object> all=new HashMap<String,Object>();
        ChemicalsInfo chemicalsInfo=null;
        //企业ID
        String companyId="";
        String chemicalName="";
        //获取此文件中最后一条数据行数
        int rowCount = sheet.getLastRowNum()+1;
        //从第二行开始读取表头
        Row firstRow = sheet.getRow(1);
        //获取列数
        int columCount = firstRow.getLastCellNum();
        Map<String,Integer> colum =new HashMap<String, Integer>();
        for (int i=0;i<columCount;i++){
            //获取第一行i列的内容
            String columText = firstRow.getCell(i).getStringCellValue();
            if (columText!=null){
                colum.put(columText,i);
            }
        }

        //从第三行开始进行数据遍历
        for (int i=2;i<rowCount;i++)
        {
            Row row = sheet.getRow(i);
            //如果此行是空的就遍历下一行
            if (row==null){
                continue;
            }
            DangerSourceChemical dangerSourceChemical=new DangerSourceChemical();
            String value ="-";
            //从之前存储在集合中的键值对进行对比
            Cell cell =row.getCell(colum.get("序号"));
            //如果有序号列
            if (cell!=null){
                value=getCellValue(cell);
            }

            cell =row.getCell(colum.get("企业名称"));
            if (cell!=null){
                //获取其值
                value=getCellValue(cell);
                //与获取的所有企业的集合元素进行比对取出CompanyId
                if (value!=null&&company.containsKey(value)){
                    value=company.get(value);
                    companyId=value;
                }else {
                    all.put("result","导入失败：第4页第"+ (i+1) + "行企业名称未找到指定对象，请核对后再次导入");
                    return all;
                }
            }else{
                all.put("result","导入失败：第4页第"+ (i+1) + "行企业名称不能为空，请核对后再次导入");
                return all;
            }

            cell =row.getCell(colum.get("重大危险源名称"));
            if (cell!=null){
                value=getCellValue(cell);
                if (value!=null){
                    if(!dS.containsKey(value)){
                        String str=value+","+companyId;
                        if(dCIs.containsKey(str)){
                            dangerSourceChemical.setSourceId(dCIs.get(str));
                        }
                    }else{
                        //判断此危险源是不是前一列公司的危险源
                        DangerSourceInfo dangerSourceInfo=this.processUnitMapper.check(value,companyId);
                        if(dangerSourceInfo!=null){
                            dangerSourceChemical.setSourceId(dangerSourceInfo.getSourceId());
                        }
                    }
                    if(dangerSourceChemical.getSourceId()==null){
                        all.put("result","导入失败：第2页第"+ (i+1) + "行重大危险源名称非此企业名称下危险源，请核对后再次导入");
                        return all;
                    }
                }else {
                    all.put("result","导入失败：第2页第"+ (i+1) + "行重大危险源名称未找到指定对象，请核对后再次导入");
                    return all;
                }
            }else{
                all.put("result","导入失败：第2页第"+ (i+1) + "行重大危险源名称不能为空，请核对后再次导入");
                return all;
            }

            cell =row.getCell(colum.get("化学品名称"));

            if (cell!=null){
                //获取其值
                value=getCellValue(cell);
                if(!value.equals("")){
                    //与获取的所有企业的集合元素进行比对取出CompanyId
                    chemicalsInfo=this.majorDSInfoEntryMapper.getObjectByName(value);
                    if(chemicalsInfo!=null){
                        chemicalName=value;
                    }
                    else {
                        all.put("result","导入失败：第4页第"+ (i+1) + "行化学品名称未找到指定对象，请核对后再次导入");
                        return all;
                    }
                }
            }else{
                all.put("result","导入失败：第4页第"+ (i+1) + "行化学品名称不能为空，请核对后再次导入");
                return all;
            }

            cell =row.getCell(colum.get("CAS"));
            if (cell!=null){
                value=getCellValue(cell);
            }else{
                value="";
            }
            //判断化学品名称和CAS是否匹配
            chemicalsInfo=this.majorDSInfoEntryMapper.piPei(chemicalName,value);

            if(chemicalsInfo!=null){
                value=chemicalsInfo.getChemId();
            }else {
                all.put("result","导入失败：第4页第"+ (i+1) + "行CAS与化学品名称不匹配，请核对后再次导入");
                return all;
            }
            dangerSourceChemical.setChemId(value);

            cell =row.getCell(colum.get("设计储量"));
            if (cell!=null){
                value=getCellValue(cell);
                //如果没有值那就有问题，跳出循环
                if (!isNumeric(value)){
                    all.put("result", "导入失败：第4页第"+ (i+1) + "行设计储量不是数字，请核对后再次导入");
                    return all;
                }
            }else{
                all.put("result", "导入失败：第4页第"+ (i+1) + "行设计储量不能为空，请核对后再次导入");
                return all;
            }
            dangerSourceChemical.setDreserves(Double.parseDouble(value));


            cell =row.getCell(colum.get("单位"));
            if (cell!=null){
                value=getCellValue(cell);
            }else{
                all.put("result","导入失败：第4页第"+ (i+1) + "行单位不能为空，请核对后再次导入");
                return all;
            }
            dangerSourceChemical.setUnit(value);


            dangerSourceChemical.setRelId(UUID.randomUUID().toString());

            dSCs.add(dangerSourceChemical);
            //如果条数太多，那就先保存100条
            if (dSCs.size()>100){
                n += this.majorDSInfoEntryMapper.insertDangerSourceChemical(dSCs);
                //新建空集合
                dSCs = new ArrayList<DangerSourceChemical>();
            }
        }
        all.put("result","");
        all.put("dSCs",dSCs);
        all.put("cn",n);
        return all;
    }

    /**
     * 获取化学品名称信息
     * @return
     */



    /**
     * 判断String是否是数字
     * @param str
     * @return
     */
    private static boolean isNumeric(String str){
        String reg = "^[0-9]+(.[0-9]+)?$";
        return str.matches(reg);
    }

    /**
     * 获取所有的企业集合
     * @return
     */
    private Map<String,String> getCompany() {
        List<Map<String,String>> cs=this.processUnitMapper.getCompany();
        Map<String,String> map=new HashMap<String,String>();
        for(Map<String,String> c:cs){
            map.put(c.get("CompanyName"),c.get("CompanyId"));
        }
        return map;
    }

    /**
     * 获取所有的重大危险源
     * @return
     */
    private Map<String,String> getDangerSource() {
        List<Map<String,String>> ds=this.processUnitMapper.getDangerSource();
        Map<String,String> map=new HashMap<String,String>();
        for(Map<String,String> d:ds){
            map.put(d.get("SourceName"),d.get("SourceId"));
        }
        return map;
    }

    /**
     * 获取所有的危险源等级
     * @return
     */
    private Map<String,String> getDangerSourceRank() {
        List<Map<String,String>> cs=this.majorDSInfoEntryMapper.getDangerSourceRank();
        Map<String,String> map=new HashMap<String,String>();
        for(Map<String,String> c:cs){
            map.put(c.get("DictName"),c.get("DictId"));
        }
        return map;
    }

    /**
     * 获取所有的状态
     * @return
     */
    private Map<String,String> getStatus() {
        List<Map<String,String>> cs=this.majorDSInfoEntryMapper.getStatus();
        Map<String,String> map=new HashMap<String,String>();
        for(Map<String,String> c:cs){
            map.put(c.get("DictName"),c.get("DictId"));
        }
        return map;
    }

    /**
     * 获取所有的事故类型
     * @return
     */
    private Map<String,String> getAccidentType() {
        List<Map<String,String>> cs=this.majorDSInfoEntryMapper.getAccidentType();
        Map<String,String> map=new HashMap<String,String>();
        for(Map<String,String> c:cs){
            map.put(c.get("DictName"),c.get("DictId"));
        }
        return map;
    }

    /**
     * 判断日期格式
     * @param str
     * @return
     */
    private boolean checkDate(String str){
        String eL = "[0-9]{4}-[0-9]{2}-[0-9]{2}";
        Pattern p = Pattern.compile(eL);
        Matcher m = p.matcher(str);
        return m.matches();
    }

    /**
     * 获取cell值
     * @param cell
     * @return
     */
    private String getCellValue(Cell cell) {
        String cellValue = "";
        if (cell == null){
            return "";
        }
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
        CellType cellType =cell.getCellTypeEnum();
        //字符串型
        if (cellType == CellType.STRING){
            cellValue = cell.getStringCellValue();
        }
        //数值型
        else if (cellType == CellType.NUMERIC){
            //判断是否为日期格式
            if (DateUtil.isCellDateFormatted(cell)){
                cellValue=sdf.format(cell.getDateCellValue());
            }else {
                if(cell.getNumericCellValue()-(int)cell.getNumericCellValue()>0){
                    cellValue = String.valueOf(cell.getNumericCellValue());
                }else{
                    cellValue = String.valueOf((int)cell.getNumericCellValue());
                }
            }
        }
        //Boolean
        else if (cellType == CellType.BOOLEAN){
            cellValue = String.valueOf(cell.getBooleanCellValue());
        }
        //空值
        else if (cellType == CellType.BLANK){
            cellValue = "";
        }
        return cellValue;
    }


}
