package top.egon.enjoyshop.cart.infrastructure.teaching.client.impl;

import top.egon.enjoyshop.cart.domain.teaching.vos.CourseCode;
import top.egon.enjoyshop.cart.domain.teaching.vos.ExternalCourse;
import top.egon.enjoyshop.cart.infrastructure.teaching.client.TeachingQueryClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class LocalTeachingQueryClientImpl implements TeachingQueryClient {
    @Override
    public Optional<ExternalCourse> findExternalCourse(CourseCode code) {
        if (!"COURSE-001".equals(code.value())) {
            return Optional.empty();
        }
        return Optional.of(new ExternalCourse(code, "Local Course"));
    }
}
