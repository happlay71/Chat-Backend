package online.happlay.chat.entity.page;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Min;

@Data
public class PageQuery {
    // TODO 可能有个simplePage

    @ApiModelProperty("页码")
    private Integer pageNo;
    @ApiModelProperty("展示数据大小")
    private Integer pageSize;
    @ApiModelProperty("排序方式")
    private String orderBy;
}