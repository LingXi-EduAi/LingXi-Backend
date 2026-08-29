package com.lxe.lx.mapper;

import com.lxe.lx.pojo.AiConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface AiConfigMapper {
    int insert(AiConfig config);

    AiConfig findById(@Param("id") String id);

    List<AiConfig> findAll(@Param("env") String env);

    List<AiConfig> findByKey(@Param("configKey") String configKey, @Param("env") String env);

    AiConfig findActive(@Param("configKey") String configKey, @Param("env") String env);

    Integer maxVersion(@Param("configKey") String configKey, @Param("env") String env);

    int deactivateKey(@Param("configKey") String configKey, @Param("env") String env);

    int activate(@Param("id") String id);
}
