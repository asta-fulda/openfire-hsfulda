package de.hsfulda.openfire;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import org.jivesoftware.openfire.auth.ConnectionException;
import org.jivesoftware.openfire.user.User;
import org.jivesoftware.openfire.user.UserAlreadyExistsException;
import org.jivesoftware.openfire.user.UserNotFoundException;
import org.jivesoftware.util.Log;

public class UserProvider implements org.jivesoftware.openfire.user.UserProvider {

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  public boolean isNameRequired() {
    return true;
  }

  @Override
  public boolean isEmailRequired() {
    return true;
  }

  @Override
  public User loadUser(final String username) throws UserNotFoundException {
    try {
      return Manager.getInstance().loadUser(username);

    } catch (ConnectionException e) {
      throw new UserNotFoundException(e);
    }
  }

  @Override
  public int getUserCount() {
    try {
      return Manager.getInstance().getUserCount();

    } catch (Exception e) {
      Log.error(e);

      return 0;
    }
  }

  @Override
  public Collection<User> getUsers() {
    try {
      return Manager.getInstance().getUsers(0, -1);

    } catch (Exception e) {
      Log.error(e);

      return Collections.emptyList();
    }
  }

  @Override
  public Collection<User> getUsers(final int startIndex, final int numResults) {
    try {
      return Manager.getInstance().getUsers(startIndex, numResults);

    } catch (Exception e) {
      Log.error(e);

      return Collections.emptyList();
    }
  }

  @Override
  public Collection<String> getUsernames() {
    try {
      return Manager.getInstance().getUsernames();

    } catch (Exception e) {
      Log.error(e);

      return Collections.emptyList();
    }
  }

  @Override
  public Set<String> getSearchFields() throws UnsupportedOperationException {
    return Manager.USERS_SEARCH_FIELDS.keySet();
  }

  @Override
  public Collection<User> findUsers(final Set<String> fields, final String query) throws UnsupportedOperationException {
    try {
      return Manager.getInstance().findUsers(fields, query, 0, -1);

    } catch (Exception e) {
      Log.error(e);

      return Collections.emptyList();
    }
  }

  @Override
  public Collection<User> findUsers(final Set<String> fields, final String query, final int startIndex, final int numResults) throws UnsupportedOperationException {
    try {
      return Manager.getInstance().findUsers(fields, query, startIndex, numResults);

    } catch (Exception e) {
      Log.error(e);

      return Collections.emptyList();
    }
  }

  @Override
  public User createUser(final String username, final String password, final String name, final String email) throws UserAlreadyExistsException {
    throw new UnsupportedOperationException("User creation not supported.");
  }

  @Override
  public void deleteUser(final String username) {
    throw new UnsupportedOperationException("User deletion not supported.");
  }

  @Override
  public void setName(final String username, final String name) throws UserNotFoundException {
    throw new UnsupportedOperationException("User modification not supported.");
  }

  @Override
  public void setEmail(final String username, final String email) throws UserNotFoundException {
    throw new UnsupportedOperationException("User modification not supported.");
  }

  @Override
  public void setCreationDate(final String username, final Date creationDate) throws UserNotFoundException {
    throw new UnsupportedOperationException("User modification not supported.");
  }

  @Override
  public void setModificationDate(final String username, final Date modificationDate) throws UserNotFoundException {
    throw new UnsupportedOperationException("User modification not supported.");
  }
}
