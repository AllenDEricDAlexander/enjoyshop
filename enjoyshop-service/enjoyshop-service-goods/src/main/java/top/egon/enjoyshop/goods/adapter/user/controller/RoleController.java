package top.egon.enjoyshop.goods.adapter.user.controller;

import lombok.extern.slf4j.Slf4j;
import top.egon.enjoyshop.goods.adapter.filter.RequestContext;
import top.egon.enjoyshop.goods.adapter.filter.RequestContextHolder;
import top.egon.enjoyshop.goods.adapter.user.pojo.convertor.UserAdapterConvertor;
import top.egon.enjoyshop.goods.adapter.user.pojo.dto.AssignRoleRequest;
import top.egon.enjoyshop.goods.adapter.user.pojo.vo.UserDetailVO;
import top.egon.enjoyshop.goods.application.user.pojo.command.AssignRoleCommand;
import top.egon.enjoyshop.goods.application.user.manage.RoleManage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/roles")
@RequiredArgsConstructor
@Slf4j
public class RoleController {
    private final RoleManage roleManage;
    private final UserAdapterConvertor convertor;

    @PostMapping
    public UserDetailVO assignRole(@PathVariable String userId, @Valid @RequestBody AssignRoleRequest request) {
        RequestContext context = RequestContextHolder.currentOrAnonymous();
        return convertor.toTarget(roleManage.assignRole(new AssignRoleCommand(
                Long.valueOf(userId),
                request.roleCode(),
                context.operatorId(),
                context.requestId())));
    }
}
