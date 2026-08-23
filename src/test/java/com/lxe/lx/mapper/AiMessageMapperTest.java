package com.lxe.lx.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiMessageMapperTest {

    @Test
    void loadsAllMessageMappings() throws Exception {
        Configuration configuration = new Configuration();
        String resource = "mybatis/AiMessageMapper.xml";
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }

        assertTrue(configuration.hasStatement(statement("insert")));
        assertTrue(configuration.hasStatement(statement("findByDifyMessageId")));
        assertTrue(configuration.hasStatement(statement("findByTaskAndRole")));
        assertTrue(configuration.hasStatement(statement("findByConversation")));
        assertTrue(configuration.hasStatement(statement("countByConversation")));
    }

    private String statement(String method) {
        return AiMessageMapper.class.getName() + "." + method;
    }
}
