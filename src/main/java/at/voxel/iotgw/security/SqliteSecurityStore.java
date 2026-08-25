package at.voxel.iotgw.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.eclipse.leshan.core.peer.OscoreIdentity;
import org.eclipse.leshan.servers.security.InMemorySecurityStore;
import org.eclipse.leshan.servers.security.NonUniqueSecurityInfoException;
import org.eclipse.leshan.servers.security.SecurityInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link SecurityStore} which persists {@link SecurityInfo} in a SQLite database.
 * <p>
 * This implementation stores security information in a local SQLite database file for persistence across server restarts.
 * </p>
 * <p>
 * NOTE: DTLS/PSK authentication is not working yet. The store loads all entries on startup and persists them to SQLite.
 * </p>
 */
public class SqliteSecurityStore extends InMemorySecurityStore {

    private static final Logger LOG = LoggerFactory.getLogger(SqliteSecurityStore.class);

    private static final String DEFAULT_DB_FILE = "data/security.db";

    private final String dbFile;
    private Connection connection;

    /**
     * Creates a SqliteSecurityStore with the default database file location.
     */
    public SqliteSecurityStore() {
        this(DEFAULT_DB_FILE);
    }

    /**
     * Creates a SqliteSecurityStore with the specified database file.
     *
     * @param dbFile the path to the SQLite database file
     */
    public SqliteSecurityStore(String dbFile) {
        this.dbFile = dbFile;
        initializeDatabase();
        loadFromDatabase();
        LOG.info("SqliteSecurityStore initialized and loaded successfully");
    }

    /**
     * Initializes the SQLite database and creates the table if it doesn't exist.
     */
    private void initializeDatabase() {
        try {
            // Load SQLite driver
            Class.forName("org.sqlite.JDBC");

            // Create connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
            connection.setAutoCommit(true);

            // Create table if not exists
            Statement stmt = connection.createStatement();
            stmt.execute(
                "CREATE TABLE IF NOT EXISTS security_info (" +
                "  endpoint TEXT PRIMARY KEY," +
                "  psk_identity TEXT," +
                "  pre_shared_key BLOB," +
                "  raw_public_key BLOB," +
                "  use_x509_cert INTEGER," +
                "  oscore_sender_id BLOB," +
                "  oscore_recipient_id BLOB," +
                "  oscore_master_secret BLOB," +
                "  oscore_aead_algorithm INTEGER," +
                "  oscore_hkdf_algorithm INTEGER," +
                "  oscore_master_salt BLOB," +
                "  serialized_info BLOB" +
                ")"
            );
            LOG.info("SQLite security store initialized at {}", dbFile);
        } catch (SQLException e) {
            LOG.error("Error initializing SQLite database", e);
            throw new RuntimeException("Failed to initialize SQLite security store", e);
        } catch (ClassNotFoundException e) {
            LOG.error("SQLite JDBC driver not found", e);
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }
    }

    /**
     * Loads security information from the database into memory.
     */
    private void loadFromDatabase() {
        try {
            String sql = "SELECT endpoint, serialized_info FROM security_info";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            int count = 0;
            while (rs.next()) {
                String endpoint = rs.getString("endpoint");
                byte[] serializedData = rs.getBytes("serialized_info");

                if (serializedData != null && serializedData.length > 0) {
                    SecurityInfo info = deserializeSecurityInfo(serializedData);
                    if (info != null) {
                        try {
                            super.add(info);
                            count++;
                        } catch (NonUniqueSecurityInfoException e) {
                            LOG.warn("Duplicate security info for endpoint {} during load", endpoint, e);
                        }
                    }
                }
            }
            LOG.info("Loaded {} security info records from database", count);
        } catch (SQLException e) {
            LOG.error("Error loading security info from database", e);
        }
    }

    /**
     * Adds a security info to the store and persists it to the database.
     */
    @Override
    public SecurityInfo add(SecurityInfo info) throws NonUniqueSecurityInfoException {
        writeLock.lock();
        try {
            SecurityInfo previous = super.add(info);
            saveToDatabase(info);
            return previous;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Removes a security info from the store and the database.
     */
    @Override
    public SecurityInfo remove(String endpoint, boolean infosAreCompromised) {
        writeLock.lock();
        try {
            SecurityInfo info = super.remove(endpoint, infosAreCompromised);
            if (info != null) {
                removeFromDatabase(endpoint);
            }
            return info;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Saves a security info to the database.
     */
    private void saveToDatabase(SecurityInfo info) {
        try {
            byte[] serializedData = serializeSecurityInfo(info);

            String sql = "INSERT OR REPLACE INTO security_info (endpoint, serialized_info) VALUES (?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, info.getEndpoint());
            pstmt.setBytes(2, serializedData);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.error("Error saving security info to database for endpoint {}", info.getEndpoint(), e);
        }
    }

    /**
     * Removes a security info from the database.
     */
    private void removeFromDatabase(String endpoint) {
        try {
            String sql = "DELETE FROM security_info WHERE endpoint = ?";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, endpoint);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            LOG.error("Error removing security info from database for endpoint {}", endpoint, e);
        }
    }

    /**
     * Serializes a SecurityInfo object to bytes.
     */
    private byte[] serializeSecurityInfo(SecurityInfo info) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(info);
            return baos.toByteArray();
        } catch (Exception e) {
            LOG.error("Error serializing security info", e);
            return new byte[0];
        }
    }

    /**
     * Deserializes a SecurityInfo object from bytes.
     */
    private SecurityInfo deserializeSecurityInfo(byte[] data) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (SecurityInfo) ois.readObject();
        } catch (Exception e) {
            LOG.error("Error deserializing security info", e);
            return null;
        }
    }

    /**
     * Closes the database connection.
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOG.info("SQLite security store closed");
            }
        } catch (SQLException e) {
            LOG.error("Error closing SQLite database", e);
        }
    }
}
