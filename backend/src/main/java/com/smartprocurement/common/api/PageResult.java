package com.smartprocurement.common.api;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.function.Function;

/**
 * 分页结果，字段结构与 Spring Data Page 保持一致，避免前端分页契约变化
 */
public record PageResult<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int number,
        int size
) {
    public static <S, T> PageResult<T> from(IPage<S> page, Function<S, T> mapper) {
        return new PageResult<>(
                page.getRecords().stream().map(mapper).toList(),
                page.getTotal(),
                (int) page.getPages(),
                (int) page.getCurrent() - 1,
                (int) page.getSize()
        );
    }
}
