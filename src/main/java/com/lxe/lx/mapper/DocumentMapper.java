package com.lxe.lx.mapper;

import com.lxe.lx.domain.qo.CustomerQO;
import com.lxe.lx.domain.qo.DocumentQO;
import com.lxe.lx.pojo.Customer;
import com.lxe.lx.pojo.Document;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Mapper
public interface DocumentMapper {
    void add(Document document);
    void edit(Document document);
    Document getDocumentById(String id);
    void deleteById(String id);
    int num(DocumentQO documentQO);
    List<Document> list(DocumentQO documentQO);
}
