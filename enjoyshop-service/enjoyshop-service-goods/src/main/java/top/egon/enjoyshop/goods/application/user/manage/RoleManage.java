package top.egon.enjoyshop.goods.application.user.manage;

import top.egon.enjoyshop.goods.application.user.pojo.command.AssignRoleCommand;
import top.egon.enjoyshop.goods.application.user.pojo.result.UserResult;

public interface RoleManage {
    UserResult assignRole(AssignRoleCommand command);
}
