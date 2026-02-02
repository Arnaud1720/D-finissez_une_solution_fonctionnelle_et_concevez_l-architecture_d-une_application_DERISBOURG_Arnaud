package com.arn.ycyw.your_car_your_way.controller.bot;

import com.arn.ycyw.your_car_your_way.dto.bot.ChatRequest;
import com.arn.ycyw.your_car_your_way.dto.bot.ChatResponse;
import com.arn.ycyw.your_car_your_way.services.impl.OpenAiChatServiceImpl;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final OpenAiChatServiceImpl chatService;

    public ChatController(OpenAiChatServiceImpl chatService) {
        this.chatService = chatService;
    }


    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String reply = chatService.chat(request.getMessage());
        return new ChatResponse(reply);
    }
}
