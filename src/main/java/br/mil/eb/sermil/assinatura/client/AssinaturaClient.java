package br.mil.eb.sermil.assinatura.client;

import java.io.IOException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Anselmo S Ribeiro <anselmo.sr@gmail.com>
 * @version 1.3-GO
 * @since 0.1.0
 */
@Slf4j
@SpringBootApplication
@PropertySource(value = "assinatura.properties")
public class AssinaturaClient extends javafx.application.Application {

  private final static boolean DEV_MODE = false ; //desenvolvimento: true, producao: false
	private final String PAGE = "http://localhost:8765/";
  private final String TITULO = "Sermil - Modulo Assinatura Digital";
  private final float LARGURA = 1070f;
  private final float ALTURA = LARGURA / 1.42f;
	public static Stage stage;

  public static void main(String[] args) {
    log.info("Iniciando Modulo Assinatura Digital ...");
    SpringApplication.run(AssinaturaClient.class, args); // Spring
    
		if (DEV_MODE) {
			// Desenvolvimento
			openBrowser();
		} else {
			// Producao
			launch(args);
			Platform.exit();
			System.exit(0);
		}
    
  }

  /**
   * OPEN BROWSER - SOMENTE PARA DESENVOLVIMENTO
   */
  private static void openBrowser() {
    val url = "http://localhost:8765";
    try {
      val rt = Runtime.getRuntime();
      rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  /**
   * OPEN DESKTOP APPLICATION
   */
  @Override
  public void start(Stage primaryStage) throws Exception {
  	stage = primaryStage;
    createWebView(primaryStage, PAGE);
  }

  private void createWebView(Stage primaryStage, String page) {
    val webView = new WebView();
    webView.getEngine().setOnAlert((e) -> log.info("Browse Alert : {}", e.getData()));
    webView.getEngine().setOnError((e) -> log.info("Browser Error: {}", e.getMessage()));
    webView.getEngine().load(page);
    val scene = new Scene(webView, LARGURA, ALTURA);
    primaryStage.getIcons().add(new Image("static/image/logo3.png"));
    primaryStage.setScene(scene);
    primaryStage.setTitle(TITULO);
    primaryStage.setMaximized(true);
    primaryStage.show();
  }

  /**
   * SPRING BEANS AND MAIN CONTROLLER CONFIGURATION
   */

  @Bean
  public CorsFilter corsFilter() {
    val source = new UrlBasedCorsConfigurationSource();
    val config = new CorsConfiguration();
    config.setAllowCredentials(true);
    config.addAllowedOrigin("*");
    config.addAllowedHeader("*");
    config.addAllowedMethod("OPTIONS");
    config.addAllowedMethod("GET");
    config.addAllowedMethod("POST");
    config.addAllowedMethod("PUT");
    config.addAllowedMethod("DELETE");
    source.registerCorsConfiguration("/**", config);
    return new CorsFilter(source);
  }

  @Controller
  @RequestMapping({"/", ""})
  class IndexController {
    public String index() {
      return "index";
    }
  }

}
