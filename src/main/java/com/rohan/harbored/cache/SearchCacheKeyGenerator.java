package com.rohan.harbored.cache;

import com.rohan.harbored.dto.request.ListingSearchFilterDto;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.stream.Collectors;

@Component("searchCacheKeyGenerator")
public class SearchCacheKeyGenerator implements KeyGenerator {

    @Override
    public Object generate(Object target, Method method, Object... params) {
        ListingSearchFilterDto filter = (params.length > 0 && params[0] instanceof ListingSearchFilterDto dto)
                ? dto
                : new ListingSearchFilterDto();

        Pageable pageable = (params.length > 1 && params[1] instanceof Pageable p)
                ? p
                : Pageable.unpaged();

        return String.format(
                "city:%s:min:%s:max:%s:start:%s:end:%s:kw:%s:p:%d:s:%d:sort:%s",
                normalizeString(filter.getCity()),
                normalizeNumber(filter.getMinPrice()),
                normalizeNumber(filter.getMaxPrice()),
                normalizeDate(filter.getStartDate()),
                normalizeDate(filter.getEndDate()),
                normalizeString(filter.getKeyword()),
                pageable.isPaged() ? pageable.getPageNumber() : 0,
                pageable.isPaged() ? pageable.getPageSize() : 20,
                normalizeSort(pageable)
        );
    }

    private String normalizeString(String value) {
        if (value == null || value.isBlank()) {
            return "_";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private String normalizeNumber(BigDecimal value) {
        return value == null ? "_" : value.stripTrailingZeros().toPlainString();
    }

    private String normalizeDate(LocalDate date) {
        return date == null ? "_" : date.toString();
    }

    private String normalizeSort(Pageable pageable) {
        if (pageable == null || pageable.getSort().isUnsorted()) {
            return "unsorted";
        }
        return pageable.getSort().stream()
                .map(order -> order.getProperty().toLowerCase(Locale.ROOT)
                        + "_" + order.getDirection().name().toLowerCase(Locale.ROOT))
                .collect(Collectors.joining(","));
    }
}