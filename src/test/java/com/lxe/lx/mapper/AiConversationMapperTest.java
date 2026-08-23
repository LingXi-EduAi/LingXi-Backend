package com.lxe.lx.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AiConversationMapperTest {
    @Test
    void loadsAllConversationMappings() throws Exception {
        Configuration configuration = new Configuration();
        String resource = "mybatis/AiConversationMapper.xml";
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        for (String method : new String[]{
                "insert", "findByIdAndUser", "findByUser", "countByUser", "updateTitle", "softDelete"}) {
            assertTrue(configuration.hasStatement(AiConversationMapper.class.getName() + "." + method));
        }
    }
}
