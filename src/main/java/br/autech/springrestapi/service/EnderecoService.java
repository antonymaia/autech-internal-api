package br.autech.springrestapi.service;

import br.autech.springrestapi.model.Cidade;
import br.autech.springrestapi.model.Endereco;
import br.autech.springrestapi.repository.EnderecoRepository;
import br.autech.springrestapi.service.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EnderecoService {

    @Autowired
    private EnderecoRepository enderecoRepository;

    public Endereco create(Endereco endereco){
        return enderecoRepository.save(endereco);
    }

    public Endereco findByCep(String cep){
        return enderecoRepository.findByCep(cep);
    }

    public Endereco findById(Long id){
        Optional<Endereco> endereco = enderecoRepository.findById(id);
        return endereco.orElseThrow(()-> new ObjectNotFoundException("Endereço não encontrado na base de dados, id: "+id));
    }

    public List<Endereco> findAll() {
       return  enderecoRepository.findAll();
    }

    public Endereco update(Endereco endereco){
        return enderecoRepository.save(endereco);
    }

    public Page<Endereco> search(String searchTerm, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.ASC, "ENDERECO");
        return enderecoRepository.search(searchTerm, pageable);
    }

    public Endereco findByLogradouroAndBairroAndCidade(String logradouro, String bairro, Cidade cidade){
        return enderecoRepository.findByLogradouroAndBairroAndCidade(logradouro, bairro, cidade);
    }

    public String padronizacaoLogradouro(String logradouro){
        logradouro = (logradouro.indexOf("Av ") == 0) ? logradouro.replace("Av ", "Avenida ") : logradouro;
        logradouro = (logradouro.indexOf("Av. ") == 0) ? logradouro.replace("Av. ", "Avenida ") : logradouro;
        logradouro = (logradouro.indexOf("R ") == 0) ? logradouro.replace("R ", "Rua ") : logradouro;
        logradouro = (logradouro.indexOf("R. ") == 0) ? logradouro.replace("R. ", "Rua ") : logradouro;
        return logradouro;
    }
}
