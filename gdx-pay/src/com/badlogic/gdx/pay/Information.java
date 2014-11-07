package com.badlogic.gdx.pay;

/** Information about a product that can be purchased provided by a purchase manager. Some methods
 * will return 'null' if requested information is not available.
 * 
 * @author noblemaster */
public class Information {

  /** The information returned if a purchase manager does not support information. */
  public static final Information UNAVAILABLE = new Information(null, null, null);
  
  private String localName;
  private String localDescription;
  private String localPricing;
  
  public Information(String localName, String localDescription, String localPricing) {
    this.localName = localName;
    this.localDescription = localDescription;
    this.localPricing = localPricing;
  }

  /** Returns the localized product name or null if not available (PurchaseManager-dependent). */
  public String getLocalName() {
    return localName;
  }

  /** Returns the localized product description or null if not available (PurchaseManager-dependent). */
  public String getLocalDescription() {
    return localDescription;
  }

  /** Returns the localized product price or null if not available (PurchaseManager-dependent). */
  public String getLocalPricing() {
    return localPricing;
  }
}
