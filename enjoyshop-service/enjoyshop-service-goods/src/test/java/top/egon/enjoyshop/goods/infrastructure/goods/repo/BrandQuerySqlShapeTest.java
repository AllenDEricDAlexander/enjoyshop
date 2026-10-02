package top.egon.enjoyshop.goods.infrastructure.goods.repo;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Static SQL-shape gate for the custom brand queries (Spec section 11.2.1, TEST-008).
 *
 * <p>The mapper XML is asserted as text so the access pattern stays reviewable without a database;
 * the LIKE escaping helper is asserted as a pure function. Nothing here boots Spring.</p>
 */
class BrandQuerySqlShapeTest {

    private static final Path MAPPER_XML =
            Path.of("src/main/resources/mybatis/mapper/goods/BrandDAO.xml");

    @Test
    void listBrandPage_orders_by_seq_desc_id_desc() throws IOException {
        String listPage = selectStatement("listBrandPage");
        assertThat(listPage).contains("ORDER BY seq DESC, id DESC");
        assertThat(listPage).contains("LIMIT #{limit} OFFSET #{offset}");
        assertThat(listPage).contains("resultMap=\"BrandPOResultMap\"");
        assertThat(listPage).contains("<include refid=\"columns\"/>");
    }

    @Test
    void listBrandPage_filters_are_parameterized_and_active_only() throws IOException {
        for (String statement : List.of(selectStatement("listBrandPage"), selectStatement("countBrandPage"))) {
            assertThat(statement).contains("deleted_at IS NULL");
            assertThat(statement).contains("name LIKE #{name} ESCAPE '\\'");
            assertThat(statement).contains("letter = #{letter}");
            assertThat(statement).contains("seq &gt;= #{seqMin}");
            assertThat(statement).contains("seq &lt;= #{seqMax}");
            assertThat(statement).contains("<if test=\"categoryName != null and categoryName != ''\">");
            assertThat(statement).contains("AND EXISTS (", "FROM category c");
            assertThat(statement).contains("JOIN category_brand cb ON cb.category_id = c.id AND cb.tenant_id = c.tenant_id");
            assertThat(statement).contains("WHERE c.name = #{categoryName}");
            assertThat(statement).contains("AND cb.brand_id = brand.id");
            assertThat(statement).contains("AND c.tenant_id = brand.tenant_id");
            assertThat(statement).contains("AND c.deleted_at IS NULL");
            assertThat(statement).contains("AND cb.deleted_at IS NULL");
            assertThat(statement).doesNotContain("SELECT *");
        }
        String countPage = selectStatement("countBrandPage");
        assertThat(countPage).doesNotContain("ORDER BY");
        assertThat(countPage).doesNotContain("LIMIT");
    }

    @Test
    void count_active_by_name_excludes_self() throws IOException {
        String byName = selectStatement("countActiveByName");
        assertThat(byName).contains("deleted_at IS NULL");
        assertThat(byName).contains("name = #{name}");
        assertThat(byName).contains("AND id != #{excludeId}");
        assertThat(byName).doesNotContain("ORDER BY");
        assertThat(byName).doesNotContain("LIMIT");
    }

    @Test
    void like_prefix_escapes_wildcards() throws Exception {
        Method likePrefix = likePrefixHelper();
        assertThat(Modifier.isStatic(likePrefix.getModifiers()))
                .as("likePrefix must be a static pure escaping helper on the Repository facade")
                .isTrue();
        assertThat(invokeLikePrefix(likePrefix, "50% off")).isEqualTo("50\\% off%");
        assertThat(invokeLikePrefix(likePrefix, "a_b\\c")).isEqualTo("a\\_b\\\\c%");
        assertThat(invokeLikePrefix(likePrefix, "   ")).isNull();
        assertThat(invokeLikePrefix(likePrefix, null)).isNull();
    }

    /**
     * Resolved reflectively on purpose: the RED gate must fail on the missing escaping behaviour,
     * not on a compilation error, so the whole test class stays runnable before the helper exists.
     */
    private static Method likePrefixHelper() {
        try {
            return BrandRepository.class.getDeclaredMethod("likePrefix", String.class);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(
                    "BrandRepository.likePrefix(String) must own the LIKE wildcard escaping", exception);
        }
    }

    private static String invokeLikePrefix(Method likePrefix, String raw) throws Exception {
        likePrefix.setAccessible(true);
        return (String) likePrefix.invoke(null, raw);
    }

    private static String selectStatement(String id) throws IOException {
        String xml = Files.readString(MAPPER_XML, StandardCharsets.UTF_8);
        String declaration = "id=\"" + id + "\"";
        int declarationAt = xml.indexOf(declaration);
        assertThat(declarationAt).as("BrandDAO.xml declares a <select> statement with id=\"%s\"", id).isNotNegative();
        int start = xml.lastIndexOf("<select", declarationAt);
        int bodyStart = xml.indexOf('>', declarationAt) + 1;
        int end = xml.indexOf("</select>", bodyStart);
        assertThat(end).as("statement id=\"%s\" has a body", id).isGreaterThan(bodyStart);
        return xml.substring(start, end);
    }
}
