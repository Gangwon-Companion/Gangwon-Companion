package com.gangwon.companion.domain.destination.external.dto.destinationApi;

import java.math.BigDecimal;

public interface DestinationApiItem {
    Long getContentId();

    Integer getContentTypeId();

    String getTitle();

    String getAddr1();

    String getAddr2();

    BigDecimal getMapXAsBigDecimal();

    BigDecimal getMapYAsBigDecimal();

    String getFirstImage();

    String getFirstImage2();

    String getTel();

    String getSigunguCode();

    String getLclsSystem1();

    String getLclsSystem2();

    String getLclsSystem3();
}
