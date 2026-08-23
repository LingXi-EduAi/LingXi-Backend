package com.lxe.lx.mapper;

import com.lxe.lx.pojo.AiConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface AiConversationMapper {
    int insert(AiConversation conversation);

    AiConversation findByIdAndUser(
            @Param("id") String id,
            @Param("userId") String userId
    );

    List<AiConversation> findByUser(
            @Param("userId") String userId,
            @Param("state") String state,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    int countByUser(@Param("userId") String userId, @Param("state") String state);

    int updateTitle(AiConversation conversation);

    int softDelete(AiConversation conversation);
}
