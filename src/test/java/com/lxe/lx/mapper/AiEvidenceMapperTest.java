package com.lxe.lx.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiEvidenceMapperTest {

    @Test
    void loadsAllEvidenceMappings() throws Exception {
        Configuration configuration = new Configuration();
        String resource = "mybatis/AiEvidenceMapper.xml";
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }

        assertTrue(configuration.hasStatement(statement("insert")));
        assertTrue(configuration.hasStatement(statement("findByMessageId")));
    }

    private String statement(String method) {
        return AiEvidenceMapper.class.getName() + "." + method;
    }
}
