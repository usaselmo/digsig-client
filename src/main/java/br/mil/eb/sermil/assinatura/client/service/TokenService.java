package br.mil.eb.sermil.assinatura.client.service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.demoiselle.signer.core.CertificateManager;
import org.demoiselle.signer.core.keystore.loader.factory.KeyStoreLoaderFactory;
import org.demoiselle.signer.policy.engine.factory.PolicyFactory.Policies;
import org.demoiselle.signer.policy.impl.cades.SignerAlgorithmEnum;
import org.demoiselle.signer.policy.impl.cades.factory.PKCS7Factory;
import org.demoiselle.signer.policy.impl.cades.pkcs7.PKCS7Signer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import br.mil.eb.sermil.assinatura.client.util.Messages;
import br.mil.eb.sermil.assinatura.core.dto.CidDocMilitarDTO;
import br.mil.eb.sermil.assinatura.core.exception.AssinaturaException;
import br.mil.eb.sermil.assinatura.core.type.AssinaturaEntity;
import br.mil.eb.sermil.assinatura.core.type.CertICPBrasil;
import br.mil.eb.sermil.assinatura.core.type.Preferences;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Anselmo S Ribeiro <anselmo.sr@gmail.com>
 * @version 1.3-GO
 * @since 0.1.0
 */
@Slf4j
@Service
public class TokenService {

  private static KeyStore keyStore;

  @Autowired
	private Messages messages;
  
  public PublicKey getPublicKey(String alia) throws KeyStoreException {
    return this.getKeyStore().getCertificate(alia).getPublicKey();
  }
  
  public PrivateKey getPrivateKey(String alias) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
    val keyStore = readKeyStore();
    return (PrivateKey) keyStore.getKey(alias, null);
  }

	public KeyStore readKeyStore() {
	  log.info("Searching keystore....");
	  val keyStoreLoader = KeyStoreLoaderFactory.factoryKeyStoreLoader();
	  keyStore = keyStoreLoader.getKeyStore();
	  log.info("A Keystore of type {} was found.", keyStore.getType());
	  return keyStore;
	}

	public KeyStore getKeyStore() {
	  this.readKeyStore();
	  return keyStore;
	}

	public List<String> getAliases() throws KeyStoreException {
	  return Collections.list(this.getKeyStore().aliases());
	}

	public X509Certificate getCertificate(String alia) throws AssinaturaException, KeyStoreException {
	  return (X509Certificate) this.getKeyStore().getCertificate(alia);
	}

	public PKCS7Signer getSigner(String alias) throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException {

    val signer = PKCS7Factory.getInstance().factoryDefault();

    val keyStore = readKeyStore();
    val privateKey = (PrivateKey) keyStore.getKey(alias, null);
    val certificateChain = keyStore.getCertificateChain(alias);

    signer.setCertificates(certificateChain);
    signer.setPrivateKey(privateKey);

    signer.setSignaturePolicy(Policies.AD_RB_CADES_2_2);
    signer.setAlgorithm(SignerAlgorithmEnum.SHA256withRSA);

    return signer;
  }

  public byte[] signDetatched(PKCS7Signer signer, byte[] content) {
    val assinatura = signer.doDetachedSign(content);
    return assinatura;
  }

  public byte[] signAttatched(PKCS7Signer signer, byte[] certificateNotSigned) {
    return signer.doAttachedSign(certificateNotSigned);
  }

  public CertICPBrasil getICPBrasilCertificate(String alias) throws AssinaturaException, KeyStoreException {
	  val certificate = getCertificate(alias);
	  val cm = new CertificateManager(certificate);
	  return cm.load(CertICPBrasil.class);
	}

	public String getCpfFromToken(String alias) throws AssinaturaException, KeyStoreException {
	  val cpf_ = getICPBrasilCertificate(alias).getCpf();
	  if (StringUtils.isEmpty(cpf_))
	    throw new AssinaturaException(messages.get("certificate.cpf.not.found"));
	  return cpf_;
	}

	public CidDocMilitarDTO signDTOCertificate(final CidDocMilitarDTO notSignedDTOCertificate, final String alias, final Preferences preferences)
	    throws Exception {
	
	  // sign pdf
		val notSignedPdf = notSignedDTOCertificate.getPdf();
	  val signature = createDetatchedSignature(notSignedPdf, alias, preferences);
	  
	  val signedDTOCertificate = notSignedDTOCertificate;
	  signedDTOCertificate.setPdf(notSignedPdf);
	  signedDTOCertificate.setAssinatura(signature);
	  signedDTOCertificate.setAssinado(true);
	  signedDTOCertificate.setImage(null); // reset image
	
	  return signedDTOCertificate;
	}

	public void storeSignedPdfInLocalFileSystem(final CidDocMilitarDTO signedDTOCertificate, final Preferences preferences, final AssinaturaEntity ae) {
	  if (preferences.getConfig().isSavePdfFileInMyLocalSystem() && !StringUtils.isEmpty(preferences.getConfig().getPdfFileSavingLocation())) {
	    String fileName = new StringBuilder().append(preferences.getConfig().getPdfFileSavingLocation().replace("\\", File.separator)).append(File.separator).append("RA_")
	        .append(signedDTOCertificate.getRa()).append("-")
	        .append(new SimpleDateFormat("yyyy-MM-dd_HHmm").format(new Date())).append("-").append(signedDTOCertificate.getTipo())
	        .append(".pdf").toString();
	    if(fileName.startsWith("/") || fileName.startsWith(File.separator))
	    	fileName = "C:" + fileName;
	    try {
				Files.write(Paths.get(fileName), signedDTOCertificate.getPdf(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			} catch (Exception e) {
				ae.addErrorMsg("Nao foi possivel salvar copia do Certificado assinado de " + signedDTOCertificate.getCidadao().getNome());
			}
	  }
	}

	public void storeSignatureInLocalFileSystem(final CidDocMilitarDTO signedDTOCertificate, final Preferences preferences, AssinaturaEntity ae) {
	  if (preferences.getConfig().isSavePdfFileInMyLocalSystem() && !StringUtils.isEmpty(preferences.getConfig().getPdfFileSavingLocation())) {
	    String fileName = new StringBuilder().append(preferences.getConfig().getPdfFileSavingLocation().replace("\\", "/")).append("/").append("RA_")
	        .append(signedDTOCertificate.getRa()).append("-")
	        .append(new SimpleDateFormat("yyyy-MM-dd_HHmm").format(new Date())).append("-ASSINATURA").append(".p7s").toString();
	    if(fileName.startsWith("/") || fileName.startsWith(File.separator))
	    	fileName = "C:" + fileName;
	    try {
				Files.write( Paths.get(fileName), signedDTOCertificate.getAssinatura(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			} catch (Exception e) {
				ae.addErrorMsg("Nao foi possivel salvar a assinatura digital do Certificado de " + signedDTOCertificate.getCidadao().getNome());
			}
	  }
	}

  public byte[] createDetatchedSignature(final byte[] content, final String alias, final Preferences preferences)
      throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
    return signDetatched(getSigner(alias), content);
  }

}
