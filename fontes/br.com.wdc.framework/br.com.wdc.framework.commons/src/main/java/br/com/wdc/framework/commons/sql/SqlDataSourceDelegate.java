package br.com.wdc.framework.commons.sql;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.ShardingKeyBuilder;
import java.util.logging.Logger;

import javax.sql.DataSource;

public class SqlDataSourceDelegate implements SqlDataSource {

    private DataSource impl;

    public SqlDataSourceDelegate() {
        this.impl = DataSourceUnavailable.INSTANCE;
    }
    
    public SqlDataSourceDelegate(DataSource impl) {
    	this.impl = impl != null ? impl : DataSourceUnavailable.INSTANCE;
    }

    public void setImpl(DataSource impl) {
        this.impl = impl != null ? impl : DataSourceUnavailable.INSTANCE;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return this.impl.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return this.impl.isWrapperFor(iface);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return this.impl.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return this.impl.getConnection(username, password);
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return this.impl.getParentLogger();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return this.impl.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        this.impl.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        this.impl.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return this.impl.getLoginTimeout();
    }

    @Override
    public ConnectionBuilder createConnectionBuilder() throws SQLException {
        return this.impl.createConnectionBuilder();
    }

    @Override
    public ShardingKeyBuilder createShardingKeyBuilder() throws SQLException {
        return this.impl.createShardingKeyBuilder();
    }

    private static class DataSourceUnavailable implements DataSource {

        private static DataSourceUnavailable INSTANCE = new DataSourceUnavailable();

        private DataSourceUnavailable() {
            // NOOP
        }

        private RuntimeException newNotImplementedException() {
            return new RuntimeException("Service unavailable");
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw newNotImplementedException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw newNotImplementedException();
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            throw newNotImplementedException();
        }

        @Override
        public Connection getConnection() throws SQLException {
            throw newNotImplementedException();
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            throw newNotImplementedException();
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            throw newNotImplementedException();
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
            throw newNotImplementedException();
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            throw newNotImplementedException();
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            throw newNotImplementedException();
        }

    }

}
