package dev.ryanfoerster.atlas.roster.application.command;

import dev.ryanfoerster.atlas.roster.domain.model.AthleteCandidate;
import dev.ryanfoerster.atlas.roster.domain.model.Rarity;
import dev.ryanfoerster.atlas.roster.domain.model.ScoutedCandidate;
import dev.ryanfoerster.atlas.roster.domain.model.ScoutedCandidateId;
import dev.ryanfoerster.atlas.roster.domain.port.ScoutedCandidateRepository;
import dev.ryanfoerster.atlas.roster.domain.service.AthleteGenerator;
import dev.ryanfoerster.atlas.roster.domain.service.RarityRoller;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Use case : scouter un athlète. Tire un tier ({@link RarityRoller}), génère un candidat cohérent
 * ({@link AthleteGenerator}), le persiste temporairement (TTL) et le renvoie. Le candidat n'est PAS
 * encore recruté — c'est une proposition (ADR-022). Le hasard est injecté ({@code random}), jamais caché.
 */
@Service
public class ScoutAthleteUseCase {

    /** Durée de vie d'un candidat scouté (ADR-022). */
    static final Duration TTL = Duration.ofHours(1);

    private final ScoutedCandidateRepository repository;
    private final RarityRoller rarityRoller;
    private final AthleteGenerator generator;
    private final Clock clock;
    private final java.util.Random random;

    public ScoutAthleteUseCase(ScoutedCandidateRepository repository, RarityRoller rarityRoller,
                               AthleteGenerator generator, Clock clock, java.util.Random random) {
        this.repository = repository;
        this.rarityRoller = rarityRoller;
        this.generator = generator;
        this.clock = clock;
        this.random = random;
    }

    @Transactional
    public ScoutResult scout() {
        Instant now = clock.instant();

        Rarity rarity = rarityRoller.roll(random.nextDouble());
        AthleteCandidate candidate = generator.generateCandidate(random.nextLong(), rarity);

        ScoutedCandidate scouted = ScoutedCandidate.issue(
                ScoutedCandidateId.generate(), candidate, now, now.plus(TTL));
        ScoutedCandidate saved = repository.save(scouted);

        return new ScoutResult(saved.id(), candidate);
    }
}
