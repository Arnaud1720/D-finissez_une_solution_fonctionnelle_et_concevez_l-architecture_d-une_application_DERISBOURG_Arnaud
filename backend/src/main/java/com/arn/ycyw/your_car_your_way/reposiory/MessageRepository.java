package com.arn.ycyw.your_car_your_way.reposiory;

import com.arn.ycyw.your_car_your_way.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {

    List<Message> findAllByConversation_IdOrderBySentAtAsc(Integer conversationId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId AND m.isRead = false AND m.sender.id != :userId")
    Integer countUnreadMessages(@Param("conversationId") Integer conversationId, @Param("userId") Integer userId);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.conversation.id = :conversationId AND m.sender.id != :userId")
    void markAllAsRead(@Param("conversationId") Integer conversationId, @Param("userId") Integer userId);
}
