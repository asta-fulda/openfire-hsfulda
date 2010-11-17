package de.hsfulda.openfire;

import org.dom4j.Element;
import org.jivesoftware.openfire.vcard.DefaultVCardProvider;
import org.jivesoftware.util.AlreadyExistsException;
import org.jivesoftware.util.NotFoundException;

public class VCardProvider implements org.jivesoftware.openfire.vcard.VCardProvider {
  
  private DefaultVCardProvider defaultProvider = new DefaultVCardProvider();
  
  @Override
  public boolean isReadOnly() {
    return false;
  }
  
  @Override
  public Element loadVCard(final String username) {
    return this.defaultProvider.loadVCard(username);
    
    // try {
    // Element e1 = Manager.getInstance().loadVCard(username);
    // Element e2 = this.defaultProvider.loadVCard(username);
    //
    // Log.warn("E1 = " + e1.asXML());
    // Log.warn("E2 = " + e2.asXML());
    //
    // return e1;
    //
    // } catch (Exception e) {
    // Log.error(e);
    // return DocumentHelper.createElement("vCard");
    // }
  }
  
  @Override
  public Element createVCard(final String username, final Element vCardElement) throws AlreadyExistsException {
    return this.defaultProvider.createVCard(username, vCardElement);
    // Creation and deletion not supported
    // throw new AlreadyExistsException();
  }
  
  @Override
  public Element updateVCard(final String username, final Element vCardElement) throws NotFoundException {
    return this.defaultProvider.updateVCard(username, vCardElement);
    // throw new NotFoundException("updateVCard(" + username + ").");
  }
  
  @Override
  public void deleteVCard(final String username) {
    this.defaultProvider.deleteVCard(username);
    // Creation and deletion not supported
  }
}
