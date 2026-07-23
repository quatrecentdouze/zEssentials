package fr.maxlego08.essentials.storage;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.SqliteConnection;
import fr.maxlego08.sarah.exceptions.DatabaseException;
import fr.maxlego08.sarah.logger.Logger;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class SerializedSqliteConnection extends SqliteConnection {

    private final ReentrantLock connectionLock = new ReentrantLock(true);

    public SerializedSqliteConnection(DatabaseConfiguration databaseConfiguration, File folder, Logger logger) {
        super(databaseConfiguration, folder, logger);
    }

    @Override
    public Connection connectToDatabase() throws Exception {
        Connection connection = super.connectToDatabase();
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout = 10000");
        }
        return connection;
    }

    @Override
    public Connection getConnection() {
        this.connectionLock.lock();
        try {
            Connection connection = connectToDatabase();
            AtomicBoolean closed = new AtomicBoolean();
            return (Connection) Proxy.newProxyInstance(SerializedSqliteConnection.class.getClassLoader(), new Class[]{Connection.class}, (proxy, method, arguments) -> {
                if (method.getName().equals("close") && method.getParameterCount() == 0) {
                    if (closed.compareAndSet(false, true)) {
                        try {
                            connection.close();
                        } finally {
                            this.connectionLock.unlock();
                        }
                    }
                    return null;
                }
                try {
                    return method.invoke(connection, arguments);
                } catch (InvocationTargetException exception) {
                    throw exception.getCause();
                }
            });
        } catch (Exception exception) {
            this.connectionLock.unlock();
            throw new DatabaseException("connect", exception);
        }
    }
}
