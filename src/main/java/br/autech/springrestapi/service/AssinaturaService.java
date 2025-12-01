package br.autech.springrestapi.service;

import br.autech.springrestapi.model.Assinatura;
import br.autech.springrestapi.model.enums.StatusAssinatura;
import br.autech.springrestapi.model.enums.TipoAssinatura;
import br.autech.springrestapi.repository.AssinaturaRepository;
import br.autech.springrestapi.service.exception.AssinaturaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
public class AssinaturaService {
    private final AssinaturaRepository assinaturaRepository;
    @Autowired
    public AssinaturaService (AssinaturaRepository assinaturaRepository) {
        this.assinaturaRepository = assinaturaRepository;
    }

    public Assinatura buscarAssinatura(String id) {
        Optional<Assinatura> assinatura = assinaturaRepository.findByClienteId(id);

        return assinatura.orElse(null);
    }
    public  Assinatura inserirAssinatura(Assinatura assinatura , String cnpjCpfCliente){

         Optional<Assinatura>  optionalAssinatura = assinaturaRepository.findByClienteId(cnpjCpfCliente);
        if(optionalAssinatura.isPresent()) {
           throw new AssinaturaException.JaExisteException(cnpjCpfCliente);
        }

       assinaturaRepository.save(assinatura);
        return assinatura;

    }

    public Assinatura atualizarStatusPorCliente( String cnpjCpfCliente, StatusAssinatura statusAssinatura){
       Assinatura assinatura = assinaturaRepository.findByClienteId(cnpjCpfCliente).orElseThrow();
       assinatura.setStatus(statusAssinatura);
       assinaturaRepository.save(assinatura);

        return assinatura;
    }
    public Assinatura atualizarTipoAssinaturaPorCliente( String cnpjCpfCliente, TipoAssinatura tipoAssinatura){
       Assinatura assinatura = assinaturaRepository.findByClienteId(cnpjCpfCliente).orElseThrow();
       assinatura.setTipo_assinatura(tipoAssinatura);
       assinaturaRepository.save(assinatura);

        return assinatura;
    }
}
