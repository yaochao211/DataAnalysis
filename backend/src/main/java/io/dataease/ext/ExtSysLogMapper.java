package io.dataease.ext;

import io.dataease.dto.log.FolderItem;
import io.dataease.ext.query.GridExample;
import io.dataease.plugins.common.base.domain.SysLogWithBLOBs;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtSysLogMapper {

    List<SysLogWithBLOBs> query(GridExample example);

    List<FolderItem> idAndName(@Param("ids") List<String> ids, @Param("type") Integer type);
}
