package br.com.tissguard.api;

import br.com.tissguard.api.dto.RespostaErroApi;
import br.com.tissguard.validacao.EsquemaInvalidoException;
import br.com.tissguard.validacao.EsquemaNaoEncontradoException;
import br.com.tissguard.validacao.XmlMalformadoException;
import br.com.tissguard.validacao.XmlSegurancaException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespostaErroApi> tratarValidacaoCampos(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        RespostaErroApi erro = RespostaErroApi.erro(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Dados de requisição inválidos ou incompletos.",
                request.getRequestURI(),
                detalhes
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<RespostaErroApi> tratarRecursoNaoEncontrado(
            RecursoNaoEncontradoException ex,
            HttpServletRequest request
    ) {
        RespostaErroApi erro = RespostaErroApi.erroSimples(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    @ExceptionHandler(EsquemaNaoEncontradoException.class)
    public ResponseEntity<RespostaErroApi> tratarEsquemaNaoEncontrado(
            EsquemaNaoEncontradoException ex,
            HttpServletRequest request
    ) {
        RespostaErroApi erro = RespostaErroApi.erroSimples(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    @ExceptionHandler({XmlSegurancaException.class, XmlMalformadoException.class, IllegalArgumentException.class})
    public ResponseEntity<RespostaErroApi> tratarErrosRequisicao(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        RespostaErroApi erro = RespostaErroApi.erroSimples(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(EsquemaInvalidoException.class)
    public ResponseEntity<RespostaErroApi> tratarEsquemaInvalido(
            EsquemaInvalidoException ex,
            HttpServletRequest request
    ) {
        RespostaErroApi erro = RespostaErroApi.erroSimples(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                HttpStatus.UNPROCESSABLE_ENTITY.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(erro);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespostaErroApi> tratarErroInesperado(
            Exception ex,
            HttpServletRequest request
    ) {
        RespostaErroApi erro = RespostaErroApi.erroSimples(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Ocorreu um erro interno inesperado ao processar a operação.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }
}
