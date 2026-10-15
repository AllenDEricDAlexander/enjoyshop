package top.egon.enjoyshop.cart.facade.teaching.dto;

import top.egon.cola.component.common.core.pojo.BasePojo;

public record SchoolClassDetailDTO(
        Long id,
        String name,
        String semester,
        String status,
        int scheduleCount) implements BasePojo {
}
