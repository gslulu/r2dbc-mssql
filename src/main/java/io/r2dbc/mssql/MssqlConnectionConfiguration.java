/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.r2dbc.mssql;

import io.r2dbc.mssql.client.ClientConfiguration;
import io.r2dbc.mssql.codec.DefaultCodecs;
import io.r2dbc.mssql.util.Assert;
import io.r2dbc.mssql.util.StringUtils;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.annotation.Nullable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Connection configuration information for connecting to a Microsoft SQL database.
 * Allows configuration of the connection endpoint, login credentials, database and trace details such as application name and connection Id.
 *
 * @author Mark Paluch
 */
public final class MssqlConnectionConfiguration {

    /**
     * Default SQL Server port.
     */
    public static final int DEFAULT_PORT = 1433;

    /**
     * Default connect timeout.
     */
    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30);

    @Nullable
    private final String applicationName;

    @Nullable
    private final UUID connectionId;

    private final Duration connectTimeout;

    private final String database;

    private final String host;

    private final String hostNameInCertificate;

    private final CharSequence password;

    private final Predicate<String> preferCursoredExecution;

    private final int port;

    private final boolean sendStringParametersAsUnicode;

    private final boolean ssl;

    private final String username;

    private MssqlConnectionConfiguration(@Nullable String applicationName, @Nullable UUID connectionId, Duration connectTimeout, @Nullable String database, String host, String hostNameInCertificate
        , CharSequence password, Predicate<String> preferCursoredExecution, int port, boolean sendStringParametersAsUnicode, boolean ssl, String username) {

        this.applicationName = applicationName;
        this.connectionId = connectionId;
        this.connectTimeout = Assert.requireNonNull(connectTimeout, "connect timeout must not be null");
        this.database = database;
        this.host = Assert.requireNonNull(host, "host must not be null");
        this.hostNameInCertificate = Assert.requireNonNull(hostNameInCertificate, "hostNameInCertificate must not be null");
        this.password = Assert.requireNonNull(password, "password must not be null");
        this.preferCursoredExecution = Assert.requireNonNull(preferCursoredExecution, "preferCursoredExecution must not be null");
        this.port = port;
        this.sendStringParametersAsUnicode = sendStringParametersAsUnicode;
        this.ssl = ssl;
        this.username = Assert.requireNonNull(username, "username must not be null");
    }

    /**
     * Returns a new {@link Builder}.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    ClientConfiguration toClientConfiguration() {
        return new DefaultClientConfiguration(this.connectTimeout, this.host, this.hostNameInCertificate, this.port, this.ssl);
    }

    ConnectionOptions toConnectionOptions() {
        return new ConnectionOptions(this.preferCursoredExecution, new DefaultCodecs(), new IndefinitePreparedStatementCache(), this.sendStringParametersAsUnicode);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append(" [applicationName=\"").append(this.applicationName).append('\"');
        sb.append(", connectionId=").append(this.connectionId);
        sb.append(", connectTimeout=\"").append(this.connectTimeout).append('\"');
        sb.append(", database=\"").append(this.database).append('\"');
        sb.append(", host=\"").append(this.host).append('\"');
        sb.append(", hostNameInCertificate=\"").append(this.hostNameInCertificate).append('\"');
        sb.append(", password=\"").append(repeat(this.password.length(), "*")).append('\"');
        sb.append(", preferCursoredExecution=\"").append(this.preferCursoredExecution).append('\"');
        sb.append(", port=").append(this.port);
        sb.append(", sendStringParametersAsUnicode=").append(this.sendStringParametersAsUnicode);
        sb.append(", ssl=").append(this.ssl);
        sb.append(", username=\"").append(this.username).append('\"');
        sb.append(']');
        return sb.toString();
    }

    @Nullable
    String getApplicationName() {
        return this.applicationName;
    }

    @Nullable
    UUID getConnectionId() {
        return this.connectionId;
    }

    Duration getConnectTimeout() {
        return this.connectTimeout;
    }

    Optional<String> getDatabase() {
        return Optional.ofNullable(this.database);
    }

    String getHost() {
        return this.host;
    }

    String getHostNameInCertificate() {
        return this.hostNameInCertificate;
    }

    CharSequence getPassword() {
        return this.password;
    }

    Predicate<String> getPreferCursoredExecution() {
        return this.preferCursoredExecution;
    }

    int getPort() {
        return this.port;
    }

    boolean isSendStringParametersAsUnicode() {
        return this.sendStringParametersAsUnicode;
    }

    boolean useSsl() {
        return this.ssl;
    }

    String getUsername() {
        return this.username;
    }

    LoginConfiguration getLoginConfiguration() {
        return new LoginConfiguration(getApplicationName(), this.connectionId, getDatabase().orElse(""), lookupHostName(), getPassword(), getHost(), useSsl(), getUsername()
        );
    }

    private static String repeat(int length, String character) {

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < length; i++) {
            builder.append(character);
        }

        return builder.toString();
    }

    /**
     * Looks up local hostname of client machine.
     *
     * @return hostname string or IP of host if hostname cannot be resolved. If neither hostname or IP found returns an empty string.
     */
    private static String lookupHostName() {

        try {
            InetAddress localAddress = InetAddress.getLocalHost();
            if (localAddress != null) {
                String value = localAddress.getHostName();
                if (StringUtils.hasText(value)) {
                    return value;
                }

                value = localAddress.getHostAddress();
                if (StringUtils.hasText(value)) {
                    return value;
                }
            }
        } catch (UnknownHostException e) {
            return "";
        }
        // If hostname not found, return standard "" string.
        return "";
    }

    /**
     * A builder for {@link MssqlConnectionConfiguration} instances.
     * <p>
     * <i>This class is not threadsafe</i>
     */
    public static final class Builder {

        @Nullable
        private String applicationName;

        private UUID connectionId = UUID.randomUUID();

        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;

        private String database;

        private String host;

        private String hostNameInCertificate;

        private Predicate<String> preferCursoredExecution = sql -> false;

        private CharSequence password;

        private int port = DEFAULT_PORT;

        private boolean sendStringParametersAsUnicode = true;

        private boolean ssl;

        private String username;

        private Builder() {
        }

        /**
         * Configure the applicationName.
         *
         * @param applicationName the applicationName
         * @return this {@link Builder}
         * @throws IllegalArgumentException if {@code applicationName} is {@code null}
         */
        public Builder applicationName(String applicationName) {
            this.applicationName = Assert.requireNonNull(applicationName, "applicationName must not be null");
            return this;
        }

        /**
         * Configure the connectionId.
         *
         * @param connectionId the application name
         * @return this {@link Builder}
         * @throws IllegalArgumentException when {@link UUID} is {@code null}.
         */
        public Builder connectionId(UUID connectionId) {
            this.connectionId = Assert.requireNonNull(connectionId, "connectionId must not be null");
            return this;
        }

        /**
         * Configure the connect timeout. Defaults to 30 seconds.
         *
         * @param connectTimeout the connect timeout
         * @return this {@link Builder}
         */
        public Builder connectTimeout(Duration connectTimeout) {

            Assert.requireNonNull(connectTimeout, "connect timeout must not be null");
            Assert.isTrue(!connectTimeout.isNegative(), "connect timeout must not be negative");

            this.connectTimeout = connectTimeout;
            return this;
        }

        /**
         * Configure the database.
         *
         * @param database the database
         * @return this {@link Builder}
         */
        public Builder database(@Nullable String database) {
            this.database = database;
            return this;
        }

        /**
         * Enable SSL usage. This flag is also known as Use Encryption in other drivers.
         *
         * @return this {@link Builder}
         */
        public Builder enableSsl() {
            this.ssl = true;
            return this;
        }

        /**
         * Configure the host.
         *
         * @param host the host
         * @return this {@link Builder}
         * @throws IllegalArgumentException if {@code host} is {@code null}
         */
        public Builder host(String host) {
            this.host = Assert.requireNonNull(host, "host must not be null");
            return this;
        }

        /**
         * Configure the expected hostname in the SSL certificate. Defaults to {@link #host(String)} if left unconfigured. Accepts wildcards such as {@code *.database.windows.net}.
         *
         * @param hostNameInCertificate the hostNameInCertificate
         * @return this {@link Builder}
         * @throws IllegalArgumentException if {@code hostNameInCertificate} is {@code null}
         */
        public Builder hostNameInCertificate(String hostNameInCertificate) {
            this.hostNameInCertificate = Assert.requireNonNull(hostNameInCertificate, "hostNameInCertificate must not be null");
            return this;
        }

        /**
         * Configure the password.
         *
         * @param password the password
         * @return this {@link Builder}
         * @throws IllegalArgumentException if {@code password} is {@code null}
         */
        public Builder password(CharSequence password) {
            this.password = Assert.requireNonNull(password, "password must not be null");
            return this;
        }

        /**
         * Configure whether to prefer cursored execution.
         *
         * @param preferCursoredExecution {@code true} prefers cursors, {@code false} prefers direct execution. Defaults to direct execution.
         * @return this {@link Builder}
         * @throws IllegalArgumentException if {@code password} is {@code null}
         */
        public Builder preferCursoredExecution(boolean preferCursoredExecution) {
            return preferCursoredExecution(sql -> preferCursoredExecution);
        }

        /**
         * Configure whether to prefer cursored execution on a statement-by-statement basis. The {@link Predicate} accepts the SQL query string and returns a boolean flag indicating preference.
         * {@code true} prefers cursors, {@code false} prefers direct execution. Defaults to direct execution.
         *
         * @param preference the {@link Predicate}.
         * @return this {@link Builder}
         * @throws IllegalArgumentException if {@code password} is {@code null}
         */
        public Builder preferCursoredExecution(Predicate<String> preference) {
            this.preferCursoredExecution = Assert.requireNonNull(preference, "Predicate must not be null");
            return this;
        }

        /**
         * Configure the port. Defaults to {@code 5432}.
         *
         * @param port the port
         * @return this {@link Builder}
         */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * Configure whether to send character data as unicode (NVARCHAR, NCHAR, NTEXT) or whether to use the database encoding. Enabled by default. If disabled, {@link CharSequence} data is sent
         * using the database-specific collation such as ASCII/MBCS instead of Unicode.
         *
         * @param sendStringParametersAsUnicode {@literal true} to send character data as unicode (NVARCHAR, NCHAR, NTEXT) or whether to use the database encoding. Enabled by default.
         * @return this {@link Builder}
         */
        public Builder sendStringParametersAsUnicode(boolean sendStringParametersAsUnicode) {
            this.sendStringParametersAsUnicode = sendStringParametersAsUnicode;
            return this;
        }

        /**
         * Configure the username.
         *
         * @param username the username
         * @return this {@link Builder}
         * @throws IllegalArgumentException if {@code username} is {@code null}
         */
        public Builder username(String username) {
            this.username = Assert.requireNonNull(username, "username must not be null");
            return this;
        }

        /**
         * Returns a configured {@link MssqlConnectionConfiguration}.
         *
         * @return a configured {@link MssqlConnectionConfiguration}.
         */
        public MssqlConnectionConfiguration build() {

            if (this.hostNameInCertificate == null) {
                this.hostNameInCertificate = this.host;
            }

            return new MssqlConnectionConfiguration(this.applicationName, this.connectionId, this.connectTimeout, this.database, this.host, this.hostNameInCertificate, this.password,
                this.preferCursoredExecution, this.port,
                this.sendStringParametersAsUnicode, this.ssl, this.username);
        }
    }

    private static class DefaultClientConfiguration implements ClientConfiguration {

        private Duration connectTimeout;

        private String host;

        private String hostNameInCertificate;

        private int port;

        private boolean ssl;

        DefaultClientConfiguration(Duration connectTimeout, String host, String hostNameInCertificate, int port, boolean ssl) {

            this.connectTimeout = connectTimeout;
            this.host = host;
            this.hostNameInCertificate = hostNameInCertificate;
            this.port = port;
            this.ssl = ssl;
        }

        @Override
        public String getHost() {
            return this.host;
        }

        @Override
        public int getPort() {
            return this.port;
        }

        @Override
        public Duration getConnectTimeout() {
            return this.connectTimeout;
        }

        @Override
        public ConnectionProvider getConnectionProvider() {
            return ConnectionProvider.newConnection();
        }

        @Override
        public boolean isSslEnabled() {
            return this.ssl;
        }

        @Override
        public String getHostNameInCertificate() {
            return this.hostNameInCertificate;
        }
    }
}
