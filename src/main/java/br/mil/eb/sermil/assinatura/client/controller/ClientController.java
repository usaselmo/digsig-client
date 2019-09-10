package br.mil.eb.sermil.assinatura.client.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.mil.eb.sermil.assinatura.client.AssinaturaClient;
import br.mil.eb.sermil.assinatura.client.service.CommunicationService;
import br.mil.eb.sermil.assinatura.client.service.DiagnosticService;
import br.mil.eb.sermil.assinatura.client.service.TokenService;
import br.mil.eb.sermil.assinatura.client.util.Messages;
import br.mil.eb.sermil.assinatura.core.dto.CidDocMilitarDTO;
import br.mil.eb.sermil.assinatura.core.exception.AssinaturaException;
import br.mil.eb.sermil.assinatura.core.type.AssinaturaEntity;
import br.mil.eb.sermil.assinatura.core.type.Preferences;
import br.mil.eb.sermil.assinatura.core.util.CryptoUtil;
import javafx.application.Platform;
import javafx.stage.DirectoryChooser;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Anselmo S Ribeiro <anselmo.sr@gmail.com>
 * @version 1.3-GO
 * @since 0.1.0
 */
@Slf4j
@RestController
public class ClientController {

  @Autowired
  private Environment env;

  @Autowired
  private CommunicationService communicationService;

  @Autowired
  private TokenService tokenService;

  @Autowired
  private DiagnosticService diagnosticService;

  @Autowired
  private Messages messages;

  @PostMapping(value = { "/diagnostic/run" }, consumes = { MediaType.APPLICATION_JSON_VALUE }, produces = {
      MediaType.APPLICATION_JSON_VALUE })
  public AssinaturaEntity diagnosticRun(@RequestBody Preferences prefs) throws JsonProcessingException {
    log.info("Running diagnostics: {}", new ObjectMapper().writeValueAsString(prefs));
    return diagnosticService.run(prefs);
  }

  @PostMapping(value = {
      "/preferencias/carregar" }, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public AssinaturaEntity preferenciasCarregar(@RequestBody Preferences prefs) {
    try {
      if (!Files.exists(Paths.get(env.getProperty("application.preferences.file"))))
        savePreferencesFile(prefs);
      else {
        val res = readPreferencesFile(env.getProperty("application.preferences.file"));
        if (res == null || res.getConfig() == null || res.getFilter() == null)
          savePreferencesFile(prefs);
        else
          prefs = res;
      }
      communicationService.setProxy(Optional.of(prefs.getConfig()));
      log.info("CONFIGURACOES DE USUARIO SALVAS/CARREGADAS COM SUCESSO.");
      AssinaturaEntity ae = AssinaturaEntity.builder().preferences(prefs).build().addInfoMsg(messages.get("preferences.save.success"));
      this.diagnosticService.checkFileConfig(Optional.of(prefs.getConfig()), ae);
      return ae;
    } catch (Exception e) {
      e.printStackTrace();
      log.error("NAO FOI POSSIVEL CARREGAR AS PREFERENCIAS/CONFITURACOES DO USUARIO: {}", e.getMessage());
      return handleException(e).addErrorMsg(messages.get("preferences.load.error"));
    }
  }

  @PostMapping(value = { "/fileChooser" }, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public AssinaturaEntity fileChooser_(@RequestBody Preferences prefs) {
    try {
      val previousLocation = prefs.getConfig().getPdfFileSavingLocation();
      prefs.getConfig().setPdfFileSavingLocation(null);
      Platform.runLater(() -> {
        val directoryChooser = new DirectoryChooser();
        val selectedDirectory = directoryChooser.showDialog(AssinaturaClient.stage).getAbsolutePath();
        if (!StringUtils.isEmpty(selectedDirectory))
          prefs.getConfig().setPdfFileSavingLocation(selectedDirectory);
        else
          prefs.getConfig().setPdfFileSavingLocation(previousLocation);
      });
      while (prefs.getConfig().getPdfFileSavingLocation() == null) {
        Thread.sleep(100);
      }
      return AssinaturaEntity.builder().preferences(prefs).build();
    } catch (Exception e) {
      return handleException(e).addErrorMsg(messages.get("preferences.load.error"));
    }
  }

  @PostMapping(value = { "/preferencias/salvar" }, consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public AssinaturaEntity savePreferences(@RequestBody Preferences prefs) {
    try {
      savePreferencesFile(prefs);
      communicationService.setProxy(Optional.of(prefs.getConfig()));
      log.info("CONFIGURACOES DE USUARIO SALVAS COM SUCESSO.");
      val ae = AssinaturaEntity.builder().preferences(prefs).build().addInfoMsg(messages.get("preferences.save.success"));
      this.diagnosticService.checkFileConfig(Optional.of(prefs.getConfig()), ae);
      return ae;
    } catch (Exception e) {
      log.error("NAO FOI POSSIVEL SALVAR AS PREFERENCIAS/CONFIGURACOES DO USUARIO: {}", e.getMessage());
      return handleException(e).setPreferences(prefs).addErrorMsg(messages.get("preferences.save.error"));
    }
  }

  @PostMapping("/version/check")
  public AssinaturaEntity versionCheck(@RequestBody Preferences prefs, @RequestParam String cpf) {
    try {
      log.info("Iniciando checagem de versao: {}", new ObjectMapper().writeValueAsString(prefs));
      val ae = this.communicationService.get("/version/check", cpf, prefs);
      this.checkVersions(ae, env.getProperty("application.version"));
      log.info("Checagem de versao terminada: {}", new ObjectMapper().writeValueAsString(ae));
      return ae;
    } catch (Exception e) {
      log.error("ERRO NA TENTATIVA DE CONFERIR VERSOES DE USUARIO E SERVIDOR: " + e.getMessage());
      return handleException(e);
    }
  }

  @RequestMapping("/token/get_aliases")
  public AssinaturaEntity getAliases() {
    try {
      val aliases = this.tokenService.getAliases();
      if (aliases != null && aliases.size() <= 0)
        return new AssinaturaEntity().addErrorMsg(messages.get("certificate.not.found"));
      else
        return AssinaturaEntity.builder().aliases(aliases).build().addInfoMsg(messages.get("certificate.present"));
    } catch (Exception e) {
      log.error("ERRO AO PROCURAR POR CERTIFICADOS DIGITAIS: {}", e.getMessage());
      return handleException(e).addErrorMsg(messages.get("certificate.not.found")).addErrorMsg(messages.get("certificate.not.found.alert"))
          .addErrorMsg(messages.get("certificate.not.found.alert2"));
    }
  }

  @GetMapping("/token/certificate/info")
  public AssinaturaEntity setXcertInfo(@RequestParam String alias) {
    try {
      val certIcpBrasil = this.tokenService.getICPBrasilCertificate(alias);
      return AssinaturaEntity.builder().certICPBrasil(certIcpBrasil).build().addInfoMsg(messages.get("token.found"));
    } catch (Exception e) {
      log.error("ERRO AO TENTAR BUSCAR SUAS INFORMACOES DENTRO DO TOKEN OU IDENTIDADE: {}", e.getMessage());
      return handleException(e).addErrorMsg(messages.get("token.notfound")).addErrorMsg(messages.get("token.notfound.alert"));
    }
  }

  @PostMapping(value = "/user/info", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public AssinaturaEntity getUserInfo(@RequestBody Preferences prefs, @RequestParam String cpf) {
    try {
      log.info("Iniciando processo de verificacao de informacoes de usuario: {}", new ObjectMapper().writeValueAsString(prefs));
      val ae = communicationService.get("/user/info", cpf, prefs);
      log.info("Processo de verificacao de informacoes de usuario terminado: {}", new ObjectMapper().writeValueAsString(ae));
      return ae;
    } catch (Exception e) {
      log.error("ERRO AO TENTAR TRAZER DO SERMIL SUAS INFORMACOES DE USUARIO: {}", e.getMessage());
      return handleException(e);
    }
  }

  @PostMapping("/user/jsms")
  public AssinaturaEntity userJsms(@RequestBody Preferences prefs, @RequestParam String cpf) throws IOException {
    try {
      return communicationService.get("/user/jsms", cpf, prefs);
    } catch (Exception e) {
      log.error("ERRO AO TENTAR TRAZER DO SERMIL AS SUAS JUNTAS DE SERVICO MILITAR: {}", e.getMessage());
      return handleException(e);
    }
  }

  @PostMapping("/user/certificates")
  public AssinaturaEntity getUserCertificates(@RequestBody Preferences prefs, @RequestParam String cpf) {
    try {
      val ae = communicationService.post(prefs, "/user/certificates", cpf, prefs);
      if (ae.getCertificados() != null && ae.getCertificados().size() > 0)
        ae.getCertificados().forEach(c -> c.setAssinaUsuario(ae.getUsuario()));
      return ae;
    } catch (Exception e) {
      log.error("ERRO AO TRAZER DO SERMIL SEUS CERTIFICADOS MILITARES PARA ASSINAR: {}", e.getMessage());
      return handleException(e);
    }
  }

  @PostMapping("/certificate/image")
  public AssinaturaEntity certificateImage(@RequestBody AssinaturaEntity assEntity) {
    try {
      val certificado = assEntity.getCertificado();
      if (certificado.getImage() == null) {
        val ae = communicationService.post(certificado, "/user/certificate/image", certificado.getAssinaUsuario().getCpf(),
            assEntity.getPreferences());
        if (ae.getErrorMsgs() != null && ae.getErrorMsgs().size() > 0)
          throw new AssinaturaException(ae.getErrorMsgs().get(0));
        certificado.setImage(ae.getCertificado().getImage());
      }
      return AssinaturaEntity.builder().certificado(certificado).build();
    } catch (Exception e) {
      log.error("ERRO AO TENTAR GERAR A IMAGEM DO CERTIFICADO: {}", e.getMessage());
      return handleException(e);
    }
  }

  @PostMapping("/certificate/download")
  public void certificatePdfDownload(@RequestBody CidDocMilitarDTO certificate, @Autowired HttpServletResponse resonse) throws IOException {
    resonse.setContentType("application/pdf");
    resonse.setHeader("Content-Disposition", "attachment;filename=RA" + certificate.getRa());
    resonse.getOutputStream().write(certificate.getPdf());
  }

  @PostMapping("/token/solicitar_senha")
  public AssinaturaEntity solicitarSenha(@RequestBody Preferences prefs, @RequestParam String alias) {
    try {
      tokenService.createDetatchedSignature("AAAAAA".getBytes(), alias, prefs);
      return new AssinaturaEntity();
    } catch (Exception e) {
      log.error("ERRO AO INICIAR O PROCESSO DE ASSINATURA: {}", e.getMessage());
      return handleException(e);
    }

  }

  @PostMapping("/token/certificate/assinar")
  public AssinaturaEntity assinar(@RequestBody AssinaturaEntity clientEntity, @RequestParam String alias) {
    try {
      val notSignedDTOCertificate = clientEntity.getCertificado();
      val prefs = clientEntity.getPreferences();
      val cpf = clientEntity.getCertificado().getAssinaUsuario().getCpf();

      // SERMIL - gerar pdf
      notSignedDTOCertificate.setPdf(communicationService.getSignablePdf(notSignedDTOCertificate, cpf, prefs));

      // LOCAL - assinar pdf
      val signedDTOCertificate = tokenService.signDTOCertificate(notSignedDTOCertificate, alias, prefs);

      // SERMIL - persistir pdf e assinatura
      val serverResponseEntity = communicationService.saveSignedDTOCertificate(signedDTOCertificate, cpf, prefs);

      // store in local file system
      tokenService.storeSignedPdfInLocalFileSystem(signedDTOCertificate, prefs, serverResponseEntity);
      tokenService.storeSignatureInLocalFileSystem(signedDTOCertificate, prefs, serverResponseEntity);

      // retornar certificado assinado
      serverResponseEntity.setCertificado(signedDTOCertificate);
      return serverResponseEntity;

    } catch (Exception e) {
      log.error(e.getMessage());
      return new AssinaturaEntity()
          .addErrorMsg(messages.get("certificate.sign.error.v1", clientEntity.getCertificado().getCidadao().getNome()));
    }
  }

  @RequestMapping(value = { "window/exit" })
  public void exit() {
    log.info("USUARIO SOLICITOU TERMINO DO PROCESSO");
    Platform.exit();
  }

  private Preferences readPreferencesFile(String prefFileLocation) throws IOException, GeneralSecurityException {
    byte[] line = Files.readAllBytes(new File(prefFileLocation).toPath());
    Preferences prefs = new ObjectMapper().readValue(line, Preferences.class);
    if (prefs.getConfig() != null && prefs.getConfig().getProxy() != null
        && !StringUtils.isEmpty(prefs.getConfig().getProxy().getPassword()))
      prefs.getConfig().getProxy().setPassword(CryptoUtil.decrypt(prefs.getConfig().getProxy().getPassword()));
    return prefs;
  }

  private void savePreferencesFile(Preferences prefs) throws GeneralSecurityException, IOException {
    if (prefs.getConfig() != null && prefs.getConfig().getProxy() != null
        && !StringUtils.isEmpty(prefs.getConfig().getProxy().getPassword()))
      prefs.getConfig().getProxy().setPassword(CryptoUtil.encrypt(prefs.getConfig().getProxy().getPassword()));
    FileUtils.writeByteArrayToFile(new File(env.getProperty("application.preferences.file")),
        new ObjectMapper().writeValueAsString(prefs).getBytes());
  }

  private AssinaturaEntity handleException(Exception e) {
    log.error("ERRO NO PROCESSAMENTO LOCAL: {}", e.getMessage());
    val ae = new AssinaturaEntity();
    if (StringUtils.isEmpty(e.getMessage()))
      ae.addErrorMsg(messages.get("exception.indefinida"));
    else if (e.getMessage().contains(messages.get("demoiselle.error.msg01"))) {
      ae.addErrorMsg(messages.get("certificate.validation.error"));
      ae.addErrorMsg(messages.get("diagnostic.proxy"));
    } else if (e.getMessage().contains(messages.get("demoiselle.error.msg02")))
      ae.addErrorMsg(messages.get("certificate.not.present"));
    else if (e.getMessage().contains(messages.get("demoiselle.error.msg03")))
      ae.addErrorMsg(messages.get("signature.canceled.by.user"));
    else
      ae.addErrorMsg(e.getMessage());
    return ae;
  }

  private void checkVersions(AssinaturaEntity ae, String version2) {
    if (!ae.getVersion().equals(version2)) {
      ae.addWarningMsg(messages.get("version.not.equal"));
    } else {
      // Do nothing. Not a big deal
    }
  }

}
