package dev.ryanfoerster.atlas.athletics.domain.service;

import dev.ryanfoerster.atlas.athletics.domain.model.PatternProgress;
import dev.ryanfoerster.atlas.athletics.domain.model.PatternStrengthRef;
import dev.ryanfoerster.atlas.athletics.domain.model.StructuralProgress;
import dev.ryanfoerster.atlas.athletics.domain.model.TrainingStimulus;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.OptionalDouble;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Dynamique de la progression structurelle (ADR-033) : cible convergente {@code plafond − (plafond −
 * départ)·exp(−C/SCALE)} + cliquet. Domain service pur, testé en isolation des trois autres échelles de
 * temps (Banister). Le scénario long (12-16 sem) vit dans le test de calibration ; ici, les invariants
 * mécaniques unitaires.
 */
class StructuralProgressionModelTest {

    private static final Instant T0 = Instant.parse("2026-01-05T17:00:00Z");
    private final StructuralProgressionModel model = new StructuralProgressionModel();

    @Test
    void merited_at_zero_chronic_load_equals_start() {
        PatternProgress fresh = PatternProgress.starting(100.0, 184.0);
        assertThat(model.meritedOneRmKg(fresh)).isCloseTo(100.0, within(1e-9));
    }

    @Test
    void merited_rises_with_chronic_load_but_stays_strictly_under_the_ceiling() {
        double low = model.meritedOneRmKg(new PatternProgress(100.0, 184.0, 10.0));
        double high = model.meritedOneRmKg(new PatternProgress(100.0, 184.0, 60.0));
        assertThat(high).isGreaterThan(low).isGreaterThan(100.0);
        assertThat(high).isLessThan(184.0); // asymptote : jamais atteinte à charge finie
    }

    @Test
    void advance_initializes_a_new_pattern_from_its_reference() {
        StructuralProgress after = model.advance(StructuralProgress.EMPTY,
                Map.of(MovementPattern.SQUAT, new TrainingStimulus(0.2)),
                Map.of(MovementPattern.SQUAT, new PatternStrengthRef(100.0, 184.0)),
                T0, T0.plus(Duration.ofHours(1)));

        PatternProgress squat = after.progress(MovementPattern.SQUAT).orElseThrow();
        assertThat(squat.startOneRmKg()).isEqualTo(100.0);   // départ = 1RM courant frais
        assertThat(squat.ceilingOneRmKg()).isEqualTo(184.0); // plafond = plafond génétique
        assertThat(squat.chronicLoad()).isCloseTo(0.2, within(1e-6));
    }

    @Test
    void advance_keeps_the_start_frozen_and_accumulates_chronic_load_across_workouts() {
        StructuralProgress p = model.advance(StructuralProgress.EMPTY,
                Map.of(MovementPattern.SQUAT, new TrainingStimulus(0.2)),
                Map.of(MovementPattern.SQUAT, new PatternStrengthRef(100.0, 184.0)),
                T0, T0.plusSeconds(1));
        // Deuxième séance : le 1RM courant a monté (130), mais le DÉPART reste figé à 100 (pas de double comptage).
        p = model.advance(p,
                Map.of(MovementPattern.SQUAT, new TrainingStimulus(0.2)),
                Map.of(MovementPattern.SQUAT, new PatternStrengthRef(130.0, 184.0)),
                T0.plusSeconds(1), T0.plusSeconds(2));

        PatternProgress squat = p.progress(MovementPattern.SQUAT).orElseThrow();
        assertThat(squat.startOneRmKg()).isEqualTo(100.0); // figé, ignore le ref courant 130
        assertThat(squat.chronicLoad()).isCloseTo(0.4, within(1e-6)); // 0.2 + 0.2, décroissance ~nulle sur 1s
    }

    @Test
    void advance_decays_chronic_load_over_a_rest_interval() {
        StructuralProgress p = model.advance(StructuralProgress.EMPTY,
                Map.of(MovementPattern.SQUAT, new TrainingStimulus(10.0)),
                Map.of(MovementPattern.SQUAT, new PatternStrengthRef(100.0, 184.0)),
                T0, T0.plusSeconds(1));
        // 90 jours de repos sans stimulus → la charge chronique décroît d'un facteur 1/e (τ = 90j).
        StructuralProgress rested = model.advance(p, Map.of(), Map.of(),
                T0.plusSeconds(1), T0.plusSeconds(1).plus(Duration.ofDays(90)));

        assertThat(rested.progress(MovementPattern.SQUAT).orElseThrow().chronicLoad())
                .isCloseTo(10.0 / Math.E, within(0.05));
    }

    @Test
    void advance_ignores_a_pattern_with_no_strength_reference() {
        // ROW / CHIN_UP n'ont pas de standard de force → pas de plafond → pas de progression structurelle.
        StructuralProgress after = model.advance(StructuralProgress.EMPTY,
                Map.of(MovementPattern.ROW, new TrainingStimulus(0.3)),
                Map.of(), // aucune référence pour ROW
                T0, T0.plusSeconds(1));

        assertThat(after.progress(MovementPattern.ROW)).isEmpty();
    }

    @Test
    void cliquet_emits_only_increases_above_the_current_one_rep_max() {
        PatternProgress merited130 = new PatternProgress(100.0, 184.0, 50.0); // mérité ~ 122
        double merited = model.meritedOneRmKg(merited130);

        // Le 1RM courant est en-dessous du mérité → on émet la progression.
        OptionalDouble up = model.progressedOneRmKg(merited130, merited - 5.0);
        assertThat(up).hasValue(merited);

        // Le 1RM courant est déjà au-dessus (cliquet : un repos a fait baisser le mérité) → rien à émettre.
        assertThat(model.progressedOneRmKg(merited130, merited + 5.0)).isEmpty();
    }

    @Test
    void an_athlete_already_at_the_ceiling_does_not_progress() {
        // plafond clampé au 1RM courant (athlète déjà au-delà de son potentiel génétique) → gap nul.
        StructuralProgress after = model.advance(StructuralProgress.EMPTY,
                Map.of(MovementPattern.SQUAT, new TrainingStimulus(5.0)),
                Map.of(MovementPattern.SQUAT, new PatternStrengthRef(200.0, 184.0)), // déjà au-dessus du plafond
                T0, T0.plusSeconds(1));

        PatternProgress squat = after.progress(MovementPattern.SQUAT).orElseThrow();
        assertThat(model.meritedOneRmKg(squat)).isCloseTo(200.0, within(1e-9)); // reste à son niveau
        assertThat(model.progressedOneRmKg(squat, 200.0)).isEmpty();
    }
}
