package de.hsfulda.openfire;

import java.util.Collection;

import org.jivesoftware.openfire.group.Group;
import org.jivesoftware.openfire.group.GroupAlreadyExistsException;
import org.jivesoftware.openfire.group.GroupNotFoundException;
import org.jivesoftware.util.Log;
import org.xmpp.packet.JID;

public class GroupProvider implements org.jivesoftware.openfire.group.GroupProvider {

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  public boolean isSearchSupported() {
    return false;
  }

  @Override
  public Group getGroup(final String name) throws GroupNotFoundException {
    Log.error("Not implemented: GroupProvider.getGroup(final String name)");
    throw new UnsupportedOperationException("!!! CURRENTLY NOT IMPLEMENTED !!!");
  }

  @Override
  public int getGroupCount() {
    Log.error("Not implemented: GroupProvider.getGroupCount()");
    throw new UnsupportedOperationException("!!! CURRENTLY NOT IMPLEMENTED !!!");
  }

  @Override
  public Collection<String> getGroupNames() {
    Log.error("Not implemented: GroupProvider.getGroupNames()");
    throw new UnsupportedOperationException("!!! CURRENTLY NOT IMPLEMENTED !!!");
  }

  @Override
  public Collection<String> getSharedGroupsNames() {
    Log.error("Not implemented: GroupProvider.getSharedGroupsNames()");
    throw new UnsupportedOperationException("!!! CURRENTLY NOT IMPLEMENTED !!!");
  }

  @Override
  public Collection<String> getGroupNames(final int startIndex, final int numResults) {
    Log.error("Not implemented: GroupProvider.getGroupNames(final int startIndex, final int numResults)");
    throw new UnsupportedOperationException("!!! CURRENTLY NOT IMPLEMENTED !!!");
  }

  @Override
  public Collection<String> getGroupNames(final JID user) {
    Log.error("Not implemented: GroupProvider.getGroupNames(final JID user)");
    throw new UnsupportedOperationException("!!! CURRENTLY NOT IMPLEMENTED !!!");
  }

  @Override
  public Group createGroup(final String name) throws UnsupportedOperationException, GroupAlreadyExistsException {
    throw new UnsupportedOperationException("Group creation not supported.");
  }

  @Override
  public void deleteGroup(final String name) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Group deletion not supported.");
  }

  @Override
  public void setName(final String oldName, final String newName) throws UnsupportedOperationException, GroupAlreadyExistsException {
    throw new UnsupportedOperationException("Group modification not supported.");
  }

  @Override
  public void setDescription(final String name, final String description) throws GroupNotFoundException {
    throw new UnsupportedOperationException("Group modification not supported.");
  }

  @Override
  public void addMember(final String groupName, final JID user, final boolean administrator) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Group membership modification not supported.");
  }

  @Override
  public void updateMember(final String groupName, final JID user, final boolean administrator) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Group membership modification not supported.");
  }

  @Override
  public void deleteMember(final String groupName, final JID user) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Group membership modification not supported.");
  }

  @Override
  public Collection<String> search(final String query) {
    throw new UnsupportedOperationException("Group search not supported.");
  }

  @Override
  public Collection<String> search(final String query, final int startIndex, final int numResults) {
    throw new UnsupportedOperationException("Group search not supported.");
  }

  // =======================================//

  // private DefaultGroupProvider defaultProvider = new DefaultGroupProvider();
  //
  // @Override
  // public Group createGroup(final String name)
  // throws UnsupportedOperationException, GroupAlreadyExistsException {
  // return this.defaultProvider.createGroup(name);
  // }
  //
  // @Override
  // public void deleteGroup(final String name)
  // throws UnsupportedOperationException {
  // this.defaultProvider.deleteGroup(name);
  // }
  //
  // @Override
  // public Group getGroup(final String name) throws GroupNotFoundException {
  // return this.defaultProvider.getGroup(name);
  // }
  //
  // @Override
  // public void setName(final String oldName, final String newName)
  // throws UnsupportedOperationException, GroupAlreadyExistsException {
  // this.defaultProvider.setName(oldName, newName);
  // }
  //
  // @Override
  // public void setDescription(final String name, final String description)
  // throws GroupNotFoundException {
  // this.defaultProvider.setDescription(name, description);
  // }
  //
  // @Override
  // public int getGroupCount() {
  // return this.defaultProvider.getGroupCount();
  // }
  //
  // @Override
  // public Collection<String> getGroupNames() {
  // return this.defaultProvider.getGroupNames();
  // }
  //
  // @Override
  // public Collection<String> getSharedGroupsNames() {
  // return this.defaultProvider.getSharedGroupsNames();
  // }
  //
  // @Override
  // public Collection<String> getGroupNames(final int startIndex,
  // final int numResults) {
  // return this.defaultProvider.getGroupNames(startIndex, numResults);
  // }
  //
  // @Override
  // public Collection<String> getGroupNames(final JID user) {
  // return this.defaultProvider.getGroupNames(user);
  // }
  //
  // @Override
  // public void addMember(final String groupName, final JID user,
  // final boolean administrator) throws UnsupportedOperationException {
  // this.defaultProvider.addMember(groupName, user, administrator);
  // }
  //
  // @Override
  // public void updateMember(final String groupName, final JID user,
  // final boolean administrator) throws UnsupportedOperationException {
  // this.defaultProvider.updateMember(groupName, user, administrator);
  // }
  //
  // @Override
  // public void deleteMember(final String groupName, final JID user)
  // throws UnsupportedOperationException {
  // this.defaultProvider.deleteMember(groupName, user);
  // }
  //
  // @Override
  // public boolean isReadOnly() {
  // return this.defaultProvider.isReadOnly();
  // }
  //
  // @Override
  // public Collection<String> search(final String query) {
  // return this.defaultProvider.search(query);
  // }
  //
  // @Override
  // public Collection<String> search(final String query, final int startIndex,
  // final int numResults) {
  // return this.defaultProvider.search(query, startIndex, numResults);
  // }
  //
  // @Override
  // public boolean isSearchSupported() {
  // return this.defaultProvider.isSearchSupported();
  // }
}
