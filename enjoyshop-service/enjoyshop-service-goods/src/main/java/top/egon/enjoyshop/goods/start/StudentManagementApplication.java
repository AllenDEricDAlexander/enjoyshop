package top.egon.enjoyshop.goods.start;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import top.egon.cola.component.common.mybatis.autoconfigure.EgonColaMybatisPlusProperties;

@SpringBootApplication(
        scanBasePackages = "top.egon.enjoyshop.goods")
@EnableConfigurationProperties(EgonColaMybatisPlusProperties.class)
@MapperScan(basePackages = {
        "top.egon.enjoyshop.goods.infrastructure.user.dao",
        "top.egon.enjoyshop.goods.infrastructure.teaching.dao",
        "top.egon.enjoyshop.goods.infrastructure.goods.dao",
        "top.egon.enjoyshop.goods.infrastructure.ad.dao"
})
public class StudentManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentManagementApplication.class, args);
    }
}
