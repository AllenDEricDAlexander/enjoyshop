package top.egon.enjoyshop.goods.facade.user.dto;

import top.egon.cola.component.common.core.pojo.BasePojo;

public record UserDetailDTO(Long id, String name, String email, String status) implements BasePojo {
}
