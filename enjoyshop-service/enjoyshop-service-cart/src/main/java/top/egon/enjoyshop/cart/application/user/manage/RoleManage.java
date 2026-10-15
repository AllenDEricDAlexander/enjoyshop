package top.egon.enjoyshop.cart.application.user.manage;

import top.egon.enjoyshop.cart.application.user.pojo.command.AssignRoleCommand;
import top.egon.enjoyshop.cart.application.user.pojo.result.UserResult;

public interface RoleManage {
    UserResult assignRole(AssignRoleCommand command);
}
