package com.asbresearch.collector.matcher;

import com.asbresearch.collector.config.CollectorProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class JaroWinklerDistanceMatcherTest {
    private CollectorProperties collectorProperties;
    private JaroWinklerDistanceMatcher matcher;

    @BeforeEach
    void setUp() {
        collectorProperties = new CollectorProperties();
        collectorProperties.setJaroWinklerThreshold(0.8);
        matcher = new JaroWinklerDistanceMatcher(collectorProperties);
    }

    @Test
    void testRemoveCommonToken() {
        String teamName = "Louisville FC";
        assertThat(teamName.toLowerCase().endsWith(" fc"), is(true));
        assertThat(teamName.substring(0, teamName.length() - 3), is("Louisville"));

        teamName = "FC Louisville";
        assertThat(teamName.toLowerCase().startsWith("fc "), is(true));
        assertThat(teamName.substring(3), is("Louisville"));

    }

    @Test
    void testSpiltTeam() {
        String[] tokens = "Louisville FC v Tulsa Roughnecks FC".split("\\s+v\\s+");
        assertThat(tokens.length, is(2));
        assertThat(tokens[0], is("Louisville FC"));
        assertThat(tokens[1], is("Tulsa Roughnecks FC"));
    }

    @Test
    void isNotSameTeam() {
        assertThat(matcher.isSameTeam("Gremio FBPA U20", "Atlético Goianiense U20"), is(false));
        assertThat(matcher.isSameTeam("Corinthians U20", "Bahia U20"), is(false));
        assertThat(matcher.isSameTeam("Braga", "Sporting Braga"), is(false));
        assertThat(matcher.isSameTeam("Chertanovo Moscow", "FK Chertanovo"), is(false));
        assertThat(matcher.isSameTeam("QPR", "Queens Park Rangers"), is(false));
        assertThat(matcher.isSameTeam("QPR", "Queens Park Rangers"), is(false));
        assertThat(matcher.isSameTeam("Lask", "LASK"), is(false));
        assertThat(matcher.isSameTeam("SCR Altach", "SC Rheindorf Altach"), is(false));
    }

    @Test
    void isSameTeam() {
        assertThat(matcher.isSameTeam("Sporting Lisbon", "Sporting CP"), is(true));
        assertThat(matcher.isSameTeam("Cruzeiro MG", "Cruzeiro EC"), is(true));
        assertThat(matcher.isSameTeam("Louisville", "Louisville City"), is(true));
        assertThat(matcher.isSameTeam("Tulsa Roughnecks", "Tulsa"), is(true));
        assertThat(matcher.isSameTeam("Montreal Impact", "Montreal"), is(true));
        assertThat(matcher.isSameTeam("C-Osaka", "Cerezo Osaka"), is(true));
    }

    @Test
    void test() {
        assertThat(matcher.isSameTeam("Valladolid".toLowerCase(), "Real Valladolid".toLowerCase()), is(false));
        assertThat(matcher.isSameTeam("Zaragoza".toLowerCase(), "Real Zaragoza".toLowerCase()), is(false));
    }
}