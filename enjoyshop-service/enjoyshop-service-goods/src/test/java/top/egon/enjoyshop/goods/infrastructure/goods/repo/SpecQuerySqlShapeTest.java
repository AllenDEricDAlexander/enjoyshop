package top.egon.enjoyshop.goods.infrastructure.goods.repo;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.ibatis.annotations.Param;
import org.junit.jupiter.api.Test;
import top.egon.enjoyshop.goods.infrastructure.goods.dao.SpecDAO;

import static org.assertj.core.api.Assertions.assertThat;

class SpecQuerySqlShapeTest {

    private static final Path MAPPER_XML = Path.of("src/main/resources/mybatis/mapper/goods/SpecDAO.xml");

    @Test
    void query_binds_exact_category_name_and_joins_active_tenant_rows() throws Exception {
        String query = selectStatement("listByCategoryName");

        assertThat(query).contains("resultMap=\"SpecPOResultMap\"");
        assertThat(query).contains("WHERE c.name = #{categoryName}");
        assertThat(query).contains("c.deleted_at IS NULL");
        assertThat(query).contains("t.deleted_at IS NULL");
        assertThat(query).contains("s.deleted_at IS NULL");
        assertThat(query).contains("t.tenant_id = c.tenant_id");
        assertThat(query).contains("s.tenant_id = t.tenant_id");
        assertThat(query).contains("c.tenant_id = s.tenant_id");
        assertThat(query).contains("SELECT DISTINCT");
        assertThat(query).contains("ORDER BY s.seq, s.id");
        assertThat(query).doesNotContain("${", "SELECT *", "LIMIT", "OFFSET");
    }

    @Test
    void dao_parameter_name_matches_mapper_binding() throws Exception {
        Method method = SpecDAO.class.getDeclaredMethod("listByCategoryName", String.class);
        Param parameter = method.getParameters()[0].getAnnotation(Param.class);

        assertThat(parameter).isNotNull();
        assertThat(parameter.value()).isEqualTo("categoryName");
    }

    private static String selectStatement(String id) throws IOException {
        String xml = Files.readString(MAPPER_XML, StandardCharsets.UTF_8);
        String declaration = "id=\"" + id + "\"";
        int declarationAt = xml.indexOf(declaration);
        assertThat(declarationAt).as("SpecDAO.xml declares a <select> statement with id=\"%s\"", id)
                .isNotNegative();
        int start = xml.lastIndexOf("<select", declarationAt);
        int bodyStart = xml.indexOf('>', declarationAt) + 1;
        int end = xml.indexOf("</select>", bodyStart);
        assertThat(end).as("statement id=\"%s\" has a body", id).isGreaterThan(bodyStart);
        return xml.substring(start, end);
    }
}
