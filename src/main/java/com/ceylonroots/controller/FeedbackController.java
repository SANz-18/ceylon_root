package com.ceylonroots.controller;

import com.ceylonroots.dto.FeedbackRequest;
import com.ceylonroots.dto.ReplyRequest;
import com.ceylonroots.model.Feedback;
import com.ceylonroots.model.User;
import com.ceylonroots.service.CurrentUserProvider;
import com.ceylonroots.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public Feedback submit(@Valid @RequestBody FeedbackRequest req) {
        User buyer = currentUserProvider.get();
        return feedbackService.submit(buyer, req);
    }

    @GetMapping
    public List<Feedback> list() {
        User user = currentUserProvider.get();
        return feedbackService.findForUser(user);
    }

    @PostMapping("/{id}/reply")
    @PreAuthorize("hasRole('ADMIN')")
    public Feedback reply(@PathVariable Long id, @Valid @RequestBody ReplyRequest req) {
        return feedbackService.reply(id, req.getReply());
    }
}
