package com.gangwon.companion.domain.destination.repository;

import com.gangwon.companion.domain.destination.entity.Destination;
import com.gangwon.companion.domain.destination.entity.SourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DestinationRepository extends JpaRepository<Destination, Long> {
    List<Destination> findByThemeId(Long themeId);

    Optional<Destination> findByTitleAndAddr1(String title, String addr1);

    @Query("""
            select d
            from Destination d
            where d.theme.id = :themeId
              and exists (
                  select 1
                  from DestinationSource ds
                  where ds.destination = d
                    and ds.sourceType = :sourceType
              )
            """)
    List<Destination> findByThemeIdAndSourceType(
            @Param("themeId") Long themeId,
            @Param("sourceType") SourceType sourceType
    );

    @Query("""
            select d
            from Destination d
            where d.theme.id = :themeId
              and exists (
                  select 1
                  from DestinationSource ds
                  where ds.destination = d
                    and ds.sourceType = :firstSourceType
              )
              and exists (
                  select 1
                  from DestinationSource ds
                  where ds.destination = d
                    and ds.sourceType = :secondSourceType
              )
            """)
    List<Destination> findByThemeIdAndBothSourceTypes(
            @Param("themeId") Long themeId,
            @Param("firstSourceType") SourceType firstSourceType,
            @Param("secondSourceType") SourceType secondSourceType
    );
}
