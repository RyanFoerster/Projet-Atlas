package dev.ryanfoerster.atlas.personaltraining.domain.model;

import dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions.InvalidExerciseNameException;

/**
 * Nom libre d'un exercice loggé (« Back Squat », « Barbell Curl »…). Value object auto-validant :
 * 2 à 80 caractères après trim. La normalisation (trim) est faite à la construction pour que
 * l'égalité par valeur ne dépende pas d'espaces parasites.
 */
public record ExerciseName(String value) {

    public static final int MIN_LENGTH = 2;
    public static final int MAX_LENGTH = 80;

    public ExerciseName {
        if (value == null) {
            throw new InvalidExerciseNameException("Le nom d'exercice ne peut pas être null");
        }
        value = value.trim();
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new InvalidExerciseNameException(
                    "Le nom d'exercice doit faire entre " + MIN_LENGTH + " et " + MAX_LENGTH
                            + " caractères : « " + value + " » (" + value.length() + ")");
        }
    }

    public static ExerciseName of(String value) {
        return new ExerciseName(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
