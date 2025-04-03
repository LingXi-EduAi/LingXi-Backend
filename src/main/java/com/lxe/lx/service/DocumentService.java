package com.lxe.lx.service;

import com.lxe.lx.domain.qo.CustomerQO;
import com.lxe.lx.domain.qo.DocumentQO;
import com.lxe.lx.pojo.Customer;
import com.lxe.lx.pojo.Document;
import com.lxe.lx.util.ResultConstant;

import java.util.List;

public interface DocumentService {
    public ResultConstant add(Document document)throws Exception;
    public ResultConstant edit(Document document)throws Exception;
    public Document getDocumentById(String id)throws Exception;
    public ResultConstant delete(String id)throws Exception;
    public int num(DocumentQO documentQO)throws Exception;
    public List<Document> list(DocumentQO documentQO)throws Exception;
}
