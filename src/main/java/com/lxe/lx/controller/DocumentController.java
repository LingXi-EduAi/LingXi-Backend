package com.lxe.lx.controller;

import com.lxe.lx.annotation.Login;
import com.lxe.lx.annotation.TeacherOnly;
import com.lxe.lx.domain.dto.DocumentDTO;
import com.lxe.lx.domain.dto.ValidDTO;
import com.lxe.lx.domain.qo.DocumentQO;
import com.lxe.lx.pojo.Document;
import com.lxe.lx.pojo.TokenEntity;
import com.lxe.lx.service.DocumentService;
import com.lxe.lx.util.ResultConstant;
import com.lxe.lx.util.Tools;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.List;

import static com.lxe.lx.config.AuthorizationInterceptor.ORG_ID_KEY;

@RestController
@RequestMapping("/document")
public class DocumentController {
    Logger logger = LogManager.getLogger(DocumentController.class);
    @Autowired
    private DocumentService documentService;

    @Login
    @TeacherOnly
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public ResultConstant add(HttpServletRequest request, @RequestBody Document document) {
        if (document == null || StringUtils.isBlank(document.getName())) {
            return ResultConstant.illegalParams("资料名称不能为空");
        } else if (StringUtils.isBlank(document.getType())) {
            return ResultConstant.illegalParams("类型不能为空");
        }else if (StringUtils.isBlank(document.getFileAddress())) {
            return ResultConstant.illegalParams("url不能为空");
        }
        try {
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            document.setId(Tools.get32UUID());
            document.setCreateId(tokenEntity.getId());
            document.setCreateTime(Tools.nowTimeStr());
            document.setState("1");
            document.setVersion(1);
            ResultConstant ref = documentService.add(document);
            return ref;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("add->error" + e.getMessage());
            return ResultConstant.error("新建文档资料失败");
        }
    }
    @Login
    @TeacherOnly
    @RequestMapping(value = "/edit", method = RequestMethod.POST)
    public ResultConstant edit(HttpServletRequest request, @RequestBody Document document) {
        if (document == null || StringUtils.isBlank(document.getName())) {
            return ResultConstant.illegalParams("资料名称不能为空");
        } else if (StringUtils.isBlank(document.getType())) {
            return ResultConstant.illegalParams("类型不能为空");
        }else if (StringUtils.isBlank(document.getFileAddress())) {
            return ResultConstant.illegalParams("url不能为空");
        }
        try{
            TokenEntity tokenEntity = (TokenEntity) request.getAttribute(ORG_ID_KEY);
            Document temp = documentService.getDocumentById(document.getId());
            if(temp==null){
                return ResultConstant.error("文档资料不存在");
            }
            if (!temp.getVersion().equals(document.getVersion())) {
                return ResultConstant.error("数据已修改，请刷新后重试");
            }
            document.setUpdateId(tokenEntity.getId());
            document.setUpdateTime(Tools.nowTimeStr());
            ResultConstant ref = documentService.edit(document);
            return ref;
        }catch (Exception e) {
            e.printStackTrace();
            logger.error("edit->error" + e.getMessage());
            return ResultConstant.error("修改失败");
        }
    }
    @Login
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public ResultConstant list(HttpServletRequest request, @RequestBody DocumentQO documentQO) throws Exception {
        ValidDTO validDTO = documentQO.validPageParams(documentQO);
        if (!validDTO.getResult()) {
            return ResultConstant.illegalParams(validDTO.getMsg());
        }
        try {
            DocumentDTO documentDTO = new DocumentDTO();
            int count = documentService.num(documentQO);
            if (count > 0) {
                List<Document> list = documentService.list(documentQO);
                documentDTO.setList(list);
            }
            return ResultConstant.success(documentDTO);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("list->error"+e.getMessage());
            return ResultConstant.error("查询失败");
        }
    }
    @Login
    @RequestMapping(value = "/detail", method = RequestMethod.POST)
    public ResultConstant detail(HttpServletRequest request,@RequestBody Document document) {
//    public ResultConstant detail(HttpServletRequest request, ClassGrouping classGrouping) {
        if (StringUtils.isBlank(document.getId())) {
            ResultConstant.illegalParams("id不能为空");
        }
        try {
            Document temp = documentService.getDocumentById(document.getId());
            if(temp!=null){
                return ResultConstant.success(temp);
            }else {
                return ResultConstant.error("当前文档资料不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();

            logger.error("detail->error"+e.getMessage());
            return ResultConstant.error("查看详情失败");
        }

    }
    @Login
    @TeacherOnly
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public ResultConstant delete(HttpServletResponse response, String id) throws Exception {
        if (StringUtils.isBlank(id)) {
            return ResultConstant.illegalParams("id不能为空");
        }
        try {
            Document document= documentService.getDocumentById(id);
            if (document == null) {
                return ResultConstant.error("当前文档资料不存在");
            }
            ResultConstant ref = documentService.delete(id);
            return ref;
        } catch (Exception e) {
            e.printStackTrace();

            logger.error("delete->error"+e.getMessage());
            return ResultConstant.error("删除失败");
        }
    }
}
