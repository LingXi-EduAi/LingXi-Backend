package com.lxe.lx.service.impl;

import com.lxe.lx.domain.qo.CustomerQO;
import com.lxe.lx.domain.qo.DocumentQO;
import com.lxe.lx.mapper.DocumentMapper;
import com.lxe.lx.pojo.Customer;
import com.lxe.lx.pojo.Document;
import com.lxe.lx.service.DocumentService;
import com.lxe.lx.util.ResultConstant;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("DocumentService")
public class DocumentServiceImpl implements DocumentService {
    @Autowired
    private DocumentMapper documentMapper;

    @Override
    public ResultConstant add(Document document)throws Exception{
        try{
            documentMapper.add(document);
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }
    @Override
    public ResultConstant edit(Document document)throws Exception{
        try{
            documentMapper.edit(document);
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }
    @Override
    public Document getDocumentById(String id)throws Exception{
        return documentMapper.getDocumentById(id);
    }
    @Override
    public ResultConstant delete(String id)throws Exception{
        try{
            documentMapper.deleteById(id);
            return ResultConstant.success("");
        }catch (Exception e){
            e.printStackTrace();
            return ResultConstant.error(e.getMessage());
        }
    }
    @Override
    public int num(DocumentQO documentQO)throws Exception{
        return documentMapper.num(documentQO);
    }
    @Override
    public List<Document> list(DocumentQO documentQO)throws Exception{
        return documentMapper.list(documentQO);
    }
}
