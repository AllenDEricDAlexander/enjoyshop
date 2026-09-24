package top.egon.enjoyshop.goods.facade.user;

import jakarta.validation.constraints.NotNull;
import top.egon.enjoyshop.goods.facade.proto.GetUserPermissionsRpcRequest;
import top.egon.enjoyshop.goods.facade.proto.GrantPermissionRpcRequest;
import top.egon.enjoyshop.goods.facade.proto.PermissionListRpcResponse;
import top.egon.enjoyshop.goods.facade.proto.PermissionRpcResponse;
import top.egon.enjoyshop.goods.facade.proto.PermissionServiceGrpc;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary contract for the Permission use cases. */
@EgonRpcService(grpcClass = PermissionServiceGrpc.class, group = "user", version = "1.0.0", retries = 0)
public interface PermissionFacade {

    @EgonRpcMethod(name = "GrantPermission", idempotent = false)
    PermissionRpcResponse grantPermission(@NotNull GrantPermissionRpcRequest request);

    @EgonRpcMethod(name = "GetUserPermissions", idempotent = true)
    PermissionListRpcResponse getUserPermissions(@NotNull GetUserPermissionsRpcRequest request);
}
