package com.arn.ycyw.your_car_your_way.reposiory;

import com.arn.ycyw.your_car_your_way.entity.Conversation;
import com.arn.ycyw.your_car_your_way.entity.ConversationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Integer> {

    List<Conversation> findAllByCustomer_IdOrderByUpdatedAtDesc(Integer customerId);

    List<Conversation> findAllByEmployee_IdOrderByUpdatedAtDesc(Integer employeeId);

    List<Conversation> findAllByStatusOrderByUpdatedAtDesc(ConversationStatus status);

    @Query("SELECT c FROM Conversation c WHERE c.employee IS NULL AND c.status = :status ORDER BY c.createdAt ASC")
    List<Conversation> findUnassignedConversations(@Param("status") ConversationStatus status);

    @Query("SELECT c FROM Conversation c WHERE (c.customer.id = :userId OR c.employee.id = :userId) ORDER BY c.updatedAt DESC")
    List<Conversation> findAllByUserId(@Param("userId") Integer userId);
}
