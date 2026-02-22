package com.tsmc.agenticPortal.sop.dao;

import com.tsmc.agenticPortal.sop.dto.SopStepDto;
import com.tsmc.agenticPortal.sop.dto.SopTemplateSummary;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SopGraphDao {

    private final Driver driver;

    public SopGraphDao(Driver driver) {
        this.driver = driver;
    }

    public List<SopTemplateSummary> searchTemplates(String keyword, int limit) {
        String q = keyword == null ? "" : keyword.trim();
        if (q.isEmpty()) return List.of();

        String cypher = """
        MATCH (t:SopTemplate)
        WHERE toLower(t.name) CONTAINS toLower($q)
           OR toLower(t.description) CONTAINS toLower($q)
           OR any(tag IN t.tags WHERE toLower(tag) CONTAINS toLower($q))
        RETURN t.code AS code,
               t.name AS name,
               t.description AS description,
               t.tags AS tags
        LIMIT $limit
    """;

        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var rs = tx.run(cypher, Values.parameters("q", q, "limit", limit));
                List<SopTemplateSummary> out = new ArrayList<>();

                while (rs.hasNext()) {
                    Record r = rs.next();
                    SopTemplateSummary s = new SopTemplateSummary();
                    s.code = r.get("code").asString(null);
                    s.name = r.get("name").asString(null);
                    s.description = r.get("description").asString(null);
                    s.tags = r.get("tags").isNull()
                            ? List.of()
                            : r.get("tags").asList(Value::asString);
                    out.add(s);
                }
                return out;
            });
        }
    }

    public SopStepDto getStartStep(String sopCode) {
        String cypher = """
            MATCH (t:SopTemplate {code: $code})-[:START_STEP]->(s:SopStep)
            RETURN t.code AS sopCode,
                   s.key AS stepKey,
                   s.name AS name,
                   s.description AS description,
                   s.stepType AS stepType
        """;

        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var rs = tx.run(cypher, Values.parameters("code", sopCode));
                if (!rs.hasNext()) throw new IllegalArgumentException("SOP not found or no START_STEP: " + sopCode);
                return getSopStepDto(rs);
            });
        }
    }

    public SopStepDto getStep(String sopCode, String stepKey) {
        String cypher = """
            MATCH (t:SopTemplate {code: $code})-[:HAS_STEP]->(s:SopStep {key: $key})
            RETURN t.code AS sopCode,
                   s.key AS stepKey,
                   s.name AS name,
                   s.description AS description,
                   s.stepType AS stepType
        """;

        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var rs = tx.run(cypher, Values.parameters("code", sopCode, "key", stepKey));
                if (!rs.hasNext()) throw new IllegalArgumentException("Step not found: " + sopCode + " / " + stepKey);
                return getSopStepDto(rs);
            });
        }
    }

    public List<SopStepDto.NextOption> getNextOptions(String sopCode, String stepKey) {
        String cypher = """
            MATCH (t:SopTemplate {code: $code})-[:HAS_STEP]->(s:SopStep {key: $key})
            OPTIONAL MATCH (s)-[r:NEXT]->(n:SopStep)
            RETURN n.key AS targetStepKey,
                   n.name AS targetName,
                   r.conditionType AS conditionType,
                   r.conditionText AS conditionText
        """;

        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var rs = tx.run(cypher, Values.parameters("code", sopCode, "key", stepKey));
                List<SopStepDto.NextOption> out = new ArrayList<>();

                while (rs.hasNext()) {
                    Record r = rs.next();
                    if (r.get("targetStepKey").isNull()) continue;
                    SopStepDto.NextOption opt = new SopStepDto.NextOption();
                    opt.nextStepKey = r.get("targetStepKey").asString("");
                    opt.conditionType = r.get("conditionType").asString("ALWAYS");
                    opt.conditionText = r.get("conditionText").asString("");
                    out.add(opt);
                }
                return out;
            });
        }
    }

    private SopStepDto getSopStepDto(Result rs) {
        Record r = rs.next();

        SopStepDto dto = new SopStepDto();
        dto.sopCode = r.get("sopCode").asString();
        dto.stepKey = r.get("stepKey").asString();
        dto.name = r.get("name").asString(null);
        dto.description = r.get("description").asString(null);
        dto.stepType = r.get("stepType").asString(null);
        dto.nextOptions = getNextOptions(dto.sopCode, dto.stepKey);
        return dto;
    }
}