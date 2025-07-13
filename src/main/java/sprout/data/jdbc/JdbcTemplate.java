package sprout.data.jdbc;

import sprout.beans.annotation.Component;
import sprout.data.RowMapper;
import sprout.data.TransactionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JdbcTemplate {

    private final TransactionManager transactionManager;

    public JdbcTemplate(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public int update(String sql, Object... params) {
        Connection connection = null;
        PreparedStatement pstmt = null;
        try {
            connection = transactionManager.getConnection();
            pstmt = connection.prepareStatement(sql);
            setParameters(pstmt, params);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute update", e);
        } finally {
            closeStatement(pstmt);
            transactionManager.releaseConnection(connection);
        }
    }

    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... params) {
        Connection connection = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            connection = transactionManager.getConnection();
            pstmt = connection.prepareStatement(sql);
            setParameters(pstmt, params);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rowMapper.mapRow(rs);
            } else {
                return null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute queryForObject", e);
        } finally {
            closeResultSet(rs);
            closeStatement(pstmt);
            transactionManager.releaseConnection(connection);
        }
    }

    public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... params) {
        Connection connection = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            connection = transactionManager.getConnection();
            pstmt = connection.prepareStatement(sql);
            setParameters(pstmt, params);
            rs = pstmt.executeQuery();
            List<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(rowMapper.mapRow(rs));
            }
            return results;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute query", e);
        } finally {
            closeResultSet(rs);
            closeStatement(pstmt);
            transactionManager.releaseConnection(connection);
        }
    }

    private void setParameters(PreparedStatement pstmt, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            pstmt.setObject(i + 1, params[i]);
        }
    }

    private void closeStatement(PreparedStatement pstmt) {
        if (pstmt != null) {
            try {
                pstmt.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }

    private void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }
}
