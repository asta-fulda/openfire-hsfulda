package de.hsfulda.openfire;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.commons.codec.digest.DigestUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.auth.ConnectionException;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.Log;
import org.jivesoftware.util.StringUtils;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.xmpp.packet.JID;

public class Manager {

  private static final String UNKNOWN_USER_OR_WRONG_PASSWORD = "Unknown user or wrong password";

  public final static Map<String, Integer> USERS_SEARCH_FIELDS = new HashMap<String, Integer>();

  private final static SimpleDateFormat LDAP_DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

  private static final String SQL_USER_SELECT = "SELECT * FROM ofUser WHERE uid = ?";
  private static final String SQL_USER_INSERT = "INSERT INTO ofUser(uid, password, name, email, creationDate, admin) VALUES (?, ?, ?, ?, ?, ?)";

  private static final String SQL_USER_SELECT_ADMIN = "SELECT * FROM ofUser WHERE admin = '1'";

  private static final String SQL_USERS_SELECT = "SELECT * FROM ofUser ORDER BY uid";
  private static final String SQL_USERS_COUNT = "SELECT COUNT(*) FROM ofUser";

  private static final String SQL_USERS_SEARCH = "SELECT * FROM ofUser WHERE uid LIKE ? ORDER BY uid";

  static {
    Log.debug("HS-Fulda user management system loaded.");

    // Add available search fields to list of search fields. The provided id
    // must match the parameter number in the SQL queries.
    Manager.USERS_SEARCH_FIELDS.put("Username", 1);

    // Set time zone of LDAP date format to UTC
    Manager.LDAP_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  private static Manager instance = null;

  public static Manager getInstance() throws ConnectionException {
    if (Manager.instance == null) {
      Manager.instance = new Manager();
      Manager.instance.init();
    }

    return Manager.instance;
  }

  private Manager() {
    // Empty. Initialization is done by "init()" called from "getInstance()".
  }

  private static final Pattern UID_PATTERN = Pattern.compile("([^@]+)(@.+)?");

  private static Cache<String, String> authCache = CacheFactory.createCache(AuthProvider.class.getName());

  private String ldapHost = null;
  private String ldapPort = null;

  private String ldapBaseDN = null;

  private String ldapProviderURL = null;

  private LdapContext ldapContext = null;

  private void init() throws ConnectionException {
    this.initSettings();

    this.initLDAP();
  }

  private void initSettings() {
    // Convert XML based provider settings to database based
    JiveGlobals.migrateProperty("hsfulda.ldap.host");
    JiveGlobals.migrateProperty("hsfulda.ldap.port");
    JiveGlobals.migrateProperty("hsfulda.ldap.basedn");

    // Load settings
    this.ldapHost = JiveGlobals.getProperty("hsfulda.ldap.host");
    this.ldapPort = JiveGlobals.getProperty("hsfulda.ldap.port");
    this.ldapBaseDN = JiveGlobals.getProperty("hsfulda.ldap.basedn");
  }

  private void initLDAP() throws ConnectionException {
    // Encode base DN
    String encodedBaseDN;
    try {
      encodedBaseDN = URLEncoder.encode(this.ldapBaseDN, "UTF-8").replaceAll("\\+", "%20");

    } catch (UnsupportedEncodingException e) {
      // "UTF-8" not supported - falling back to raw base DN
      encodedBaseDN = this.ldapBaseDN;
    }

    // Build provider URL and store it
    StringBuilder providerURL = new StringBuilder();
    providerURL.append("ldap://");
    providerURL.append(this.ldapHost);
    providerURL.append(':');
    providerURL.append(this.ldapPort);
    providerURL.append('/');
    providerURL.append(encodedBaseDN);

    this.ldapProviderURL = providerURL.toString();

    // Build LDAP environment
    Hashtable<String, Object> environment = new Hashtable<String, Object>();
    environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    environment.put(Context.PROVIDER_URL, this.ldapProviderURL);

    // environment.put("java.naming.ldap.factory.socket",
    // "org.jivesoftware.util.SimpleSSLSocketFactory");
    // environment.put(Context.SECURITY_PROTOCOL, "ssl");

    environment.put(Context.SECURITY_AUTHENTICATION, "none");

    environment.put(Context.REFERRAL, "follow");

    // Create LDAP context
    try {
      this.ldapContext = new InitialLdapContext(environment, null);

    } catch (NamingException e) {
      Log.error("HS-FULDA: Unable to connect to LDAP using '" + this.ldapHost + ":" + this.ldapPort + "' with '" + this.ldapBaseDN + "'", e);
      throw new ConnectionException("Connection to LDAP failed", e);
    }
  }

  /**
   * Converts the given user name into local UID by checking and stripping the
   * attached domain name.
   * 
   * If a given user name does not contain a domain name the plain UID will be
   * returned.
   * 
   * The returned UID is encoded using <tt>JID.unescapeNode()</tt>.
   * 
   * @param username
   *          the user name to check and convert
   * 
   * @return the UID
   * 
   * @throws UnauthorizedException
   */
  private static String usernameToUID(final String username) throws UnauthorizedException {
    // Parse UID
    Matcher uidMatcher = Manager.UID_PATTERN.matcher(username);

    // Check parameters
    if (username == null || username.length() == 0 || !uidMatcher.matches())
      throw new UnauthorizedException("Empty or malformed user name '" + username + "'");

    // Get UID from user name and un-escape it
    String uid = JID.unescapeNode(uidMatcher.group(1));

    // Check server name in UID
    String domainname = uidMatcher.group(2);
    if (domainname != null && !XMPPServer.getInstance().getServerInfo().getXMPPDomain().equals(domainname))
      throw new UnauthorizedException("Unable to authenicate forign domain '" + domainname + "'");

    // Return the extracted UID
    return uid;
  }

  /**
   * Authenticate a given user with a given password.
   * 
   * To authenticate successfully the user name and password field must not be
   * empty and if the user name contains a domain name the server name must
   * match the local server name.
   * 
   * The authentication process fetches the user definition from the database
   * and checks if a password is set for the user.
   * 
   * If the password was set and the given password matches the password in the
   * database the user becomes authenticated.
   * 
   * If the password in the database is not set the LDAP server will be used to
   * authenticate the user.
   * 
   * If the database does not contain a record for this user the LDAP server
   * will be used to authenticate the user an a new user with no password (to
   * reused LDAP on further logins) set will be created in the database.
   * 
   * If the user name is provided including server name the server name is
   * checked to match the local server name and will be truncated.
   * 
   * @param username
   *          the user name of the user to authenticated
   * 
   * @param password
   *          the password entered by the user
   * 
   * @throws UnauthorizedException
   *           if authentication fails
   * 
   * @throws ConnectionException
   *           if connection to authentication server fail
   */
  public void authenticate(final String username, final String password) throws UnauthorizedException, ConnectionException {

    Connection connection = null;
    PreparedStatement statement = null;
    ResultSet rs = null;

    try {
      // Check if UID is in cache
      if (Manager.authCache.containsKey(username)) {

        // Get hashed password from cache
        String cachePassword = Manager.authCache.get(username);

        // Compare provided password with password in cache
        if (cachePassword.equals(StringUtils.hash(password)))
          return;
      }

      // Get UID from user name
      String uid = Manager.usernameToUID(username);

      // Fetch database record for user
      connection = DbConnectionManager.getConnection();

      statement = connection.prepareStatement(Manager.SQL_USER_SELECT);
      statement.setString(1, uid);

      rs = statement.executeQuery();

      // Check if user record was found in database. If record was found, try
      // to authenticate against the database or LDAP depending on password
      // field in record. Try to authenticate against LDAP if record was not
      // found and create a new user record.
      if (rs.next()) {
        Log.debug("HS-FULDA: User '" + uid + "' found in database.");

        // Try to retrieve password from database. If password was set use it
        // to authenticate. If password was NULL try to authenticate against
        // LDAP.
        String databasePassword = rs.getString("password");
        if (!rs.wasNull()) {
          Log.debug("HS-FULDA: User '" + uid + "' uses password in database.");

          // Hash user provided password
          String passwordHash = DigestUtils.shaHex(password);

          // Password was found - compare SHA-1 hash of user provided password
          // with hash stored in database to authenticate.
          if (!passwordHash.equals(databasePassword)) {
            Log.debug("HS-FULDA: User '" + uid + "' faild to authenticate using password (db='" + databasePassword + "' & passwd='" + passwordHash + "').");

            throw new UnauthorizedException(Manager.UNKNOWN_USER_OR_WRONG_PASSWORD);
          }

          // User is successfully authenticated
          Log.debug("HS-FULDA: User '" + uid + "' authenticate using password in database.");

          // Store hashed password in cache and return
          Manager.authCache.put(username, StringUtils.hash(password));
          return;

        } else {
          Log.debug("HS-FULDA: User '" + uid + "' uses LDAP to authenticate.");

          // Try to authenticate existing user against LDAP server
          this.ldapAuthenticate(uid, password);

          // User is successfully authenticated
          Log.debug("HS-FULDA: User '" + uid + "' authenticate using LDAP.");

          // Store hashed password in cache and return
          Manager.authCache.put(username, StringUtils.hash(password));
          return;
        }

      } else {
        Log.debug("HS-FULDA: User '" + uid + "' not found in database.");

        // Try to authenticate new user against LDAP server
        this.ldapAuthenticate(uid, password);

        // New user is successfully authenticated - populate database with new
        // user
        this.createUser(uid);

        // New user is successfully authenticated
        Log.debug("HS-FULDA: New user '" + uid + "' authenticate using LDAP.");

        // Store hashed password in cache and return
        Manager.authCache.put(username, StringUtils.hash(password));
        return;
      }

    } catch (SQLException e) {
      Log.error(e);
      throw new UnauthorizedException(e);

    } finally {
      try {
        DbConnectionManager.closeConnection(rs, statement, connection);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Searches the user identified by the given user name on the LDAP server.
   * 
   * @param username
   *          the user name of the user to search
   * 
   * @return the result of the search or <tt>null</tt> if no such user was found
   * 
   * @throws NamingException
   */
  private SearchResult ldapSearchUser(final String username) throws NamingException {
    // Build controller for LDAP subtree search
    SearchControls searchControls = new SearchControls();
    searchControls.setCountLimit(1);
    searchControls.setDerefLinkFlag(true);
    searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

    // Search the LDAP for the given user
    NamingEnumeration<SearchResult> answer = this.ldapContext.search("", "(uid={0})", new String[] { username }, searchControls);

    // Check if user was found in LDAP
    if (answer == null || !answer.hasMore())
      return null;

    // Return the search result
    return answer.next();
  }

  /**
   * Authenticate the user identified the given user name using the given
   * password against the LDAP server.
   * 
   * @param username
   *          the user name of the user to authenticate
   * 
   * @param password
   *          the password of the user to authenticate
   * 
   * @throws UserNotFoundException
   */
  private void ldapAuthenticate(final String username, final String password) throws UnauthorizedException {
    try {
      // Search for user using user name
      SearchResult searchResult = this.ldapSearchUser(username);

      // Check if user was found on LDAP server
      if (searchResult == null)
        throw new UnauthorizedException(Manager.UNKNOWN_USER_OR_WRONG_PASSWORD);

      // Get user DN
      String userDN = searchResult.getName();

      // Build LDAP environment
      Hashtable<String, Object> environment = new Hashtable<String, Object>();
      environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
      environment.put(Context.PROVIDER_URL, this.ldapProviderURL);

      // environment.put("java.naming.ldap.factory.socket",
      // "org.jivesoftware.util.SimpleSSLSocketFactory");
      // environment.put(Context.SECURITY_PROTOCOL, "ssl");

      environment.put(Context.SECURITY_AUTHENTICATION, "simple");
      environment.put(Context.SECURITY_PRINCIPAL, userDN + "," + this.ldapBaseDN);
      environment.put(Context.SECURITY_CREDENTIALS, password);

      environment.put(Context.REFERRAL, "follow");

      // Authenticate as user with given password
      try {
        new InitialDirContext(environment).close();

      } catch (NamingException e) {
        throw new UnauthorizedException(Manager.UNKNOWN_USER_OR_WRONG_PASSWORD);
      }

    } catch (NamingException e) {
      Log.error(e);
      throw new UnauthorizedException(e);
    }
  }

  /**
   * Creates a new user with the given user name.
   * 
   * A new user definition with no password (to use LDAP for further request)
   * will be inserted into the database.
   * 
   * The faculty of the user will be identified using LDAP and used to assign
   * default groups for the user.
   * 
   * Additional, some bots and special users will be added to the users roster.
   * 
   * @param username
   *          the user name of the user to create
   */
  private void createUser(final String username) throws UnauthorizedException {

    Connection connection = null;
    PreparedStatement statement = null;

    try {
      connection = DbConnectionManager.getConnection();

      statement = connection.prepareStatement(Manager.SQL_USER_INSERT);

      // Set UID
      statement.setString(1, username);

      // Set fields referencing LDAP server to NULL
      statement.setNull(2, Types.VARCHAR);
      statement.setNull(3, Types.VARCHAR);
      statement.setNull(4, Types.VARCHAR);

      // Set creation date of user
      statement.setLong(5, 0l);

      // Disallow administration access
      statement.setBoolean(6, false);

      // Insert user into database
      statement.executeUpdate();

    } catch (SQLException e) {
      Log.error(e);
      throw new UnauthorizedException(e);

    } finally {
      DbConnectionManager.closeConnection(statement, connection);
    }
  }

  /**
   * Returns a list of administrators.
   * 
   * The returned list will contain the JIDs of all users having the "admin"
   * column in the users table set to true.
   * 
   * @return the list of administrators
   * 
   * @throws UserNotFoundException
   */
  public List<JID> getAdmins() throws UserNotFoundException {

    Connection connection = null;
    PreparedStatement statement = null;
    ResultSet rs = null;

    try {
      // Fetch users with "admin" column set to true from database
      connection = DbConnectionManager.getConnection();

      statement = connection.prepareStatement(Manager.SQL_USER_SELECT_ADMIN);

      rs = statement.executeQuery();

      // Create list of JID using records from query
      List<JID> admins = new ArrayList<JID>();
      while (rs.next()) {
        // Building JID using UID from record and domain name
        JID jid = new JID(rs.getString("uid"), XMPPServer.getInstance().getServerInfo().getXMPPDomain(), null);

        // Add JID to list of administrators
        admins.add(jid);
      }

      // Close result set
      rs.close();

      // Return list of administrators
      return admins;

    } catch (SQLException e) {
      Log.error(e);
      throw new UserNotFoundException(e);

    } finally {
      DbConnectionManager.closeConnection(rs, statement, connection);
    }
  }

  /**
   * Parses the given LDAP attribute to a date object.
   * 
   * @param dateAttribute
   *          the date attribute to parse
   * 
   * @return the parsed date object
   * 
   * @throws NamingException
   */
  private static Date ldapParseDate(final Attribute dateAttribute) throws NamingException {
    // Check for valid attribute
    if (dateAttribute == null || dateAttribute.size() == 0)
      return new Date();

    // Extract date from attribute
    String date = (String) dateAttribute.get();

    // Check if attribute was set
    if (date == null || date.trim().length() == 0)
      return new Date();

    // Try to pares the date using the LDAP date format
    try {
      return Manager.LDAP_DATE_FORMAT.parse(date);

    } catch (ParseException e) {
      return new Date();
    }
  }

  /**
   * Loads user information from LDAP server.
   * 
   * The user identified by the given user name is search on the LDAP server and
   * all available user information will be fetched.
   * 
   * @param uid
   *          the UID of the user to load
   * 
   * @return the user object
   * 
   * @throws NamingException
   * @throws UnauthorizedException
   */
  private User ldapLoadUser(final String uid) throws NamingException {

    // Fetch user information from LDAP server.
    SearchResult searchResult = this.ldapSearchUser(uid);

    // Check if user information is available via LDAP and use it
    if (searchResult != null) {

      // Get attributes of user
      Attributes attributes = this.ldapContext.getAttributes(searchResult.getName(), new String[] { "givenName", "sn", "mail", "createTimestamp", "modifyTimestamp" });

      // Fetch and build name from LDAP server
      Object givenName = attributes.get("givenName").get();
      Object surName = attributes.get("sn").get();

      StringBuilder name = new StringBuilder();
      if (givenName != null)
        name.append(givenName).append(' ');

      if (surName != null)
        name.append(surName);

      // Fetch email address from LDAP server
      Object email = attributes.get("mail").get();

      // Fetch creation date and modification date from LDAP server
      Date creationDate = Manager.ldapParseDate(attributes.get("createTimestamp"));
      Date modificationDate = Manager.ldapParseDate(attributes.get("modifyTimestamp"));

      // Build and return the user object
      return new User(uid, name.toString(), email != null ? email.toString() : "", creationDate, modificationDate);

    } else
      return null;
  }

  /**
   * Loads user information.
   * 
   * The user information is tried to be fetched from LDAP server. If there is
   * no information about the user on the LDAP server, the user information will
   * be received from the database.
   * 
   * @param username
   * @return
   * @throws UserNotFoundException
   */
  public User loadUser(final String username) throws UserNotFoundException {

    Connection connection = null;
    PreparedStatement statement = null;
    ResultSet rs = null;

    try {
      // Get UID from user name
      String uid = Manager.usernameToUID(username);

      // Fetch user information from database
      connection = DbConnectionManager.getConnection();

      statement = connection.prepareStatement(Manager.SQL_USER_SELECT);
      statement.setString(1, uid);

      rs = statement.executeQuery();

      // Check if database contains user
      if (rs.next()) {
        // Fetch user information from database record
        String name = rs.getString("name");
        String email = rs.getString("email");

        // Check if fields contain <tt>NULL</tt>
        if (rs.wasNull()) {
          // Try to load user from LDAP server
          User ldapUser = this.ldapLoadUser(uid);

          // Check if user information is available from LDAP server and return
          // it
          if (ldapUser != null)
            return ldapUser;
        }

        Date creationDate = new Date(rs.getLong("creationDate"));

        // Close result set
        rs.close();

        // Build and return the user object
        return new User(uid, name, email, creationDate, creationDate);

      } else {
        // Try to load user from LDAP server
        User ldapUser = this.ldapLoadUser(uid);

        // Check if user information is available from LDAP server
        if (ldapUser == null)
          throw new UserNotFoundException();

        // Return LDAP user information
        return ldapUser;
      }

    } catch (SQLException e) {
      Log.error(e);
      throw new UserNotFoundException(e);

    } catch (NamingException e) {
      Log.error(e);
      throw new UserNotFoundException(e);

    } catch (UnauthorizedException e) {
      Log.error(e);
      throw new UserNotFoundException(e);

    } finally {
      DbConnectionManager.closeConnection(rs, statement, connection);
    }
  }

  /**
   * Returns the number of users.
   * 
   * The number of users in the database will be calculated. This will only
   * represent the number of users which have logged in at least once.
   * 
   * @return the number of users
   */
  public int getUserCount() throws UserNotFoundException {

    Connection connection = null;
    PreparedStatement statement = null;
    ResultSet rs = null;

    try {
      // Fetch count of users from database
      connection = DbConnectionManager.getConnection();

      statement = connection.prepareStatement(Manager.SQL_USERS_COUNT);

      rs = statement.executeQuery();

      // Check if result is available
      if (!rs.next())
        return 0;

      // Fetch count from result
      int count = rs.getInt(1);

      // Close result set
      rs.close();

      // Return user count
      return count;

    } catch (SQLException e) {
      Log.error(e);
      throw new UserNotFoundException(e);

    } finally {
      DbConnectionManager.closeConnection(rs, statement, connection);
    }
  }

  /**
   * Returns a list of users from a given start index with a given maximum size.
   * 
   * The list of users will be fetched from the database starting with the given
   * start index. This will only represent the users which have logged in at
   * least once.
   * 
   * @param startIndex
   *          the index to start from
   * 
   * @param numResults
   *          the maximum number of results, if <tt>-1</tt> all results will be
   *          fetched
   * 
   * @return a list of users
   * 
   * @throws UserNotFoundException
   */
  public Collection<User> getUsers(final int startIndex, final int numResults) throws UserNotFoundException {

    Connection connection = null;
    PreparedStatement statement = null;
    ResultSet rs = null;

    try {
      // Fetch all users from database
      connection = DbConnectionManager.getConnection();

      statement = connection.prepareStatement(Manager.SQL_USERS_SELECT);

      rs = statement.executeQuery();

      // Skip results to reach given start index
      if (startIndex > 0)
        rs.absolute(startIndex);

      // Build list of users from result starting by given start index with a
      // maximum size of given result number
      ArrayList<User> users = new ArrayList<User>(numResults != -1 ? numResults : 10);
      for (int i = 0; (numResults == -1 || i < numResults) && rs.next(); i++) {

        // Fetch user name from database record
        String uid = rs.getString("uid");

        // Fetch user information from database record
        String name = rs.getString("name");
        String email = rs.getString("email");

        // Check if fields contain <tt>NULL</tt>
        if (rs.wasNull()) {
          // Try to load user from LDAP server
          User ldapUser = this.ldapLoadUser(uid);

          // Check if user information is available from LDAP server and add
          // it to list of users
          if (ldapUser != null) {
            users.add(ldapUser);
            continue;
          }

        }

        Date creationDate = new Date(rs.getLong("creationDate"));

        // Build the user object and add it to result list
        users.add(new User(uid, name, email, creationDate, creationDate));
      }

      // Close result set
      rs.close();

      // Return user count
      return users;

    } catch (SQLException e) {
      Log.error(e);
      throw new UserNotFoundException(e);

    } catch (NamingException e) {
      Log.error(e);
      throw new UserNotFoundException(e);

    } finally {
      DbConnectionManager.closeConnection(rs, statement, connection);
    }
  }

  /**
   * Returns a list of user names.
   * 
   * The list of users will be fetched from the database starting with the given
   * start index. This will only represent the users which have logged in at
   * least once.
   * 
   * @return a list of user names
   * 
   * @throws UserNotFoundException
   * 
   */
  public Collection<String> getUsernames() throws UserNotFoundException {

    Connection connection = null;
    PreparedStatement statement = null;
    ResultSet rs = null;

    try {
      // Fetch all users from database
      connection = DbConnectionManager.getConnection();

      statement = connection.prepareStatement(Manager.SQL_USERS_SELECT);

      rs = statement.executeQuery();

      // Build list of user names from result
      ArrayList<String> usernames = new ArrayList<String>();
      while (rs.next())

        // Read user name from database record and add it to list
        usernames.add(rs.getString("uid"));

      // Close result set
      rs.close();

      // Return user count
      return usernames;

    } catch (SQLException e) {
      Log.error(e);
      throw new UserNotFoundException(e);

    } finally {
      DbConnectionManager.closeConnection(rs, statement, connection);
    }
  }

  /**
   * Searches for users.
   * 
   * The databases will be searched for users matching the given criteria.
   * 
   * The search will be done be setting a list of fields and a query which must
   * match at least on of the fields to make a user be found.
   * 
   * This will only represent the users which have logged in at least once.
   * 
   * @param fields
   *          the fields to search in
   * 
   * @param query
   *          the query to search for
   * 
   * @param startIndex
   *          the index to start from
   * 
   * @param numResults
   *          the maximum number of results, if <tt>-1</tt> all results will be
   *          fetched
   * 
   * @return a list of users
   * 
   * @throws UserNotFoundException
   */
  public Collection<User> findUsers(final Set<String> fields, final String query, final int startIndex, final int numResults) throws UserNotFoundException {

    Connection connection = null;
    PreparedStatement statement = null;
    ResultSet rs = null;

    try {
      // Translate given query to SQL LIKE query by replacing '*' with '%' and
      // surrounding with '%'
      String sqlQuery = query.replace('*', '%');

      if (sqlQuery.charAt(0) != '%')
        sqlQuery = '%' + sqlQuery;

      if (sqlQuery.charAt(sqlQuery.length() - 1) != '%')
        sqlQuery = sqlQuery + '%';

      // Populate statement with SQL query on fields used for search and '%' on
      // others and fetch matching users from database
      connection = DbConnectionManager.getConnection();

      statement = connection.prepareStatement(Manager.SQL_USERS_SEARCH);

      for (Entry<String, Integer> entry : Manager.USERS_SEARCH_FIELDS.entrySet())

        // Check if field is set
        if (fields.contains(entry.getKey()))
          // Set statement parameter to SQL LIKE query
          statement.setString(entry.getValue(), sqlQuery);

        else
          // Ignore the field
          statement.setString(entry.getValue(), "%");

      rs = statement.executeQuery();

      // Skip results to reach given start index
      if (startIndex > 0)
        rs.absolute(startIndex);

      // Build list of users from result starting by given start index with a
      // maximum size of given result number
      ArrayList<User> users = new ArrayList<User>(numResults != -1 ? numResults : 10);
      for (int i = 0; (numResults == -1 || i < numResults) && rs.next(); i++) {

        // Fetch user name from database record
        String username = rs.getString("uid");

        // Try to fetch user information from LDAP server
        User ldapUser = this.ldapLoadUser(username);

        // Check if user information is available from LDAP server
        if (ldapUser != null)

          // Add user to result list
          users.add(ldapUser);

        else {
          // Fetch user information from database record
          String name = rs.getString("name");
          String email = rs.getString("email");

          Date creationDate = new Date(rs.getLong("creationDate"));

          // Build the user object and add it to result list
          users.add(new User(username, name, email, creationDate, creationDate));
        }
      }

      // Close result set
      rs.close();

      // Return user count
      return users;

    } catch (SQLException e) {
      Log.error(e);
      throw new UserNotFoundException(e);

    } catch (NamingException e) {
      Log.error(e);
      throw new UserNotFoundException(e);

    } finally {
      DbConnectionManager.closeConnection(rs, statement, connection);
    }
  }

  /**
   * Loads the VCard for the given user name.
   * 
   * @param username
   *          the user name of the VCard owner
   * 
   * @return the created VCard
   * 
   * @throws UserNotFoundException
   */
  public Element loadVCard(final String username) throws UserNotFoundException {
    // TODO: Load VCard information from database and LDAP server
    return DocumentHelper.createElement("vCard");
  }
}
