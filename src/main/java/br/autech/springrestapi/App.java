package br.autech.springrestapi;

import br.autech.springrestapi.model.Assinatura;
import br.autech.springrestapi.model.Cliente;
import br.autech.springrestapi.model.enums.StatusAssinatura;
import br.autech.springrestapi.model.enums.TipoAssinatura;
import br.autech.springrestapi.repository.AssinaturaRepository;
import br.autech.springrestapi.service.AssinaturaService;
import br.autech.springrestapi.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class})
@EnableScheduling
public class App implements CommandLineRunner {
   @Autowired
   private ClienteService ClienteService;
   @Autowired
   private AssinaturaRepository assinaturaRepository;

   private Cliente cliente;
   private Assinatura assinatura;
    @Autowired
    private AssinaturaService assinaturaService;

    public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

	@Bean
	public PasswordEncoder getPasswordEncoder(){
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		return encoder;
	}
        
    public void run(String... args) throws Exception {


    }


}
