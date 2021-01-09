package ru.klavogonki.kgparser.export.vocabulary.standard;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.klavogonki.kgparser.jsonParser.entity.PlayerVocabularyStatsEntity;
import ru.klavogonki.kgparser.jsonParser.repository.PlayerVocabularyStatsRepository;

import java.util.List;

@Component
public abstract class StandardVocabularyTopExporterDefaultImpl implements StandardVocabularyTopExporter {

    @Autowired
    protected PlayerVocabularyStatsRepository repository;

    @Override
    public List<PlayerVocabularyStatsEntity> getPlayersByBestSpeed() {
        List<PlayerVocabularyStatsEntity> players = repository.findByVocabularyCodeEqualsAndRacesCountGreaterThanEqualOrderByBestSpeedDesc(vocabularyCode(), minRacesCount());
        logger().debug("Total players by best speed, min total races = {}: {}", minRacesCount(), players.size());

        return players;
    }

    @Override
    public List<PlayerVocabularyStatsEntity> getPlayersByRacesCount() {
        List<PlayerVocabularyStatsEntity> players = repository.findByVocabularyCodeEqualsAndRacesCountGreaterThanEqualOrderByRacesCountDesc(vocabularyCode(), minRacesCount());
        logger().debug("Total players by races count, min total races = {}: {}", minRacesCount(), players.size());

        return players;
    }

    @Override
    public List<PlayerVocabularyStatsEntity> getPlayersByHaul() {
        List<PlayerVocabularyStatsEntity> players = repository.findByVocabularyCodeEqualsAndRacesCountGreaterThanEqualOrderByHaulDesc(vocabularyCode(), minRacesCount());
        logger().debug("Total players by haul, min total races = {}: {}", minRacesCount(), players.size());

        return players;
    }
}
