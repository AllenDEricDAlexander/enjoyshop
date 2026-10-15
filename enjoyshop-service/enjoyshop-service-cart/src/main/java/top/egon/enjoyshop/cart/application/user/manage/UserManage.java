package top.egon.enjoyshop.cart.application.user.manage;

import top.egon.enjoyshop.cart.application.user.pojo.command.CreateUserCommand;
import top.egon.enjoyshop.cart.application.user.pojo.query.GetUserQuery;
import top.egon.enjoyshop.cart.application.user.pojo.result.UserResult;

public interface UserManage {
    UserResult create(CreateUserCommand command);

    UserResult get(GetUserQuery query);
}
