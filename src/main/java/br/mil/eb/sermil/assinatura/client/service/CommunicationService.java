package br.mil.eb.sermil.assinatura.client.service;

import java.io.UnsupportedEncodingException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.mil.eb.sermil.assinatura.client.util.Messages;
import br.mil.eb.sermil.assinatura.core.dto.CidDocMilitarDTO;
import br.mil.eb.sermil.assinatura.core.exception.AssinaturaException;
import br.mil.eb.sermil.assinatura.core.type.AssinaturaEntity;
import br.mil.eb.sermil.assinatura.core.type.Preferences;
import br.mil.eb.sermil.assinatura.core.type.Preferences.Config;
import br.mil.eb.sermil.assinatura.core.util.Constants;
import br.mil.eb.sermil.assinatura.core.util.CryptoUtil;
import br.mil.eb.sermil.assinatura.core.util.JwtTokenUtil;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Anselmo S Ribeiro <anselmo.sr@gmail.com>
 * @version 1.3-GO
 * @since 0.1.0
 */
@Slf4j
@Service
public class CommunicationService {

	@Autowired
	Environment env;

	@Autowired
	private Messages messages;

	public AssinaturaEntity get(String url, String cpf, Preferences prefs) throws AssinaturaException {
		try {
			val entity = new HttpEntity<Object>(getHeaders(cpf, prefs));
			val client = getRestTemplate(prefs);
			val url2 = getServerUrl(prefs) + url;
			log.info("Iniciando comunicacao GET com Assinatura Servidor: {}", url2);
			val res = client.exchange(new URI(url2), HttpMethod.GET, entity, AssinaturaEntity.class);
			log.info("Comunicacao GET com Assinatura Servidor terminada: {}", new ObjectMapper().writeValueAsString(res));
			return res.getBody();
		} catch (Throwable e) {
			return this.handleException(e);
		}
	}

	public <B> AssinaturaEntity post(B body, String url, String cpf, Preferences prefs) throws AssinaturaException {
	  try {
	    val entity = new HttpEntity<B>(body, getHeaders(cpf, prefs));
	    val client = getRestTemplate(prefs);
	    val url2 = getServerUrl(prefs) + url;
			log.info("Iniciando comunicacao GET com Assinatura Servidor: {}", url2);
	    val res = client.postForEntity(new URI(url2), entity, AssinaturaEntity.class);
			log.info("Comunicacao GET com Assinatura Servidor terminada: {}", new ObjectMapper().writeValueAsString(res));
	    return res.getBody();
	  } catch (Throwable e) {
	    return this.handleException(e);
	  }
	}

	public RestTemplate getRestTemplate(Preferences prefs) {
	  RestTemplate rt = new RestTemplateBuilder().build();
  	if (prefs.getConfig().getProxy() != null && !StringUtils.isEmpty(prefs.getConfig().getProxy().getAddress())) {
  		prefs.getConfig().getProxy().setAddress(prefs.getConfig().getProxy().getAddress().replace("http://", ""));
  		val clientHttpReq = new SimpleClientHttpRequestFactory();
  		val proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(prefs.getConfig().getProxy().getAddress(), Integer.valueOf(prefs.getConfig().getProxy().getPort())));
  		clientHttpReq.setProxy(proxy);
  		if (prefs.getConfig().getProxy().isAuthenticated()) {
  			Authenticator.setDefault(new Authenticator() {
  				@Override
  				protected PasswordAuthentication getPasswordAuthentication() {
  					return new PasswordAuthentication(prefs.getConfig().getProxy().getUser(), prefs.getConfig().getProxy().getPassword().toCharArray());
  				}
  			});
  		}
  		rt.setRequestFactory(clientHttpReq);
  		return rt;
  	} else {
  		return rt;
  	}
  }

  public byte[] getSignablePdf(final CidDocMilitarDTO dtoCertificate, final String cpf, final Preferences prefs)
	    throws RestClientException, URISyntaxException, AssinaturaException {
		dtoCertificate.setAssinado(true);
		val signablePdf = post(dtoCertificate, "/user/certificate/pdf", cpf, prefs).getCertificado().getPdf();
		dtoCertificate.setAssinado(false);
		return signablePdf;
	}

  public AssinaturaEntity saveSignedDTOCertificate(final CidDocMilitarDTO signedDTOCertificate, final String cpf, final Preferences prefs)
      throws RestClientException, URISyntaxException, AssinaturaException {
    return post(AssinaturaEntity.builder().certificado(signedDTOCertificate).build(), "/token/certificate/persistir", cpf, prefs);
  }

	public void setProxy(Optional<Config> optional) {
		if (optional.isPresent() && optional.get().getProxy() != null)
			setProxy(optional.get().getProxy());
	}

	private void setProxy(br.mil.eb.sermil.assinatura.core.type.Preferences.Config.Proxy p) {
		if (!StringUtils.isEmpty(p.getAddress()) && !StringUtils.isEmpty(p.getPort())) {
			org.demoiselle.signer.core.util.Proxy.setProxyEndereco(p.getAddress());
			org.demoiselle.signer.core.util.Proxy.setProxyPorta(p.getPort());
			if (p.isAuthenticated()) {
				org.demoiselle.signer.core.util.Proxy.setProxyUsuario(p.getUser());
				org.demoiselle.signer.core.util.Proxy.setProxySenha(p.getPassword());
			}
		}
	}

	private HttpHeaders getHeaders(String cpf, Preferences prefs) throws JsonProcessingException, UnsupportedEncodingException, GeneralSecurityException {
		val headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.set(Constants.TOKEN_NAME, JwtTokenUtil.generateToken(cpf));
		headers.set(env.getProperty("header.prefs.name"), new ObjectMapper().writeValueAsString(prefs));
		headers.set(Constants.VERIFICATION_PARAMETER, CryptoUtil.encrypt(Constants.SECRET + CryptoUtil.SALT));
		return headers;
	}

	private String getServerUrl(Preferences prefs) throws AssinaturaException {
		if (prefs.getConfig().getDataSource().equalsIgnoreCase("local"))
			return env.getProperty("local.server.url");
		else if (prefs.getConfig().getDataSource().equalsIgnoreCase("remote"))
			return env.getProperty("remote.server.url");
		else
			throw new AssinaturaException(messages.get("preferences.datasource.not.defined"));
	}

	private AssinaturaEntity handleException(Throwable e) throws AssinaturaException {
		log.error(e.getMessage());
		val ae = new AssinaturaEntity();
		if(e.getClass().getName().equals(AssinaturaException.class.getName()))
		  ae.addErrorMsg(e.getMessage());
		else if (StringUtils.isEmpty(e.getMessage()))
			ae.addErrorMsg("Erro inesperado: " + e.getClass().getName());
		else if (e.getMessage().contains("o route to host"))
		  ae.addErrorMsg(messages.get("diagnostic.initernet.error"));
		else if (e.getMessage().contains("Connection refused") || e.getMessage().contains("java.net.UnknownHostException"))
		  ae.addErrorMsg(messages.get("conection.interupted"));
		else if (e.getMessage().contains("java.net.ConnectException: Connection timed out"))
		  ae.addErrorMsg(messages.get("diagnostic.initernet.error"));
		else if (e.getMessage().contains("Connection reset"))
		  ae.addErrorMsg(messages.get("conection.reset"));
		else ae.addErrorMsg(e.getMessage());
		
		return ae;
	}

}
