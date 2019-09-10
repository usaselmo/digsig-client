package br.mil.eb.sermil.assinatura.client.service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import br.mil.eb.sermil.assinatura.client.util.Messages;
import br.mil.eb.sermil.assinatura.core.type.AssinaturaEntity;
import br.mil.eb.sermil.assinatura.core.type.Preferences;
import br.mil.eb.sermil.assinatura.core.type.Preferences.Config;
import br.mil.eb.sermil.assinatura.core.util.Constants;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Anselmo S Ribeiro <anselmo.sr@gmail.com>
 * @version 1.3-GO
 * @since 0.1.0
 */
@Slf4j
@Service
public class DiagnosticService {

  @Autowired
  Environment env;

  @Autowired
  Messages messages;

  @Autowired
  private CommunicationService commService;

  public AssinaturaEntity run(Preferences prefs) {
    val ae = new AssinaturaEntity();
    log.debug("begining connection test ...");
    if (isConnectedToSerpro())
      ae.addInfoMsg(messages.get("diagnostic.serpro.connection.ok"));
    else
      ae.addErrorMsg(messages.get("diagnostic.serpro.connection.failure"));

    if (isConnectedToAssinaturaServer(prefs))
      ae.addInfoMsg(messages.get("diagnostic.assinatura.server.connection.ok"));
    else
      ae.addErrorMsg(messages.get("diagnostic.assinatura.server.connection.error"));

    return ae;
  }

  private boolean isConnectedToSerpro() {
    val client = getClient();
    val url = env.getProperty("server.serpro.url");
    try {
      val res = client.optionsForAllow(new URI(url));
      if (res != null && res.size() > 0)
        return true;
      return false;
    } catch (Exception e) {
      log.error("SEM ACESSO AO SERPRO PARA VALIDAÇÃO DE CERTIFICADOS DIGITAIS: " + e.getMessage() + ", URL: " + url);
      return false;
    }
  }

  private boolean isConnectedToAssinaturaServer(Preferences prefs) {
    try {
      return this.commService.get("/autoteste", "", prefs).getInfoMsgs().stream()
          .filter(msg -> msg.contains(Constants.ASSINATURA_SERVIDOR_FUNCIONANDO)).count() > 0;
    } catch (Exception e) {
      log.error("NAO HA CONEXAO COM O SERVIDOR DO ASSINATURA DIGITAL: " + e.getMessage());
      return false;
    }
  }

  private RestTemplate getClient() {
    val preferences = new Preferences();
    val config = new Preferences.Config();
    config.setDataSource("remote");
    preferences.setConfig(config);
    val client = this.commService.getRestTemplate(preferences);
    return client;
  }

  public void checkFileConfig(final Optional<Config> config, final AssinaturaEntity ae) {
    if (config.isPresent()) {
      if (config.get().isSavePdfFileInMyLocalSystem()) {
        if (StringUtils.isEmpty(config.get().getPdfFileSavingLocation()))
          writeFolderErrorMsgs(ae);
        else {
          val folder = new File(config.get().getPdfFileSavingLocation());
          if (!folder.exists()) {
            try {
              FileUtils.forceMkdir(folder);
            } catch (IOException e) {
              writeFolderErrorMsgs(ae);
            }
          }
        }
      }
    } else {
      writeFolderErrorMsgs(ae);
    }
  }

  private void writeFolderErrorMsgs(final AssinaturaEntity ae) {
    val msg = "Não foi possível achar ou criar a pasta para salvar cópias dos Certificados assinados digitalmente em seu computador. "
        + "Isso pode causar erro no processamento da Assinatura Digital. " + "Confira as permissões de seu usuário de máquina.";
    log.error(msg);
    ae.addErrorMsg(msg);
  }

}
