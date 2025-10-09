package br.autech.springrestapi.service;

import br.autech.springrestapi.model.Cidade;
import br.autech.springrestapi.repository.CidadeRepositoy;
import br.autech.springrestapi.service.exception.DataIntegretyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CidadeService {

    @Autowired
    private CidadeRepositoy cidadeRepositoy;

    public Cidade insert(Cidade cidade){
        if(cidadeRepositoy.findByCodigoMunicipio(cidade.getCodigoMunicipio()) != null){
            throw new DataIntegretyException(
                    "Cidade com código de municipio "+ cidade.getCodigoMunicipio() +", já cadastrada"
            );
        }
        cidade.setNome(cidade.getNome().toUpperCase());
        return cidadeRepositoy.save(cidade);
    }

    public Cidade findByCodigoMunicipio(String codigoMunicipio){
        return cidadeRepositoy.findByCodigoMunicipio(codigoMunicipio);
    }
}
