package top.egon.enjoyshop.goods.adapter.goods;

import top.egon.enjoyshop.goods.application.goods.BrandManage;
import top.egon.enjoyshop.goods.application.goods.CreateBrandCommand;
import top.egon.enjoyshop.goods.application.goods.UpdateBrandCommand;
import top.egon.enjoyshop.goods.application.goods.DeleteBrandCommand;
import top.egon.enjoyshop.goods.application.goods.BrandDetailQuery;
import top.egon.enjoyshop.goods.application.goods.BrandPageQuery;
import top.egon.enjoyshop.goods.application.goods.BrandResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.egon.cola.component.common.core.pojo.PageSlice;

@RestController("brandController")
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
@Slf4j
public class BrandController {

    @Qualifier("brandManage")
    private final BrandManage manage;

    @PostMapping
    public BrandResult create(@Valid @RequestBody CreateBrandCommand command) {
        log.info("http create {}", "brand");
        return manage.create(command);
    }

    @GetMapping("/{id}")
    public BrandResult detail(@PathVariable String id) {
        return manage.detail(new BrandDetailQuery().setId(parseId(id)));
    }

    @GetMapping
    public PageSlice<BrandResult> page(@Valid BrandPageQuery query) {
        return manage.page(query);
    }

    @PutMapping("/{id}")
    public BrandResult update(@PathVariable String id, @Valid @RequestBody UpdateBrandCommand command) {
        command.setId(parseId(id));
        return manage.update(command);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id, @RequestParam("expectedVersion") long expectedVersion) {
        manage.delete(new DeleteBrandCommand().setId(parseId(id)).setExpectedVersion(expectedVersion));
    }

    private static long parseId(String id) {
        if (id == null || !id.matches("[0-9]+")) {
            throw new IllegalArgumentException("id must be a positive decimal string");
        }
        return Long.parseLong(id);
    }
}
