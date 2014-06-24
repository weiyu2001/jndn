/**
 * Copyright (C) 2014 Regents of the University of California.
 * @author: Jeff Thompson <jefft0@remap.ucla.edu>
 * @author: From code in NDN-CPP by Yingdi Yu <yingdi@cs.ucla.edu>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * A copy of the GNU General Public License is in the file COPYING.
 */

package net.named_data.jndn.security;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.KeyLocatorType;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.Sha256WithRsaSignature;
import net.named_data.jndn.Signature;
import net.named_data.jndn.encoding.Tlv0_1WireFormat;
import net.named_data.jndn.encoding.WireFormat;
import net.named_data.jndn.encoding.tlv.Tlv;
import net.named_data.jndn.encoding.tlv.TlvEncoder;
import net.named_data.jndn.security.certificate.Certificate;
import net.named_data.jndn.security.certificate.IdentityCertificate;
import net.named_data.jndn.security.encryption.EncryptionManager;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.policy.NoVerifyPolicyManager;
import net.named_data.jndn.security.policy.PolicyManager;
import net.named_data.jndn.util.Blob;
import net.named_data.jndn.util.SignedBlob;

/**
 * KeyChain is the main class of the security library.
 *
 * The KeyChain class provides a set of interfaces to the security library such 
 * as identity management, policy configuration and packet signing and 
 * verification.
 * @note This class is an experimental feature.  See the API docs for more 
 * detail at
 * http://named-data.net/doc/ndn-ccl-api/key-chain.html .
 */
public class KeyChain {
  /**
   * Create a new KeyChain with the given IdentityManager and PolicyManager.
   * @param identityManager An object of a subclass of IdentityManager.
   * @param policyManager An object of a subclass of PolicyManager.
   */
  public KeyChain
    (IdentityManager identityManager, PolicyManager policyManager)
  {
    identityManager_ = identityManager;
    policyManager_ = policyManager;
  }

  /**
   * Create a new KeyChain with the given IdentityManager and a 
   * NoVerifyPolicyManager.
   * @param identityManager An object of a subclass of IdentityManager.
   */
  public KeyChain(IdentityManager identityManager)
  {
    identityManager_ = identityManager;
    policyManager_ = new NoVerifyPolicyManager();
  }

  // TODO: KeyChain()
  
  /*****************************************
   *          Identity Management          *
   *****************************************/

  /**
   * Create an identity by creating a pair of Key-Signing-Key (KSK) for this identity and a self-signed certificate of the KSK.
   * @param identityName The name of the identity.
   * @return The key name of the auto-generated KSK of the identity.
   * @throws SecurityException if the identity has already been created.
   */
  public final Name
  createIdentity(Name identityName) throws SecurityException
  {
    return identityManager_.createIdentity(identityName);
  }

  /**
   * Get the default identity.
   * @return The name of default identity.
   * @throws SecurityException if the default identity is not set.
   */
  public final Name
  getDefaultIdentity() throws SecurityException
  {
    return identityManager_.getDefaultIdentity();
  }
    
  /**
   * Get the default certificate name of the default identity.
   * @return The requested certificate name.
   * @throws SecurityException if the default identity is not set or the default 
   * key name for the identity is not set or the default certificate name for 
   * the key name is not set.
   */
  public final Name
  getDefaultCertificateName() throws SecurityException
  {
    return identityManager_.getDefaultCertificateName();
  }
  
  
  /**
   * Generate a pair of RSA keys for the specified identity.
   * @param identityName The name of the identity.
   * @param isKsk true for generating a Key-Signing-Key (KSK), false for a Data-Signing-Key (KSK).
   * @param keySize The size of the key.
   * @return The generated key name.
   */
  public final Name
  generateRSAKeyPair
    (Name identityName, boolean isKsk, int keySize) throws SecurityException
  {
    return identityManager_.generateRSAKeyPair(identityName, isKsk, keySize);
  }
  
  /**
   * Generate a pair of RSA keys for the specified identity and default keySize
   * 2048.
   * @param identityName The name of the identity.
   * @param isKsk true for generating a Key-Signing-Key (KSK), false for a Data-Signing-Key (KSK).
   * @return The generated key name.
   */
  public final Name
  generateRSAKeyPair(Name identityName, boolean isKsk) throws SecurityException
  {
    return identityManager_.generateRSAKeyPair(identityName, isKsk);
  }
  
  /**
   * Generate a pair of RSA keys for the specified identity for a 
   * Data-Signing-Key and default keySize 2048.
   * @param identityName The name of the identity.
   * @return The generated key name.
   */
  public final Name
  generateRSAKeyPair(Name identityName) throws SecurityException
  {
    return identityManager_.generateRSAKeyPair(identityName);
  }

  /**
   * Set a key as the default key of an identity.
   * @param keyName The name of the key.
   * @param identityName the name of the identity. If empty, the 
   * identity name is inferred from the keyName.
   */
  public final void
  setDefaultKeyForIdentity(Name keyName, Name identityName)
  {
    identityManager_.setDefaultKeyForIdentity(keyName, identityName);
  }

  /**
   * Set a key as the default key of an identity, inferred from the keyName.
   * @param keyName The name of the key.
   */
  public final void
  setDefaultKeyForIdentity(Name keyName)
  {
    identityManager_.setDefaultKeyForIdentity(keyName);
  }

  /**
   * Generate a pair of RSA keys for the specified identity and set it as 
   * default key for the identity.
   * @param identityName The name of the identity.
   * @param isKsk true for generating a Key-Signing-Key (KSK), false for a Data-Signing-Key (KSK).
   * @param keySize The size of the key.
   * @return The generated key name.
   */
  public final Name
  generateRSAKeyPairAsDefault
    (Name identityName, boolean isKsk, int keySize) throws SecurityException
  {
    return identityManager_.generateRSAKeyPairAsDefault(identityName, isKsk, keySize);
  }
  
  /**
   * Generate a pair of RSA keys for the specified identity and set it as 
   * default key for the identity, using the default keySize 2048.
   * @param identityName The name of the identity.
   * @param isKsk true for generating a Key-Signing-Key (KSK), false for a Data-Signing-Key (KSK).
   * @return The generated key name.
   */
  public final Name
  generateRSAKeyPairAsDefault(Name identityName, boolean isKsk) throws SecurityException
  {
    return identityManager_.generateRSAKeyPairAsDefault(identityName, isKsk);
  }
  
  /**
   * Generate a pair of RSA keys for the specified identity and set it as 
   * default key for the identity for a Data-Signing-Key and using the default 
   * keySize 2048.
   * @param identityName The name of the identity.
   * @return The generated key name.
   */
  public final Name
  generateRSAKeyPairAsDefault(Name identityName) throws SecurityException
  {
    return identityManager_.generateRSAKeyPairAsDefault(identityName);
  }

  /**
   * Create a public key signing request.
   * @param keyName The name of the key.
   * @return The signing request data.
   * @throws SecurityException if the keyName is not found.
   */
  public final Blob
  createSigningRequest(Name keyName) throws SecurityException
  {
    return identityManager_.getPublicKey(keyName).getKeyDer();
  }

  /**
   * Install an identity certificate into the public key identity storage.
   * @param certificate The certificate to to added.
   */
  public final void
  installIdentityCertificate(IdentityCertificate certificate) throws SecurityException
  {
    identityManager_.addCertificate(certificate);
  }

  /**
   * Set the certificate as the default for its corresponding key.
   * @param certificate The certificate.
   */
  public final void
  setDefaultCertificateForKey(IdentityCertificate certificate) throws SecurityException
  {
    identityManager_.setDefaultCertificateForKey(certificate);
  }

  /**
   * Get a certificate with the specified name.
   * @param certificateName The name of the requested certificate.
   * @return The requested certificate which is valid.
   */
  public final Certificate
  getCertificate(Name certificateName)
  {
    return identityManager_.getCertificate(certificateName);
  }

  /**
   * Get a certificate even if the certificate is not valid anymore.
   * @param certificateName The name of the requested certificate.
   * @return The requested certificate.
   */
  public final Certificate
  getAnyCertificate(Name certificateName)
  {
    return identityManager_.getAnyCertificate(certificateName);
  }

  /**
   * Get an identity certificate with the specified name.
   * @param certificateName The name of the requested certificate.
   * @return The requested certificate which is valid.
   */
  public final IdentityCertificate
  getIdentityCertificate(Name certificateName)
  {
    return identityManager_.getCertificate(certificateName);
  }

  /**
   * Get an identity certificate even if the certificate is not valid anymore.
   * @param certificateName The name of the requested certificate.
   * @return The requested certificate.
   */
  public final IdentityCertificate
  getAnyIdentityCertificate(Name certificateName)
  {
    return identityManager_.getAnyCertificate(certificateName);
  }

  /**
   * Revoke a key.
   * @param keyName The name of the key that will be revoked.
   */
  public final void 
  revokeKey(Name keyName)
  {
    //TODO: Implement
  }

  /**
   * Revoke a certificate.
   * @param certificateName The name of the certificate that will be revoked.
   */
  public final void 
  revokeCertificate(Name certificateName)
  {
    //TODO: Implement
  }

  /**
   * Get the identity manager given to or created by the constructor.
   * @return The identity manager.
   */
  public final IdentityManager
  getIdentityManager() { return identityManager_; }
  
    
  /*****************************************
   *              Sign/Verify              *
   *****************************************/

  /**
   * Wire encode the Data object, sign it and set its signature.
   * @param data The Data object to be signed.  This updates its signature and 
   * key locator field and wireEncoding.
   * @param certificateName The certificate name of the key to use for signing.
   * @param wireFormat A WireFormat object used to encode the input.
   */
  public void 
  sign(Data data, Name certificateName, WireFormat wireFormat) throws SecurityException
  {
    identityManager_.signByCertificate(data, certificateName, wireFormat);
  }

  /**
   * Wire encode the Data object, sign it and set its signature.
   * Use the default WireFormat.getDefaultWireFormat()
   * @param data The Data object to be signed.  This updates its signature and 
   * key locator field and wireEncoding.
   * @param certificateName The certificate name of the key to use for signing.
   */
  public void 
  sign(Data data, Name certificateName) throws SecurityException
  {
    sign(data, certificateName, WireFormat.getDefaultWireFormat());
  }

  public void 
  sign(Interest interest, Name certificateName, WireFormat wireFormat) throws SecurityException
  {
    // TODO: Handle signature algorithms other than Sha256WithRsa.
    Sha256WithRsaSignature signature = new Sha256WithRsaSignature();
    signature.getKeyLocator().setType(KeyLocatorType.KEYNAME);
    signature.getKeyLocator().setKeyName(certificateName.getPrefix(-1));

    // Append the encoded SignatureInfo.
    { // TODO: Move this into a WireFormat abstraction.
      TlvEncoder encoder = new TlvEncoder(256);
      Tlv0_1WireFormat.encodeSignatureSha256WithRsa(signature, encoder);

      interest.getName().append(new Blob(encoder.getOutput(), false));  
    }

    // Append an empty signature so that the "signedPortion" is correct.
    interest.getName().append(new Name.Component());
    // Encode once to get the signed portion.
    SignedBlob encoding = interest.wireEncode(wireFormat);
    Sha256WithRsaSignature signedSignature = (Sha256WithRsaSignature)sign
      (encoding.signedBuf(), certificateName);

    // Remove the empty signature and append the real one.
    { // TODO: Move this into a WireFormat abstraction.
      TlvEncoder encoder = new TlvEncoder(256);
      encoder.writeBlobTlv(Tlv.SignatureValue, signedSignature.getSignature().buf());
      interest.setName(interest.getName().getPrefix(-1).append
        (new Blob(encoder.getOutput(), false)));
    }
  }

  public void 
  sign(Interest interest, Name certificateName) throws SecurityException
  {
    sign(interest, certificateName, WireFormat.getDefaultWireFormat());
  }
  
  /**
   * Sign the byte buffer using a certificate name and return a Signature object.
   * @param buffer The byte array to be signed.
   * @param bufferLength the length of buffer.
   * @param certificateName The certificate name used to get the signing key and which will be put into KeyLocator.
   * @return The Signature.
   */
  public Signature
  sign(ByteBuffer buffer, Name certificateName) throws SecurityException
  {
    return identityManager_.signByCertificate(buffer, certificateName);
  }

  /**
   * Wire encode the Data object, sign it and set its signature.
   * @param data The Data object to be signed.  This updates its signature and 
   * key locator field and wireEncoding.
   * @param identityName The identity name for the key to use for signing.  
   * If empty, infer the signing identity from the data packet name.
   * @param wireFormat A WireFormat object used to encode the input. If omitted, use WireFormat getDefaultWireFormat().
   */
  public void 
  signByIdentity
    (Data data, Name identityName, WireFormat wireFormat) throws SecurityException
  {
    Name signingCertificateName;

    if (identityName.size() == 0) {
      Name inferredIdentity = policyManager_.inferSigningIdentity(data.getName());
      if (inferredIdentity.size() == 0)
        signingCertificateName = identityManager_.getDefaultCertificateName();
      else
        signingCertificateName = 
          identityManager_.getDefaultCertificateNameForIdentity(inferredIdentity);    
    }
    else
      signingCertificateName = 
        identityManager_.getDefaultCertificateNameForIdentity(identityName);

    if (signingCertificateName.size() == 0)
      throw new SecurityException("No qualified certificate name found!");

    if (!policyManager_.checkSigningPolicy(data.getName(), signingCertificateName))
      throw new SecurityException
        ("Signing Cert name does not comply with signing policy");

    identityManager_.signByCertificate(data, signingCertificateName, wireFormat);  
  }

  /**
   * Wire encode the Data object, sign it and set its signature.
   * @param data The Data object to be signed.  This updates its signature and 
   * key locator field and wireEncoding.
   * Use the default WireFormat.getDefaultWireFormat().
   * @param identityName The identity name for the key to use for signing.  
   * If empty, infer the signing identity from the data packet name.
   */
  public void 
  signByIdentity(Data data, Name identityName) throws SecurityException
  {
    signByIdentity(data, identityName, WireFormat.getDefaultWireFormat());
  }

  /**
   * Wire encode the Data object, sign it and set its signature.
   * @param data The Data object to be signed.  This updates its signature and 
   * key locator field and wireEncoding.
   * Infer the signing identity from the data packet name.
   * Use the default WireFormat.getDefaultWireFormat().
   */
  public void 
  signByIdentity(Data data) throws SecurityException
  {
    signByIdentity(data, new Name(), WireFormat.getDefaultWireFormat());
  }

  /**
   * Sign the byte buffer using an identity name and return a Signature object.
   * @param buffer The byte array to be signed.
   * @param bufferLength the length of buffer.
   * @param identityName The identity name.
   * @return The Signature.
   */
  public Signature
  signByIdentity(ByteBuffer buffer, Name identityName) throws SecurityException
  {
    Name signingCertificateName = 
      identityManager_.getDefaultCertificateNameForIdentity(identityName);

    if (signingCertificateName.size() == 0)
      throw new SecurityException("No qualified certificate name found!");

    return identityManager_.signByCertificate(buffer, signingCertificateName);
  }

  public void
  verifyData
    (Data data, OnVerified onVerified, OnVerifyFailed onVerifyFailed, 
     int stepCount)
  {
    Logger.getLogger(this.getClass().getName()).log
      (Level.INFO, "Enter Verify");

    if (policyManager_.requireVerify(data)) {
      ValidationRequest nextStep = policyManager_.checkVerificationPolicy
        (data, stepCount, onVerified, onVerifyFailed);
      if (nextStep != null) {
        VerifyCallbacks callbacks = new VerifyCallbacks
          (nextStep, nextStep.retry_, onVerifyFailed, data);
        try {
          face_.expressInterest(nextStep.interest_, callbacks, callbacks);
        } 
        catch (IOException ex) {
          onVerifyFailed.onVerifyFailed(data);
        }
      }
    }
    else if (policyManager_.skipVerifyAndTrust(data))
      onVerified.onVerified(data);
    else
      onVerifyFailed.onVerifyFailed(data);
  }
  
  /**
   * Check the signature on the Data object and call either onVerify.onVerify or 
   * onVerifyFailed.onVerifyFailed. 
   * We use callback functions because verify may fetch information to check the 
   * signature.
   * @param data The Data object with the signature to check. It is an error if 
   * data does not have a wireEncoding. 
   * To set the wireEncoding, you can call data.wireDecode.
   * @param onVerified If the signature is verified, this calls 
   * onVerified.onVerified(data).
   * @param onVerifyFailed If the signature check fails, this calls 
   * onVerifyFailed.onVerifyFailed(data).
   */
  public void
  verifyData(Data data, OnVerified onVerified, OnVerifyFailed onVerifyFailed)
  {
    verifyData(data, onVerified, onVerifyFailed, 0);
  }

  
  
  /**
   * Set the Face which will be used to fetch required certificates.
   * @param face The Face object.
   */
  public void
  setFace(Face face) { face_ = face; }
  
  /**
   * A VerifyCallbacks is used for callbacks from verifyData.
   */
  private class VerifyCallbacks implements OnData, OnTimeout {
    public VerifyCallbacks
      (ValidationRequest nextStep, int retry, OnVerifyFailed onVerifyFailed,
       Data data)
    {
      nextStep_ = nextStep;
      retry_ = retry;
      onVerifyFailed_ = onVerifyFailed;
      data_ = data;
    }

    public void onData(Interest interest, Data data)
    {
      // Try to verify the certificate (data) according to the parameters in 
      //   nextStep.
      verifyData
        (data, nextStep_.onVerified_, nextStep_.onVerifyFailed_, 
         nextStep_.stepCount_);
    }

    public void onTimeout(Interest interest) 
    {
      if (retry_ > 0) {
        // Issue the same expressInterest as in verifyData except decrement 
        //   retry.
        VerifyCallbacks callbacks = new VerifyCallbacks
          (nextStep_, retry_ - 1, onVerifyFailed_, data_);
        try {
          face_.expressInterest(interest, callbacks, callbacks);
        } 
        catch (IOException ex) {
          onVerifyFailed_.onVerifyFailed(data_);
        }
      }
      else
        onVerifyFailed_.onVerifyFailed(data_);
    }

    private ValidationRequest nextStep_;
    private int retry_;
    private OnVerifyFailed onVerifyFailed_;
    private Data data_;
  }
  
  private IdentityManager identityManager_;
  private PolicyManager policyManager_;
  private EncryptionManager encryptionManager_;
  private Face face_ = null;
  private int maxSteps_ = 100;
}
