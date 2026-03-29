package com.tsmc.agenticPortal.sop.dao;

import com.tsmc.agenticPortal.sop.dto.SopStepDTO;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SopGraphDAO {

    private final Driver driver;

    public SopGraphDAO(Driver driver) {
        this.driver = driver;
    }


    public SopStepDTO getStartStep(String sopCode) {
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
                return getSopStepDTO(rs);
            });
        }
    }

    public SopStepDTO getStep(String sopCode, String stepKey) {
        String cypher = """
            MATCH (t:SopTemplate {code: $code})-[:HAS_STEP]->(s:SopStep {key: $key})
            RETURN t.code AS sopCode,
                   s.key AS stepKey,
                   s.name AS name,
                   s.description AS description,
                   s.stepType AS stepType
        """;

        return getSopStepDTO(sopCode, stepKey, cypher);
    }

    public SopStepDTO getNextStep(String sopCode, String stepKey) {
        String cypher = """
            MATCH (t:SopTemplate {code: $code})-[:HAS_STEP]->(s:SopStep {key: $key})
            OPTIONAL MATCH (s)-[r:NEXT]->(n:SopStep)
            RETURN t.code AS sopCode,
                   n.key AS stepKey,
                   n.name AS name,
                   n.description AS description,
                   n.stepType AS stepType
        """;

        return getSopStepDTO(sopCode, stepKey, cypher);
    }

    private SopStepDTO getSopStepDTO(String sopCode, String stepKey, String cypher) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var rs = tx.run(cypher, Values.parameters("code", sopCode, "key", stepKey));
                if (!rs.hasNext()) throw new IllegalArgumentException("Step not found: " + sopCode + " / " + stepKey);
                return getSopStepDTO(rs);
            });
        }
    }

    private SopStepDTO getSopStepDTO(Result rs) {
        Record r = rs.next();

        SopStepDTO dto = new SopStepDTO();
        dto.sopCode = r.get("sopCode").asString();
        dto.stepKey = r.get("stepKey").asString();
        dto.name = r.get("name").asString(null);
        dto.description = r.get("description").asString(null);
        dto.stepType = r.get("stepType").asString(null);
        return dto;
    }
}