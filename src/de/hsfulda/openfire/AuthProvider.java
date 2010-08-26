package de.hsfulda.openfire;

import org.jivesoftware.openfire.auth.ConnectionException;
import org.jivesoftware.openfire.auth.InternalUnauthenticatedException;
import org.jivesoftware.openfire.auth.UnauthorizedException;
import org.jivesoftware.openfire.user.UserNotFoundException;

public class AuthProvider implements org.jivesoftware.openfire.auth.AuthProvider {

  @Override
  public boolean isPlainSupported() {
    return true;
  }

  @Override
  public boolean isDigestSupported() {
    return false;
  }

  @Override
  public boolean supportsPasswordRetrieval() {
    return false;
  }

  @Override
  public void authenticate(final String username, final String password) throws UnauthorizedException, ConnectionException, InternalUnauthenticatedException {
    Manager.getInstance().authenticate(username, password);
  }

  @Override
  public void authenticate(final String username, final String token, final String digest) throws UnauthorizedException, ConnectionException, InternalUnauthenticatedException {
    throw new UnsupportedOperationException("Digest authentication not supported.");
  }

  @Override
  public String getPassword(final String username) throws UserNotFoundException, UnsupportedOperationException {
    throw new UnsupportedOperationException("Password retrival not supported.");
  }

  @Override
  public void setPassword(final String username, final String password) throws UserNotFoundException, UnsupportedOperationException {
    throw new UnsupportedOperationException("Password change not supported.");
  }
}
