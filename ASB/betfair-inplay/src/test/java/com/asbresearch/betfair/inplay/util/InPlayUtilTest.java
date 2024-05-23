package com.asbresearch.betfair.inplay.util;

import com.asbresearch.betfair.inplay.model.UpdateDetail;
import com.asbresearch.betfair.inplay.util.InPlayUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InPlayUtilTest {

    @Test
    void getScoreHistory() throws Exception {
        List<UpdateDetail> updateDetails = getUpdateDetails();
        List<String> scoreHistory = InPlayUtil.getScoreHistory(updateDetails);
        assertThat(scoreHistory).isNotNull();
        assertThat(scoreHistory.size()).isEqualTo(14);
        assertThat(scoreHistory.get(0)).isEqualTo("0-0");
        assertThat(scoreHistory.get(1)).isEqualTo("1-0");
        assertThat(scoreHistory.get(2)).isEqualTo("1-1");
        assertThat(scoreHistory.get(3)).isEqualTo("1-2");
        assertThat(scoreHistory.get(4)).isEqualTo("1-3");
        assertThat(scoreHistory.get(5)).isEqualTo("1-4");
        assertThat(scoreHistory.get(6)).isEqualTo("2-4");
        assertThat(scoreHistory.get(7)).isEqualTo("2-4");
        assertThat(scoreHistory.get(8)).isEqualTo("2-4");
        assertThat(scoreHistory.get(9)).isEqualTo("2-4");
        assertThat(scoreHistory.get(10)).isEqualTo("3-4");
        assertThat(scoreHistory.get(11)).isEqualTo("3-4");
        assertThat(scoreHistory.get(12)).isEqualTo("3-5");
        assertThat(scoreHistory.get(13)).isEqualTo("3-5");
    }

    private List<UpdateDetail> getUpdateDetails() throws URISyntaxException, IOException {
        Path path = Paths.get(getClass().getClassLoader().getResource("UpdateDetail.json").toURI());
        Stream<String> lines = Files.lines(path);
        String data = lines.collect(Collectors.joining("\n"));
        lines.close();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(data, new TypeReference<>() {
        });
    }
}