-- pgAdmin Query Tool에서 gangwon 데이터베이스를 선택한 뒤 실행하세요.
-- 동일한 제목의 테스트 데이터가 있으면 다시 삽입하지 않습니다.

INSERT INTO special_offers (
    title,
    region,
    category,
    original_price,
    sale_price,
    discount_rate,
    reason,
    image_url,
    link_url
)
SELECT *
FROM (VALUES
    (
        '강릉 오션뷰 호텔 주중 특가', '강릉', '숙박', 180000, 126000, 30,
        '최근 강릉 숙박 검색이 많은 사용자에게 추천',
        'https://images.unsplash.com/photo-1566073771259-6a8506099945',
        'https://example.com/special-offers/gangneung-ocean-hotel'
    ),
    (
        '속초 리조트 조식 포함 패키지', '속초', '숙박', 220000, 154000, 30,
        '조식이 포함된 가족 여행 패키지',
        'https://images.unsplash.com/photo-1571896349842-33c89424de2d',
        'https://example.com/special-offers/sokcho-resort-package'
    ),
    (
        '춘천 테마파크 종일 이용권', '춘천', '관광', 60000, 45000, 25,
        '가족 여행객을 위한 온라인 예매 할인',
        'https://images.unsplash.com/photo-1594736797933-d0501ba2fe65',
        'https://example.com/special-offers/chuncheon-theme-park'
    ),
    (
        '평창 목장 체험 패키지', '평창', '체험', 40000, 28000, 30,
        '입장권과 체험 프로그램이 포함된 패키지',
        'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee',
        'https://example.com/special-offers/pyeongchang-ranch'
    ),
    (
        '정선 레일바이크 2인권', '정선', '레저', 35000, 28000, 20,
        '커플 여행객에게 인기 있는 체험 상품',
        'https://images.unsplash.com/photo-1473448912268-2022ce9509d8',
        'https://example.com/special-offers/jeongseon-railbike'
    ),
    (
        '양양 서핑 입문 강습', '양양', '레저', 80000, 56000, 30,
        '장비 대여가 포함된 초보자용 강습',
        'https://images.unsplash.com/photo-1502680390469-be75c86b636f',
        'https://example.com/special-offers/yangyang-surfing'
    )
) AS sample(
    title,
    region,
    category,
    original_price,
    sale_price,
    discount_rate,
    reason,
    image_url,
    link_url
)
WHERE NOT EXISTS (
    SELECT 1
    FROM special_offers existing
    WHERE existing.title = sample.title
);
