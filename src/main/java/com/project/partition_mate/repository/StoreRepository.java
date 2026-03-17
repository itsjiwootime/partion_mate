package com.project.partition_mate.repository;

import com.project.partition_mate.domain.PartyStatus;
import com.project.partition_mate.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByName(String name);

    @Query(value = """
            select
                s.id as id,
                s.name as name,
                s.address as address,
                s.open_time as openTime,
                s.close_time as closeTime,
                s.phone as phone,
                (6371 * acos(
                    least(1.0, greatest(-1.0,
                        cos(radians(:latitude)) * cos(radians(s.latitude))
                            * cos(radians(s.longitude) - radians(:longitude))
                            + sin(radians(:latitude)) * sin(radians(s.latitude))
                    ))
                )) as distance,
                coalesce(count(p.id), 0) as partyCount
            from store s
            left join party p
                on p.store_id = s.id
               and p.party_status = :partyStatus
            where s.latitude between :minLatitude and :maxLatitude
              and s.longitude between :minLongitude and :maxLongitude
            group by s.id, s.name, s.address, s.open_time, s.close_time, s.phone, s.latitude, s.longitude
            order by distance asc
            """, nativeQuery = true)
    List<NearbyStoreProjection> findNearbyStoresWithinBounds(@Param("latitude") double latitude,
                                                             @Param("longitude") double longitude,
                                                             @Param("minLatitude") double minLatitude,
                                                             @Param("maxLatitude") double maxLatitude,
                                                             @Param("minLongitude") double minLongitude,
                                                             @Param("maxLongitude") double maxLongitude,
                                                             @Param("partyStatus") String partyStatus);

    @Query(value = """
            select
                s.id as id,
                s.name as name,
                s.address as address,
                s.open_time as openTime,
                s.close_time as closeTime,
                s.phone as phone,
                (6371 * acos(
                    least(1.0, greatest(-1.0,
                        cos(radians(:latitude)) * cos(radians(s.latitude))
                            * cos(radians(s.longitude) - radians(:longitude))
                            + sin(radians(:latitude)) * sin(radians(s.latitude))
                    ))
                )) as distance,
                coalesce(count(p.id), 0) as partyCount
            from store s
            left join party p
                on p.store_id = s.id
               and p.party_status = :partyStatus
            group by s.id, s.name, s.address, s.open_time, s.close_time, s.phone, s.latitude, s.longitude
            order by distance asc
            """, nativeQuery = true)
    List<NearbyStoreProjection> findAllOrderByDistance(@Param("latitude") double latitude,
                                                       @Param("longitude") double longitude,
                                                       @Param("partyStatus") String partyStatus);

    interface NearbyStoreProjection {
        Long getId();

        String getName();

        String getAddress();

        LocalTime getOpenTime();

        LocalTime getCloseTime();

        String getPhone();

        Double getDistance();

        Long getPartyCount();
    }
}
