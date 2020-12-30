package ru.klavogonki.kgparser.jsonParser.entity;

import lombok.Data;
import org.hibernate.annotations.NaturalId;
import ru.klavogonki.kgparser.Rank;
import ru.klavogonki.kgparser.http.UrlConstructor;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Player")
public class PlayerEntity {

    @Id
    @GeneratedValue
    private Long dbId;

    private LocalDateTime importDate; // when PlayerDataDownloader has been executed

    private String getSummaryError; // we import all users, including non-existing

    private String getIndexDataError; // we import all users, including non-existing and users with failed /get-index-data

    private String getStatsOverviewError; // we import all users, including non-existing and users with failed /get-stats-overview

    // fields from PlayerSummary
    @NaturalId
    private Integer playerId; // KG user id

    private String login;

    // this is a solution from https://stackoverflow.com/a/12912377/8534088
    // todo: also think about https://stackoverflow.com/a/57863734/8534088
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "car_id")),
        @AttributeOverride(name = "color", column = @Column(name = "car_color"))
    })
    private CarEntity car;

    // don't care about isOnline

    private Integer rankLevel;

    private String title;

    private Integer blocked;

    // fields from PlayerIndexData
    // don't care for PlayerIndexData.bio for now
    // todo: add bio data if ever required

    // PlayerIndexData.stats

    // see https://www.baeldung.com/jpa-java-time
    private LocalDateTime registered;

    private Integer achievementsCount;

    private Integer totalRacesCount;

    private Integer bestSpeed;

    private Integer ratingLevel;

    private Integer friendsCount;

    private Integer vocabulariesCount;

    private Integer carsCount;

    @Transient
    public String getProfileLink() {
        return UrlConstructor.userProfileLink(playerId);
    }

    @Transient
    public String getRank() {
        return Rank.getRank(rankLevel).name();
    }

    @Transient
    public String getRankDisplayName() {
        return Rank.getDisplayName(rankLevel);
    }
}
