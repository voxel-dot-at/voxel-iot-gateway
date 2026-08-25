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

import org.eclipse.leshan.core.oscore.OscoreSetting;
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
            String sql = "SELECT endpoint, psk_identity, pre_shared_key, raw_public_key, use_x509_cert, " +
                    "oscore_sender_id, oscore_recipient_id, oscore_master_secret, oscore_aead_algorithm, " +
                    "oscore_hkdf_algorithm, oscore_master_salt FROM security_info";
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            int count = 0;
            while (rs.next()) {
                String endpoint = rs.getString("endpoint");
                String pskIdentity = rs.getString("psk_identity");
                byte[] preSharedKey = rs.getBytes("pre_shared_key");
                byte[] rawPublicKeyBytes = rs.getBytes("raw_public_key");
                boolean useX509Cert = rs.getInt("use_x509_cert") == 1;
                byte[] oscoreSenderId = rs.getBytes("oscore_sender_id");
                byte[] oscoreRecipientId = rs.getBytes("oscore_recipient_id");
                byte[] oscoreMasterSecret = rs.getBytes("oscore_master_secret");
                Integer oscoreAeadAlgorithm = rs.getInt("oscore_aead_algorithm");
                boolean wasNullAead = rs.wasNull();
                Integer oscoreHkdfAlgorithm = rs.getInt("oscore_hkdf_algorithm");
                boolean wasNullHkdf = rs.wasNull();
                byte[] oscoreMasterSalt = rs.getBytes("oscore_master_salt");

                LOG.debug("Loading security info for endpoint: {}, psk_identity: {}, use_x509_cert: {}", endpoint, pskIdentity, useX509Cert);

                SecurityInfo info;
                if (pskIdentity != null && preSharedKey != null && preSharedKey.length > 0) {
                    // PSK mode
                    info = SecurityInfo.newPreSharedKeyInfo(endpoint, pskIdentity, preSharedKey);
                    LOG.info("Loaded PSK security info for endpoint: {}", endpoint);
                } else if (useX509Cert) {
                    // X.509 mode
                    info = SecurityInfo.newX509CertInfo(endpoint);
                    LOG.info("Loaded X.509 security info for endpoint: {}", endpoint);
                } else if (rawPublicKeyBytes != null && rawPublicKeyBytes.length > 0) {
                    // RPK mode - would need to reconstruct PublicKey from bytes
                    LOG.warn("RPK mode not fully supported yet for endpoint: {}", endpoint);
                    continue;
                } else if (oscoreSenderId != null && oscoreRecipientId != null && oscoreMasterSecret != null) {
                    // OSCORE mode
                    OscoreSetting oscoreSetting = new OscoreSetting(
                            oscoreSenderId, oscoreRecipientId, oscoreMasterSecret,
                            wasNullAead ? null : oscoreAeadAlgorithm,
                            wasNullHkdf ? null : oscoreHkdfAlgorithm,
                            oscoreMasterSalt);
                    info = SecurityInfo.newOscoreInfo(endpoint, oscoreSetting);
                    LOG.info("Loaded OSCORE security info for endpoint: {}", endpoint);
                } else {
                    LOG.warn("Unknown security info type for endpoint: {}", endpoint);
                    continue;
                }

                try {
                    super.add(info);
                    count++;
                    LOG.info("Successfully loaded security info for endpoint: {}", endpoint);
                } catch (NonUniqueSecurityInfoException e) {
                    LOG.warn("Duplicate security info for endpoint {} during load", endpoint, e);
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
            String sql = "INSERT OR REPLACE INTO security_info (endpoint, psk_identity, pre_shared_key, raw_public_key, " +
                    "use_x509_cert, oscore_sender_id, oscore_recipient_id, oscore_master_secret, oscore_aead_algorithm, " +
                    "oscore_hkdf_algorithm, oscore_master_salt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement pstmt = connection.prepareStatement(sql);
            pstmt.setString(1, info.getEndpoint());

            if (info.usePSK()) {
                pstmt.setString(2, info.getPskIdentity());
                pstmt.setBytes(3, info.getPreSharedKey());
                pstmt.setBytes(4, null);
                pstmt.setInt(5, 0);
                pstmt.setBytes(6, null);
                pstmt.setBytes(7, null);
                pstmt.setBytes(8, null);
                pstmt.setNull(9, java.sql.Types.INTEGER);
                pstmt.setNull(10, java.sql.Types.INTEGER);
                pstmt.setBytes(11, null);
                LOG.info("Saved PSK security info for endpoint: {}", info.getEndpoint());
            } else if (info.useX509Cert()) {
                pstmt.setString(2, null);
                pstmt.setBytes(3, null);
                pstmt.setBytes(4, null);
                pstmt.setInt(5, 1);
                pstmt.setBytes(6, null);
                pstmt.setBytes(7, null);
                pstmt.setBytes(8, null);
                pstmt.setNull(9, java.sql.Types.INTEGER);
                pstmt.setNull(10, java.sql.Types.INTEGER);
                pstmt.setBytes(11, null);
                LOG.info("Saved X.509 security info for endpoint: {}", info.getEndpoint());
            } else if (info.useOSCORE()) {
                pstmt.setString(2, null);
                pstmt.setBytes(3, null);
                pstmt.setBytes(4, null);
                pstmt.setInt(5, 0);
                OscoreSetting oscoreSetting = info.getOscoreSetting();
                pstmt.setBytes(6, oscoreSetting.getSenderId());
                pstmt.setBytes(7, oscoreSetting.getRecipientId());
                pstmt.setBytes(8, oscoreSetting.getMasterSecret());
                pstmt.setNull(9, java.sql.Types.INTEGER);
                pstmt.setNull(10, java.sql.Types.INTEGER);
                pstmt.setBytes(11, oscoreSetting.getMasterSalt());
                LOG.info("Saved OSCORE security info for endpoint: {}", info.getEndpoint());
            } else {
                pstmt.setString(2, null);
                pstmt.setBytes(3, null);
                pstmt.setBytes(4, null);
                pstmt.setInt(5, 0);
                pstmt.setBytes(6, null);
                pstmt.setBytes(7, null);
                pstmt.setBytes(8, null);
                pstmt.setNull(9, java.sql.Types.INTEGER);
                pstmt.setNull(10, java.sql.Types.INTEGER);
                pstmt.setBytes(11, null);
                LOG.info("Saved security info for endpoint: {}", info.getEndpoint());
            }

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
