package de.hsfulda.openfire;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jivesoftware.util.AlreadyExistsException;
import org.jivesoftware.util.Log;
import org.jivesoftware.util.NotFoundException;

public class VCardProvider implements org.jivesoftware.openfire.vcard.VCardProvider {

  @Override
  public Element loadVCard(final String username) {
    try {
      return Manager.getInstance().loadVCard(username);

    } catch (Exception e) {
      Log.error(e);
      return DocumentHelper.createElement("vCard");
    }
  }

  @Override
  public Element createVCard(final String username, final Element vCardElement) throws AlreadyExistsException {
    throw new UnsupportedOperationException("VCard creation not supported.");
  }

  @Override
  public Element updateVCard(final String username, final Element vCardElement) throws NotFoundException {
    throw new UnsupportedOperationException("VCard modification not supported.");
  }

  @Override
  public void deleteVCard(final String username) {
    throw new UnsupportedOperationException("VCard deletion not supported.");
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  // =================================//

  // private DefaultVCardProvider defaultProvider = new DefaultVCardProvider();
  //
  // @Override
  // public Element loadVCard(final String username) {
  // return this.defaultProvider.loadVCard(username);
  // }
  //
  // @Override
  // public Element createVCard(final String username, final Element
  // vCardElement)
  // throws AlreadyExistsException {
  // return this.defaultProvider.createVCard(username, vCardElement);
  // }
  //
  // @Override
  // public Element updateVCard(final String username, final Element
  // vCardElement)
  // throws NotFoundException {
  // return this.defaultProvider.updateVCard(username, vCardElement);
  // }
  //
  // @Override
  // public void deleteVCard(final String username) {
  // this.defaultProvider.deleteVCard(username);
  // }
  //
  // @Override
  // public boolean isReadOnly() {
  // return this.defaultProvider.isReadOnly();
  // }
}
