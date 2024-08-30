package online.happlay.chat.entity.query;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class PageQuery {
    // TODO 可能有个simplePage

    @ApiModelProperty("页码")
    private Integer pageNo = 1;
    @ApiModelProperty("展示数据大小")
    private Integer pageSize = 5;
    @ApiModelProperty("排序方式")
    private String orderBy;
}