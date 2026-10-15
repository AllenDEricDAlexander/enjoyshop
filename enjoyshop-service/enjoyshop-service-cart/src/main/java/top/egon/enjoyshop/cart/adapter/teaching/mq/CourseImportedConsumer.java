package top.egon.enjoyshop.cart.adapter.teaching.mq;

import lombok.extern.slf4j.Slf4j;
import top.egon.enjoyshop.cart.adapter.teaching.validators.TeachingRequestValidator;
import top.egon.enjoyshop.cart.application.teaching.pojo.command.CreateCourseCommand;
import top.egon.enjoyshop.cart.application.teaching.manage.CourseManage;
import top.egon.enjoyshop.cart.facade.teaching.dto.CreateCourseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CourseImportedConsumer {
    private final CourseManage courseManage;
    private final TeachingRequestValidator validator;

    @RabbitListener(
            queues = "${spring.application.name}.course.imported",
            errorHandler = "rabbitConsumerErrorHandler",
            autoStartup = "${app.integrations.rabbitmq.enabled:false}")
    public void consume(CreateCourseDTO message) {
        validator.validate(message);
        courseManage.create(new CreateCourseCommand(
                message.code(), message.name(), message.operatorId(), message.requestId()));
    }
}
