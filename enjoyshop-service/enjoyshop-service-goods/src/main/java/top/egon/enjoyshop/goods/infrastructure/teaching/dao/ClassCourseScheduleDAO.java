package top.egon.enjoyshop.goods.infrastructure.teaching.dao;

import top.egon.enjoyshop.goods.infrastructure.teaching.po.ClassCourseSchedulePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.egon.cola.component.common.mybatis.extension.EgonColaMapper;

import java.util.List;

/** MyBatis mapper for class-course schedules. */
@Mapper
/** Shared active reads and versioned deletion are bound by this DAO XML. */
public interface ClassCourseScheduleDAO extends EgonColaMapper<ClassCourseSchedulePO> {
    List<ClassCourseSchedulePO> selectBySchoolClassIdOrderByStartsAt(
            @Param("schoolClassId") Long schoolClassId);
}
