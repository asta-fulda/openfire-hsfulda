package de.hsfulda.openfire;

import java.util.Collections;
import java.util.List;

import org.jivesoftware.util.Log;
import org.xmpp.packet.JID;

public class AdminProvider implements org.jivesoftware.openfire.admin.AdminProvider {

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  public List<JID> getAdmins() {
    try {
      return Manager.getInstance().getAdmins();

    } catch (Exception e) {
      Log.error(e);

      return Collections.emptyList();
    }
  }

  @Override
  public void setAdmins(final List<JID> admins) {
    throw new UnsupportedOperationException("Admin modification not supported.");
  }
}
