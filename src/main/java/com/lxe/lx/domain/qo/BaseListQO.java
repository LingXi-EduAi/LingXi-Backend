
package com.lxe.lx.domain.qo;

import com.lxe.lx.domain.dto.ValidDTO;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
public class BaseListQO {
    //每页条数
    private Integer pageSize = 9999;//默认查询全部不分页
    //当前第X页
    private Integer currentPage = 1;
    //取数的起始数
    private int start = 0;
    //模糊查询的值
    private String condition;
    //0:模糊查询,1:精确查询
    private String queryType = "1";
    //排序字段名
    private String orderField = "id";
    //排序规则
    private String orderRule = "desc";
    //查询起始时间
    private String createTimeStart;
    //查询结束时间
    private String createTimeEnd;

    /**
     * 计算起始值
     * @param baseListQO
     * @return
     */
    public BaseListQO calculateStart(BaseListQO baseListQO){
        baseListQO.setStart((baseListQO.getCurrentPage() - 1) * baseListQO.getPageSize());
        return baseListQO;
    }

    public BaseListQO dealTimeSecond(BaseListQO baseListQO){
        if(StringUtils.isNotBlank(baseListQO.getCreateTimeStart())){
            baseListQO.setCreateTimeStart(baseListQO.getCreateTimeStart() + " 00:00:00");
        }
        if(StringUtils.isNotBlank(baseListQO.getCreateTimeEnd())){
            baseListQO.setCreateTimeEnd(baseListQO.getCreateTimeEnd() + " 23:59:59");
        }
        return baseListQO;
    }

    public ValidDTO validPageParams(BaseListQO baseListQO){
        ValidDTO validDTO = new ValidDTO();
        boolean result = true;
        String msg = "";
        if(baseListQO == null || baseListQO.getPageSize() == null || baseListQO.getPageSize() < 0){
            result = false;
            msg = "每页条数不能为空或者小于0";
        }else if(baseListQO.getCurrentPage() == null || baseListQO.getCurrentPage() < 1){
            result = false;
            msg = "当前页不能为空或者小于1";
        }
        validDTO.setResult(result);
        validDTO.setMsg(msg);
        if(result){
            baseListQO.calculateStart(baseListQO);
        }
        return validDTO;
    }
}

