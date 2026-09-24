package top.egon.enjoyshop.goods.adapter.pojo.convertor;

import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import top.egon.cola.component.common.core.converter.BaseConverter;
import org.mapstruct.NullValueMappingStrategy;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/** Maps native Protobuf fields while preserving the existing facade DTO contract. */
@Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface LightFacadeConverter extends BaseConverter<
        top.egon.enjoyshop.goods.facade.teaching.dto.CreateCourseDTO,
        top.egon.enjoyshop.goods.facade.proto.CreateCourseRpcRequest> {

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.enjoyshop.goods.facade.proto.CreateCourseRpcRequest toTarget(top.egon.enjoyshop.goods.facade.teaching.dto.CreateCourseDTO source);

    @Override
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.enjoyshop.goods.facade.teaching.dto.CreateCourseDTO toSource(top.egon.enjoyshop.goods.facade.proto.CreateCourseRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "semester", source = "semester")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.enjoyshop.goods.facade.proto.CreateSchoolClassRpcRequest toTarget(top.egon.enjoyshop.goods.facade.teaching.dto.CreateSchoolClassDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "semester", source = "semester")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.enjoyshop.goods.facade.teaching.dto.CreateSchoolClassDTO toSource(top.egon.enjoyshop.goods.facade.proto.CreateSchoolClassRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "schoolClassId", source = "schoolClassId")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.enjoyshop.goods.facade.proto.ScheduleCourseRpcRequest toTarget(top.egon.enjoyshop.goods.facade.teaching.dto.ScheduleCourseDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "schoolClassId", source = "schoolClassId")
    @Mapping(target = "courseId", source = "courseId")
    @Mapping(target = "startsAt", source = "startsAt")
    @Mapping(target = "endsAt", source = "endsAt")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.enjoyshop.goods.facade.teaching.dto.ScheduleCourseDTO toSource(top.egon.enjoyshop.goods.facade.proto.ScheduleCourseRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "externalId", source = "externalId")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.enjoyshop.goods.facade.proto.CreateUserRpcRequest toTarget(top.egon.enjoyshop.goods.facade.user.dto.CreateUserDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "externalId", source = "externalId")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.enjoyshop.goods.facade.user.dto.CreateUserDTO toSource(top.egon.enjoyshop.goods.facade.proto.CreateUserRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "roleCode", source = "roleCode")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.enjoyshop.goods.facade.proto.AssignRoleRpcRequest toTarget(top.egon.enjoyshop.goods.facade.user.dto.AssignRoleDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "roleCode", source = "roleCode")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.enjoyshop.goods.facade.user.dto.AssignRoleDTO toSource(top.egon.enjoyshop.goods.facade.proto.AssignRoleRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "roleCode", source = "roleCode")
    @Mapping(target = "permissionCode", source = "permissionCode")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.enjoyshop.goods.facade.proto.GrantPermissionRpcRequest toTarget(top.egon.enjoyshop.goods.facade.user.dto.GrantPermissionDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "roleCode", source = "roleCode")
    @Mapping(target = "permissionCode", source = "permissionCode")
    @Mapping(target = "operatorId", source = "operatorId")
    @Mapping(target = "requestId", source = "requestId")
    top.egon.enjoyshop.goods.facade.user.dto.GrantPermissionDTO toSource(top.egon.enjoyshop.goods.facade.proto.GrantPermissionRpcRequest source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "status", source = "status")
    top.egon.enjoyshop.goods.facade.proto.CourseResponse toTarget(top.egon.enjoyshop.goods.facade.teaching.dto.CourseDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "status", source = "status")
    top.egon.enjoyshop.goods.facade.teaching.dto.CourseDTO toSource(top.egon.enjoyshop.goods.facade.proto.CourseResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "semester", source = "semester")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "scheduleCount", source = "scheduleCount")
    top.egon.enjoyshop.goods.facade.proto.SchoolClassResponse toTarget(top.egon.enjoyshop.goods.facade.teaching.dto.SchoolClassDetailDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "semester", source = "semester")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "scheduleCount", source = "scheduleCount")
    top.egon.enjoyshop.goods.facade.teaching.dto.SchoolClassDetailDTO toSource(top.egon.enjoyshop.goods.facade.proto.SchoolClassResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "status", source = "status")
    top.egon.enjoyshop.goods.facade.proto.UserResponse toTarget(top.egon.enjoyshop.goods.facade.user.dto.UserDetailDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "status", source = "status")
    top.egon.enjoyshop.goods.facade.user.dto.UserDetailDTO toSource(top.egon.enjoyshop.goods.facade.proto.UserResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "roleCode", source = "roleCode")
    @Mapping(target = "permissionCode", source = "permissionCode")
    @Mapping(target = "status", source = "status")
    top.egon.enjoyshop.goods.facade.proto.PermissionResponse toTarget(top.egon.enjoyshop.goods.facade.user.dto.PermissionDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "roleCode", source = "roleCode")
    @Mapping(target = "permissionCode", source = "permissionCode")
    @Mapping(target = "status", source = "status")
    top.egon.enjoyshop.goods.facade.user.dto.PermissionDTO toSource(top.egon.enjoyshop.goods.facade.proto.PermissionResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    top.egon.enjoyshop.goods.facade.proto.PermissionDetailResponse toTarget(top.egon.enjoyshop.goods.facade.user.dto.PermissionDetailDTO source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "code", source = "code")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "children", source = "childrenList")
    top.egon.enjoyshop.goods.facade.user.dto.PermissionDetailDTO toSource(top.egon.enjoyshop.goods.facade.proto.PermissionDetailResponse source);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    top.egon.enjoyshop.goods.facade.proto.CourseRpcResponse courseSuccess(top.egon.enjoyshop.goods.facade.teaching.dto.CourseDTO source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    top.egon.enjoyshop.goods.facade.proto.CourseRpcResponse courseFailure(String code, String message, String traceId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    top.egon.enjoyshop.goods.facade.proto.SchoolClassRpcResponse schoolClassSuccess(top.egon.enjoyshop.goods.facade.teaching.dto.SchoolClassDetailDTO source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    top.egon.enjoyshop.goods.facade.proto.SchoolClassRpcResponse schoolClassFailure(String code, String message, String traceId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    top.egon.enjoyshop.goods.facade.proto.UserRpcResponse userSuccess(top.egon.enjoyshop.goods.facade.user.dto.UserDetailDTO source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    top.egon.enjoyshop.goods.facade.proto.UserRpcResponse userFailure(String code, String message, String traceId);

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "success", constant = "true")
    @Mapping(target = "code", constant = "SUCCESS")
    @Mapping(target = "message", constant = "success")
    @Mapping(target = "data", source = "source")
    top.egon.enjoyshop.goods.facade.proto.PermissionRpcResponse permissionSuccess(top.egon.enjoyshop.goods.facade.user.dto.PermissionDTO source);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", constant = "false")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    top.egon.enjoyshop.goods.facade.proto.PermissionRpcResponse permissionFailure(String code, String message, String traceId);

    @BeanMapping(ignoreByDefault = true, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    @Mapping(target = "success", source = "success")
    @Mapping(target = "code", source = "code")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "traceId", source = "traceId")
    top.egon.enjoyshop.goods.facade.proto.PermissionListRpcResponse permissionListResponse(
            List<top.egon.enjoyshop.goods.facade.user.dto.PermissionDetailDTO> data, boolean success,
            String code, String message, String traceId);

    @AfterMapping
    default void appendPermissionList(
            List<top.egon.enjoyshop.goods.facade.user.dto.PermissionDetailDTO> data, boolean success,
            @MappingTarget top.egon.enjoyshop.goods.facade.proto.PermissionListRpcResponse.Builder target) {
        if (success) {
            Objects.requireNonNull(data, "permission result must not be null");
        }
        if (data != null) {
            target.addAllData(data.stream().map(this::toTarget).toList());
        }
    }

    /** Protobuf exposes immutable list getters; append through its builder API. */
    @AfterMapping
    default void appendPermissionDetailResponseCollections(
            top.egon.enjoyshop.goods.facade.user.dto.PermissionDetailDTO source,
            @MappingTarget top.egon.enjoyshop.goods.facade.proto.PermissionDetailResponse.Builder target) {
        target.addAllChildren(source.children().stream().map(this::toTarget).toList());
    }

    default String toProtoTime(LocalDateTime value) {
        return value == null ? null : DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value);
    }

    default LocalDateTime fromProtoTime(String value) {
        return value == null ? null : LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
