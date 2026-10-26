package br.insper.estudo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // necessário para o worker da fila (relatorio/RelatorioWorker)
public class EstudoApplication {

    public static void main(String[] args) {
        SpringApplication.run(EstudoApplication.class, args);
    }
}
