package dev.ryanfoerster.atlas.shared.domain.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainExceptionTest {

    /** Sous-classe minimale pour exercer la base abstraite (les deux constructeurs). */
    private static final class SampleDomainException extends DomainException {
        SampleDomainException(String message) {
            super(message);
        }

        SampleDomainException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @Test
    void is_an_unchecked_exception_carrying_a_message() {
        DomainException exception = new SampleDomainException("règle métier violée");

        assertThat(exception).isInstanceOf(RuntimeException.class).hasMessage("règle métier violée");
    }

    @Test
    void can_wrap_a_cause() {
        Throwable cause = new IllegalStateException("détail technique");

        DomainException exception = new SampleDomainException("règle métier violée", cause);

        assertThat(exception).hasMessage("règle métier violée").hasCause(cause);
    }
}
