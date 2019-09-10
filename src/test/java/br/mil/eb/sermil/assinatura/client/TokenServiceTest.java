package br.mil.eb.sermil.assinatura.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.demoiselle.signer.policy.impl.cades.SignerAlgorithmEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

import br.mil.eb.sermil.assinatura.client.service.TokenService;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Anselmo S Ribeiro <anselmo.sr@gmail.com>
 * @version 1.2
 * @since 1.2
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
public class TokenServiceTest {

	@Autowired
	TokenService tokenService;
	private boolean isKeyStorePresent;
	private List<String> aliases;

	@Test
	@Before
	public void loadConfig() throws Exception {
		assertNotNull(this.tokenService);
		try {
			this.tokenService.readKeyStore();
			this.isKeyStorePresent = true;
		} catch (Exception e) {
			this.isKeyStorePresent = false;
			log.error("Key Store NOT present: {}", e.getMessage());
		}

	}
	
	@Test
	public void test_01() throws Exception {
		this.setAliases();
		this.setSigner();
		this.assinar();
	}

	private void setAliases() throws Exception {
		if (isKeyStorePresent) {
			val alieases = this.tokenService.getAliases();
			assertNotNull(alieases);
			assertTrue(alieases.size() > 0);
			this.aliases = alieases;
			Stream.of(alieases).forEach(alias -> log.info("Alias: {}", alias));
		}
	}

  private void setSigner() throws Exception {
    if (isKeyStorePresent) {
      val signer = this.tokenService.getSigner(this.aliases.get(0));
      assertNotNull(signer);
      assertNotNull(signer.getAlgorithm());
      assertSame(SignerAlgorithmEnum.SHA256withRSA.getAlgorithm(), signer.getAlgorithm());
      log.info("Signer algorithm: {}", signer.getAlgorithm());
    }
  }

  private void assinar() throws Exception {
    if (isKeyStorePresent) {
      val signer = this.tokenService.getSigner(this.aliases.get(0));
      if(signer != null) {
        signer.doDetachedSign("AAAAA".getBytes());
      }
    }
  }

}


