package top.egon.enjoyshop.goods.application.user.manage;

import top.egon.enjoyshop.goods.application.user.pojo.command.GrantPermissionCommand;
import top.egon.enjoyshop.goods.application.user.pojo.query.GetUserPermissionsQuery;
import top.egon.enjoyshop.goods.application.user.pojo.result.PermissionDetailResult;
import top.egon.enjoyshop.goods.application.user.pojo.result.PermissionResult;

import java.util.List;

public interface PermissionManage {
    PermissionResult grantPermission(GrantPermissionCommand command);

    List<PermissionDetailResult> getByUser(GetUserPermissionsQuery query);
}
