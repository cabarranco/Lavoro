package com.asbresearch.collector.event;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class TeamNameIgnoreTokenRemoverTest {
    TeamNameIgnoreTokenRemover remover = new TeamNameIgnoreTokenRemover();

    @Test
    void removeCommonToken() {
        assertThat(remover.removeIgnoreToken("Arsenal"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("Real Madrid"), is("real madrid"));

        assertThat(remover.removeIgnoreToken("Real Madrid FC"), is("real madrid"));

        assertThat(remover.removeIgnoreToken("Arsenal FC"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("FC Arsenal"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("Arsenal fc"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("fc Arsenal"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("f.c. Arsenal"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("Arsenal f.c."), is("arsenal"));

        assertThat(remover.removeIgnoreToken("Arsenal CLUB"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("CLUB Arsenal"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("Arsenal club"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("club Arsenal"), is("arsenal"));

        assertThat(remover.removeIgnoreToken("CLUB Arsenal U20"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("CLUB Arsenal United U20"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("CLUB Arsenal Utd U20"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("CLUBE Arsenal U20"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("CLUBE Arsenal United U20"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("CLUBE Arsenal Utd U20"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("Arsenal f.c. Utd U20"), is("arsenal"));

        assertThat(remover.removeIgnoreToken("Arsenal CF"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("CF Arsenal"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("Arsenal cf"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("cf Arsenal"), is("arsenal"));

        assertThat(remover.removeIgnoreToken("Arsenal United"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("United Arsenal"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("Arsenal united"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("united Arsenal"), is("arsenal"));

        assertThat(remover.removeIgnoreToken("Arsenal UTD"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("UTD Arsenal"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("Arsenal utd"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("utd Arsenal"), is("arsenal"));

        assertThat(remover.removeIgnoreToken("Arsenal U19"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("U19 Arsenal"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("Arsenal u19"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("u19 Arsenal"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("Arsenal U190"), is("arsenal u190"));
        assertThat(remover.removeIgnoreToken("U190 Arsenal"), is("u190 arsenal"));

        assertThat(remover.removeIgnoreToken("Arsenal Sporting"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("Sporting Arsenal"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("Arsenal sporting"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("sporting Arsenal"), is("arsenal"));

        assertThat(remover.removeIgnoreToken("(Res) Arsenal"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("Arsenal (Res)"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("Reserve Arsenal"), is("arsenal"));
        assertThat(remover.removeIgnoreToken("Reserves Arsenal"), is("arsenal"));
    }
}