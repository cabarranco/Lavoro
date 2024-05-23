package com.asbresearch.collector.copy;

import com.asbresearch.collector.config.CopyProperties;
import com.asbresearch.common.bigquery.BigQueryService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component("UploadCsvToBigQuery")
@EnableConfigurationProperties({CopyProperties.class})
@Slf4j
@ConditionalOnProperty(prefix = "copy", name = "UploadCsvToBigQuery", havingValue = "on")
public class UploadCsvToBigQuery {
    private final BigQueryService bigQueryService;
    private final String csvUploadDir;
    private final String datasetName;
    private final String tableName;

    @Autowired
    public UploadCsvToBigQuery(BigQueryService bigQueryService, CopyProperties copyProperties) {
        this.bigQueryService = bigQueryService;
        this.csvUploadDir = copyProperties.getCsvUploadDir();
        this.datasetName = copyProperties.getDatasetName();
        this.tableName = copyProperties.getTableName();
    }

    @SneakyThrows
    @PostConstruct
    public void execute() {
        Set<Path> files = getFiles();
        files.forEach(file -> loadFile(file));
    }

    @SneakyThrows
    private void loadFile(Path file) {
        log.info("Begin Loading file={} datasetName={} tableName={}", file, datasetName, tableName);
        List<String> lines = Files.lines(file).collect(Collectors.toList());
        bigQueryService.insertRows(datasetName, tableName, lines);
        log.info("End Loading file={}", file);
    }

    public Set<Path> getFiles() throws IOException {
        Set<Path> fileList = new HashSet<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(csvUploadDir))) {
            for (Path path : stream) {
                if (!Files.isDirectory(path)) {
                    fileList.add(path);
                }
            }
        }
        return fileList;
    }
}
