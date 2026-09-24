package top.egon.enjoyshop.goods.domain.teaching.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import top.egon.enjoyshop.goods.domain.teaching.vos.CourseCode;
import top.egon.enjoyshop.goods.domain.teaching.vos.ExternalCourse;

import java.util.Optional;

/** Outbound teaching query capability owned by the domain. */
public interface TeachingQueryService {
    Optional<ExternalCourse> findExternalCourse(@Valid @NotNull CourseCode code);
}
