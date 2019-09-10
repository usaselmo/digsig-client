package br.mil.eb.sermil.assinatura.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_ATOM_XML;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PDF;
import static org.springframework.http.MediaType.APPLICATION_RSS_XML;
import static org.springframework.http.MediaType.APPLICATION_XHTML_XML;
import static org.springframework.http.MediaType.APPLICATION_XML;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;
import static org.springframework.http.MediaType.TEXT_HTML;
import static org.springframework.http.MediaType.TEXT_PLAIN;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.mil.eb.sermil.assinatura.core.type.AssinaturaEntity;
import br.mil.eb.sermil.assinatura.core.type.Preferences;

/**
 * @author Anselmo S Ribeiro <anselmo.sr@gmail.com>
 * @version 1.2
 * @since 1.2
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
public class ControllersTest {

  private static final String USER_INFO = "/user/info?cpf=55555555555";

  private static String HTTP_LOCALHOST;

  private static final String PREFERENCIAS_CARREGAR = "/preferencias/carregar";
  private static final String DIAGNOSTIC_RUN = "/diagnostic/run";
  private static final String PREFERENCIAS_SALVAR = "/preferencias/salvar";

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private MockMvc mockMvc;

  @Test
  @Before
  public void init() throws Exception {
    assertNotNull(this.mockMvc);
    assertNotNull(this.restTemplate);
    HTTP_LOCALHOST = "http://localhost:8765/";
  }

  /**
   * General Tests
   */

  @Test
  public void test_GeneralError_ReturnAssinaturaEntity() {
    assertEquals(restTemplate.getForObject(HTTP_LOCALHOST + "sdfsdd", AssinaturaEntity.class).getClass(), AssinaturaEntity.class);
  }

  /**
   * Diagnostic run
   */

  @Test
  public void test_() throws Exception {
    // arrange
    // act
    ResponseEntity<AssinaturaEntity> response = this.restTemplate.postForEntity("/diagnostic/run", new Preferences(),
        AssinaturaEntity.class);
    // assert
    Assertions.assertThat(response).isNotNull();
    Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void test_DiagnosticRun_OK() throws Exception {

    // @formatter:off
		mockMvc
		.perform(MockMvcRequestBuilders
				.post(DIAGNOSTIC_RUN)
				.contentType(APPLICATION_JSON_VALUE)
				.content(new ObjectMapper().writeValueAsString(new Preferences()))
				.accept(APPLICATION_JSON_VALUE))
    .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
		.andExpect(status().isOk());
		// @formatter:on
  }

  @Test
  public void test_DiagnosticRun_ReturnAssinaturaEntity() throws Exception {
    assertEquals(restTemplate.getForObject(HTTP_LOCALHOST + DIAGNOSTIC_RUN, AssinaturaEntity.class).getClass(), AssinaturaEntity.class);
  }

  @Test
  public void test_DiagnosticRun_UNSUPPORTED_MEDIA_TYPE() throws Exception {

    Stream.of(ALL, APPLICATION_XML, TEXT_HTML, APPLICATION_ATOM_XML, APPLICATION_PDF, APPLICATION_RSS_XML, APPLICATION_XHTML_XML,
        TEXT_PLAIN, TEXT_EVENT_STREAM, MULTIPART_FORM_DATA).forEach(type -> {
          try {
        // @formatter:off
						mockMvc.perform(MockMvcRequestBuilders
								.post(DIAGNOSTIC_RUN)
								.contentType(type)
								.content(new ObjectMapper().writeValueAsString(new Preferences()))
								.accept(type))
						.andExpect(status().is(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()));
						// @formatter:on
          } catch (Exception e) {
            fail(e.getMessage());
          }
        });

  }

  @Test
  public void test_DiagnosticRun_BAD_REQUEST() throws Exception {
    // @formatter:off
		mockMvc.perform(MockMvcRequestBuilders
				.post(DIAGNOSTIC_RUN)
				.contentType(APPLICATION_JSON_VALUE)
				.accept(APPLICATION_JSON_VALUE))
		.andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
		// @formatter:on
  }

  /**
   * Preferencia carregar
   */

  @Test
  public void test_PreferenciasCarregar_OK() throws Exception {
    // @formatter:off
		mockMvc.perform(MockMvcRequestBuilders
				.post(PREFERENCIAS_CARREGAR)
				.contentType(APPLICATION_JSON_VALUE)
				.content(new ObjectMapper().writeValueAsString(new Preferences()))
				.accept(APPLICATION_JSON_VALUE))
    .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
		.andExpect(status().isOk());
		// @formatter:on
  }

  @Test
  public void test_PreferenciasCarregar_ReturnAssinaturaEntity() {
    assertEquals(restTemplate.getForObject(HTTP_LOCALHOST + PREFERENCIAS_CARREGAR, AssinaturaEntity.class).getClass(),
        AssinaturaEntity.class);
  }

  @Test
  public void test_PreferenciasCarregar_BAD_REQUEST() throws Exception {
    // @formatter:off
		mockMvc.perform(MockMvcRequestBuilders
				.post(PREFERENCIAS_CARREGAR)
				.contentType(APPLICATION_JSON_VALUE)
				.content("")
				.accept(APPLICATION_JSON_VALUE))
		.andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
		// @formatter:on
  }

  @Test
  public void test_PreferenciasCarregar_UNSUPPORTED_MEDIA_TYPE() throws Exception {
    // @formatter:off
		mockMvc.perform(MockMvcRequestBuilders
				.post(PREFERENCIAS_CARREGAR)
				.contentType(ALL)
				.content(new ObjectMapper().writeValueAsString(new Preferences()))
				.accept(ALL))
		.andExpect(status().is(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()));
		// @formatter:on
  }

  @Test
  public void test_PreferenciasSalvar_ReturnAssinaturaEntity() {
    assertEquals(restTemplate.getForObject(HTTP_LOCALHOST + PREFERENCIAS_SALVAR, AssinaturaEntity.class).getClass(),
        AssinaturaEntity.class);
  }

  @Test
  public void test_PreferenciasSalvar_OK() throws Exception {
    // @formatter:off
		mockMvc
		.perform(MockMvcRequestBuilders
				.post(PREFERENCIAS_SALVAR)
				.contentType(APPLICATION_JSON_VALUE)
				.content(new ObjectMapper().writeValueAsString(new Preferences()))
				.accept(APPLICATION_JSON_VALUE))
    .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
		.andExpect(status().isOk());
		// @formatter:on
  }

  @Test
  public void test_PreferenciasSalvar_BAD_REQUEST() throws Exception {
    // @formatter:off
		mockMvc
		.perform(MockMvcRequestBuilders
				.post(PREFERENCIAS_SALVAR)
				.contentType(APPLICATION_JSON_VALUE)
				.content(new ObjectMapper().writeValueAsString(null))
				.accept(APPLICATION_JSON_VALUE))
		.andExpect(status().is(HttpStatus.BAD_REQUEST.value()));
		// @formatter:on
  }

  @Test
  public void test_PreferenciasSalvar_UNSUPPORTED_MEDIA_TYPE() throws Exception {
    // @formatter:off
		mockMvc
		.perform(MockMvcRequestBuilders
				.post(PREFERENCIAS_SALVAR)
				.contentType(ALL)
				.content(new ObjectMapper().writeValueAsString(new Preferences()))
				.accept(ALL))
		.andExpect(status().is(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()));
		// @formatter:on
  }

  /**
   * User Info
   */

  @Test
  public void test_UserInfo_OK() throws Exception {
    //@formatter:off
    mockMvc.perform(MockMvcRequestBuilders
    		.post(USER_INFO)
				.contentType(APPLICATION_JSON_VALUE)
				.content(new ObjectMapper().writeValueAsString(new Preferences()))
				.accept(APPLICATION_JSON_VALUE))
    .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON_VALUE))
    .andExpect(status().isOk());
    //@formatter:on
  }

  @Test
  public void test_UserInfo_ReturnAssinaturaEntity() {
    assertEquals(restTemplate.getForObject(HTTP_LOCALHOST + USER_INFO, AssinaturaEntity.class).getClass(), AssinaturaEntity.class);
  }

}
