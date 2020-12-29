package ru.klavogonki.kgparser.jsonParser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Required data from <a href="https://klavogonki.ru/api/profile/get-summary?id=242585">get-summary</a> API request.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerSummary {
    public static final String INVALID_USER_ID_ERROR = ApiErrors.INVALID_USER_ID_ERROR;

    public static final String HIDDEN_PROFILE_USER_ERROR = ApiErrors.HIDDEN_PROFILE_USER_ERROR;

    public static final String MONGO_REFS_ERROR_USER_498727 = ApiErrors.MONGO_REFS_ERROR_USER_498727;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class User {
        public int id;
        public String login;
        // todo: other fields if required
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Car {
        public int car;
        public String color;

        // todo: tuning object. Understand, how is it filled...
    }

    /**
     * Non-empty error will indicate error.
     * Typically, it is {@link #INVALID_USER_ID_ERROR "invalid user id"}.
     */
    public String err;

    public User user;
    public Car car;

    @JsonProperty("is_online")
    public Boolean isOnline;

    /**
     * Числовой код ранга.
     * @see ru.klavogonki.kgparser.Rank#getLevel
     */
    public Integer level;

    /**
     * Название ранга по-русски.
     * Может быть перегружено в {@link ru.klavogonki.kgparser.Rank#KLAVO_MECHANIC_TITLE "Клавомеханик"}.
     * @see ru.klavogonki.kgparser.Rank#getDisplayName
     */
    public String title;

    public Integer blocked; // boolean?
}
