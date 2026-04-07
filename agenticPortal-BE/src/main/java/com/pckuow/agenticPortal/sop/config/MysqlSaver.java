package com.pckuow.agenticPortal.sop.config;


import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.checkpoint.AbstractMysqlServer;
import org.bsc.langgraph4j.checkpoint.Checkpoint;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;

public class MysqlSaver extends AbstractMysqlServer {

    private static final String RELEASE_THREAD = """
            DELETE FROM LANGRAPH4J_THREAD WHERE thread_name = ?
            """;

    public static class Builder extends AbstractBuilder<Builder> {

        public MysqlSaver build() {
            return new MysqlSaver(this);
        }
    }

    private MysqlSaver(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected Tag releaseCheckpoints(RunnableConfig config, LinkedList<Checkpoint> checkpoints) throws Exception {
        final String threadName = threadId(config);

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(RELEASE_THREAD)) {
            preparedStatement.setString(1, threadName);
            preparedStatement.execute();
        } catch (SQLException sqlException) {
            throw new Exception("Unable to release checkpoint", sqlException);
        }

        return new Tag(threadName, checkpoints);
    }
}
