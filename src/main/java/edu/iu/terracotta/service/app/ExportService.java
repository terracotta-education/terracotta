package edu.iu.terracotta.service.app;

import java.util.List;
import java.util.Map;

public interface ExportService {
    Map<String, List<String[]>> getCsvFiles(Long experimentId);
}

