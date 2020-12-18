package ru.klavogonki.kgparser.jsonParser.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import ru.klavogonki.kgparser.jsonParser.entity.PlayerEntity;

import java.util.List;

/**
 * @see <a href="https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.core-concepts">Spring JPA core concepts</a>
 * @see <a href="https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.query-creation">Spring JPA query creation</a>
 */
public interface PlayerRepository extends CrudRepository<PlayerEntity, Long> {

    List<PlayerEntity> findByPlayerId(int playerId);

//    List<PlayerEntity> findByTotalRacesCountGreaterThanEqualAndBlockedEqualsOrderByBestSpeedDesc(int totalRacesCount, int blocked);
    List<PlayerEntity> findByTotalRacesCountGreaterThanEqualAndBlockedEqualsOrderByBestSpeedDescTotalRacesCountDesc(int totalRacesCount, int blocked);

    // JPA query, non-native
    @Query(value =
        "select" +
        " min(p.playerId)" +
        " from PlayerEntity p" +
        " where (p.login is not null)"
    )
    Integer selectMinExistingPlayerId();

    // JPA query, non-native
    @Query(value =
        "select" +
        " max(p.playerId)" +
        " from PlayerEntity p" +
        " where (p.login is not null)"
    )
    Integer selectMaxExistingPlayerId();

    // non-existing users count, users with errors from /get-summary
    Integer countByGetSummaryErrorIsNotNull();

    // blocked users count, blocked > 0
    Integer countByBlockedIsGreaterThan(int blocked);

    // players with successful /get-summary, but failed /get-index-data
    // todo: implement a separate count method if required
    List<PlayerEntity> findByGetSummaryErrorIsNullAndGetIndexDataErrorIsNotNull();

    // actual users with given (namely 0) total texts count
    Integer countByGetSummaryErrorIsNullAndGetIndexDataErrorIsNullAndBlockedEqualsAndTotalRacesCountEquals(int blocked, int totalRacesCount);

    // actual users with at least N total texts count
    Integer countByGetSummaryErrorIsNullAndGetIndexDataErrorIsNullAndBlockedEqualsAndTotalRacesCountIsGreaterThanEqual(int blocked, int totalRacesCount);
}
