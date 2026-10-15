package top.egon.enjoyshop.cart.adapter.user.controller;

import lombok.extern.slf4j.Slf4j;
import top.egon.enjoyshop.cart.adapter.filter.RequestContext;
import top.egon.enjoyshop.cart.adapter.filter.RequestContextHolder;
import top.egon.enjoyshop.cart.adapter.user.pojo.convertor.UserAdapterConvertor;
import top.egon.enjoyshop.cart.adapter.user.pojo.dto.CreateUserRequest;
import top.egon.enjoyshop.cart.adapter.user.pojo.vo.UserDetailVO;
import top.egon.enjoyshop.cart.application.user.pojo.command.CreateUserCommand;
import top.egon.enjoyshop.cart.application.user.manage.UserManage;
import top.egon.enjoyshop.cart.application.user.pojo.query.GetUserQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserManage userManage;
    private final UserAdapterConvertor convertor;

    @PostMapping
    public UserDetailVO create(@Valid @RequestBody CreateUserRequest request) {
        RequestContext context = RequestContextHolder.currentOrAnonymous();
        return convertor.toTarget(userManage.create(new CreateUserCommand(
                request.externalId(),
                request.name(),
                request.email(),
                context.operatorId(),
                context.requestId())));
    }

    @GetMapping("/{userId}")
    public UserDetailVO get(@PathVariable String userId) {
        return convertor.toTarget(userManage.get(new GetUserQuery(Long.valueOf(userId))));
    }
}
