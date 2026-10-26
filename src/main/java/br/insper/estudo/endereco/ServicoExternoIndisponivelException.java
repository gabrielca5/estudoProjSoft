package br.insper.estudo.endereco;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** 503: o problema não é do cliente, é de uma dependência nossa. */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ServicoExternoIndisponivelException extends RuntimeException {
    public ServicoExternoIndisponivelException(String mensagem) {
        super(mensagem);
    }
}
