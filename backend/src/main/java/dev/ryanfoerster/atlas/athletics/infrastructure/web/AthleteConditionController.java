package dev.ryanfoerster.atlas.athletics.infrastructure.web;

import dev.ryanfoerster.atlas.athletics.application.query.GetAthleteConditionUseCase;
import dev.ryanfoerster.atlas.athletics.infrastructure.web.dto.AthleteConditionDto;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de la condition (forme Banister) d'un athlète. Servi par <strong>Athletics</strong> (pas par
 * Roster) : Roster ne peut pas composer la condition sans créer un cycle Modulith (Athletics dépend déjà
 * de {@code roster.api} pour résoudre le miroir). Le frontend fait donc un appel dédié.
 *
 * <p>Sécurisé par la chaîne {@code /api/** authenticated}. <strong>Sprint 4</strong> : pas de contrôle de
 * propriété par athlète (seul le miroir a une condition réelle ; les autres renvoient un état neutre). Le
 * contrôle d'appartenance (le demandeur possède-t-il cet athlète ?) viendra au sprint 6, quand les
 * athlètes virtuels s'entraîneront aussi.
 */
@RestController
@RequestMapping("/api/athletes")
class AthleteConditionController {

    private final GetAthleteConditionUseCase getCondition;

    AthleteConditionController(GetAthleteConditionUseCase getCondition) {
        this.getCondition = getCondition;
    }

    @GetMapping("/{athleteId}/condition")
    ResponseEntity<AthleteConditionDto> condition(@PathVariable String athleteId) {
        AthleteId id;
        try {
            id = AthleteId.from(athleteId);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build(); // id malformé = inexistant
        }
        return ResponseEntity.ok(AthleteConditionDto.from(getCondition.forAthlete(id)));
    }
}
