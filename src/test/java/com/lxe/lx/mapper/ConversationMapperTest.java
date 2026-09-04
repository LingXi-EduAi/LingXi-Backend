package com.lxe.lx.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.ResultMap;
import org.apache.ibatis.mapping.ResultMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationMapperTest {
    @Test
    void mapsVersionAsInteger() throws Exception {
        Configuration configuration = new Configuration();
        String resource = "mybatis/ConversationMapper.xml";
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }

        assertTrue(configuration.hasStatement(ConversationMapper.class.getName() + ".edit"));
        ResultMap resultMap = configuration.getResultMap(ConversationMapper.class.getName() + ".Conversation");
        ResultMapping version = resultMap.getResultMappings().stream()
                .filter(mapping -> "version".equals(mapping.getProperty()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("version mapping is missing"));
        assertEquals(Integer.class, version.getJavaType());
        assertEquals(org.apache.ibatis.type.JdbcType.INTEGER, version.getJdbcType());
    }
}
