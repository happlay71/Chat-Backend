package online.happlay.chat.entity.po;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * app发布
 * </p>
 *
 * @author happlay
 * @since 2024-09-08
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("app_update")
@ApiModel(value = "AppUpdate对象", description = "app发布")
public class AppUpdate implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "自增ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @ApiModelProperty(value = "版本号")
    private String version;

    @ApiModelProperty(value = "更新描述")
    private String updateDesc;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "0:未发布 1:灰度发布 2:全网发布")
    private Integer status;

    @ApiModelProperty(value = "灰度uid")
    private String grayscaleUid;

    @ApiModelProperty(value = "文件类型: 0:本地文件 1:外链")
    private Integer fileType;

    @ApiModelProperty(value = "外链地址")
    private String outerLink;

    @ApiModelProperty(value = "更新描述数组")
    @TableField(exist = false)
    private String[] updateDescArray;

    public String[] getUpdateDescArray() {
        if (!StrUtil.isEmpty(updateDesc)) {
            return updateDesc.split("\\|");
        }
        return updateDescArray;
    }
}
