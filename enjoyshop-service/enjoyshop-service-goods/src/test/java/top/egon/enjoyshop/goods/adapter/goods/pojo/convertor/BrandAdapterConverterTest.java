package top.egon.enjoyshop.goods.adapter.goods.pojo.convertor;

import org.junit.jupiter.api.Test;
import top.egon.enjoyshop.goods.adapter.goods.pojo.dto.CreateBrandDTO;
import top.egon.enjoyshop.goods.adapter.goods.pojo.dto.UpdateBrandDTO;
import top.egon.enjoyshop.goods.adapter.goods.pojo.vo.BrandVO;
import top.egon.enjoyshop.goods.application.goods.BrandResult;
import top.egon.enjoyshop.goods.application.goods.CreateBrandCommand;
import top.egon.enjoyshop.goods.application.goods.UpdateBrandCommand;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Adapter normalisation and projection gate (Spec §9.2 request tables, §10.4 mapping ownership).
 *
 * <p>Pure mapping assertions only: constraint negatives are exercised by the controller slice, so
 * this test instantiates the MapStruct generated implementation directly, which is how every other
 * adapter converter test in the module is written.</p>
 */
class BrandAdapterConverterTest {

    private static final String IMAGE = "https://cdn.example.com/apple.png";
    private static final long BRAND_ID = 1948672938456580097L;

    private final BrandAdapterConverter converter = new BrandAdapterConverterImpl();

    @Test
    void letter_is_normalized_to_uppercase() {
        CreateBrandCommand command = converter.toTarget(request("Apple", "a", 5));

        assertThat(command.getLetter()).isEqualTo("A");
    }

    @Test
    void name_is_trimmed_and_space_normalized() {
        CreateBrandCommand command = converter.toTarget(request("  Apple   Store ", "A", 5));

        assertThat(command.getName()).isEqualTo("Apple Store");
    }

    @Test
    void seq_defaults_to_zero_when_absent() {
        CreateBrandCommand command = converter.toTarget(request("Apple", "A", null));

        assertThat(command.getSeq()).isZero();
    }

    @Test
    void result_string_id_maps_to_long_vo() {
        BrandResult result = new BrandResult().setId(Long.toString(BRAND_ID)).setVersion(3L)
                .setName("Apple").setImage(IMAGE).setLetter("A").setSeq(5);

        BrandVO vo = converter.toTarget(result);

        assertThat(vo.id()).isEqualTo(BRAND_ID);
        assertThat(vo.version()).isEqualTo(3L);
        assertThat(vo.name()).isEqualTo("Apple");
        assertThat(vo.image()).isEqualTo(IMAGE);
        assertThat(vo.letter()).isEqualTo("A");
        assertThat(vo.seq()).isEqualTo(5);
    }

    @Test
    void update_request_version_maps_to_expected_version() {
        UpdateBrandCommand command = converter.toCommand(90001L,
                new UpdateBrandDTO("  Apple   Store ", null, "b", 7, 42L));

        assertThat(command.getId()).isEqualTo(90001L);
        assertThat(command.getExpectedVersion()).isEqualTo(42L);
        assertThat(command.getName()).isEqualTo("Apple Store");
        assertThat(command.getImage()).isNull();
        assertThat(command.getLetter()).isEqualTo("B");
        assertThat(command.getSeq()).isEqualTo(7);
    }

    private static CreateBrandDTO request(String name, String letter, Integer seq) {
        return new CreateBrandDTO(name, IMAGE, letter, seq);
    }
}
