package br.autech.springrestapi.service;

import br.autech.springrestapi.model.Estado;
import br.autech.springrestapi.repository.EstadoRepository;
import br.autech.springrestapi.service.exception.DataIntegretyException;
import br.autech.springrestapi.service.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EstadoService {

    @Autowired
    EstadoRepository estadoRepository;

    public Estado insert(Estado estado){
        if(estadoRepository.findByCodigoUf(estado.getCodigoUf()).isPresent()){
            throw new DataIntegretyException("Código UF "+ estado.getCodigoUf() + ", já cadastrado");
        }
        estado.setNome(estado.getNome().toUpperCase());
        estado.setUf(estado.getUf().toUpperCase());
        return estadoRepository.save(estado);
    }

    public Estado findByCodigoUF(String codigo){
        return estadoRepository.findByCodigoUf(codigo).orElseThrow(
                () -> new ObjectNotFoundException("Estado não encontrado, Código UF: " + codigo)
        );
    }
}
