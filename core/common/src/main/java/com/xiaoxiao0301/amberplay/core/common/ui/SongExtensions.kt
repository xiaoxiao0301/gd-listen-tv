package com.xiaoxiao0301.amberplay.core.common.ui

import com.xiaoxiao0301.amberplay.domain.model.Song

/** 构造专辑封面 URL，[size] 默认 300（像素）。 */
fun Song.picUrl(size: Int = 300): String =
    "https://music-api.gdstudio.xyz/api.php?types=pic&source=$source&id=$picId&size=$size"
