package top.egon.enjoyshop.goods.infrastructure.goods.po;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import top.egon.cola.component.common.mybatis.model.EgonModel;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@TableName("brand")
public class BrandPO extends EgonModel<BrandPO> {
    @TableField("name")
    private String name;
    @TableField("image")
    private String image;
    @TableField("letter")
    private String letter;
    @TableField("seq")
    private Integer seq;
}
