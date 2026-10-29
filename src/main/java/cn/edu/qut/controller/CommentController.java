package cn.edu.qut.controller;

import cn.edu.qut.common.Result;
import cn.edu.qut.dto.Requests;
import cn.edu.qut.entity.User;
import cn.edu.qut.service.CommentService;
import cn.edu.qut.support.web.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping
    public Result<?> list(@RequestParam("spotId") Long spotId,
                          @RequestParam(name = "page", required = false, defaultValue = "1") int page,
                          @RequestParam(name = "size", required = false, defaultValue = "10") int size) {
        return Result.ok(commentService.list(spotId, page, size));
    }

    @PostMapping
    public Result<?> add(@RequestBody Requests.CommentRequest request, HttpSession session) {
        User user = SessionUtils.requireUser(session);
        return Result.ok(commentService.add(user.getId(), request.spotId(), request.rating(), request.content()));
    }
}
