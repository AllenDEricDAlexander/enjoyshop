package top.egon.enjoyshop.cart.facade.teaching;

import jakarta.validation.constraints.NotNull;
import top.egon.enjoyshop.cart.facade.proto.CreateSchoolClassRpcRequest;
import top.egon.enjoyshop.cart.facade.proto.GetSchoolClassRpcRequest;
import top.egon.enjoyshop.cart.facade.proto.ScheduleCourseRpcRequest;
import top.egon.enjoyshop.cart.facade.proto.SchoolClassRpcResponse;
import top.egon.enjoyshop.cart.facade.proto.SchoolClassServiceGrpc;
import top.egon.cola.component.rpc.annotation.EgonRpcMethod;
import top.egon.cola.component.rpc.annotation.EgonRpcService;

/** Native unary contract for the SchoolClass use cases. */
@EgonRpcService(grpcClass = SchoolClassServiceGrpc.class, group = "teaching", version = "1.0.0", retries = 0)
public interface SchoolClassFacade {

    @EgonRpcMethod(name = "CreateSchoolClass", idempotent = false)
    SchoolClassRpcResponse createSchoolClass(@NotNull CreateSchoolClassRpcRequest request);

    @EgonRpcMethod(name = "ScheduleCourse", idempotent = false)
    SchoolClassRpcResponse scheduleCourse(@NotNull ScheduleCourseRpcRequest request);

    @EgonRpcMethod(name = "GetSchoolClass", idempotent = true)
    SchoolClassRpcResponse getSchoolClass(@NotNull GetSchoolClassRpcRequest request);
}
