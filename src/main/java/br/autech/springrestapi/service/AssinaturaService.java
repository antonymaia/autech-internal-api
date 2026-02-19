package br.autech.springrestapi.service;

import br.autech.springrestapi.dtos.AssinaturaDTO;
import br.autech.springrestapi.dtos.AssinaturaProdutoDTO;
import br.autech.springrestapi.model.Assinatura;
import br.autech.springrestapi.model.AssinaturaProduto;
import br.autech.springrestapi.model.Cliente;
import br.autech.springrestapi.model.enums.StatusAssinatura;
import br.autech.springrestapi.model.enums.TipoAssinatura;
import br.autech.springrestapi.repository.AssinaturaRepository;

import br.autech.springrestapi.repository.ClienteRepository;
import br.autech.springrestapi.service.exception.AssinaturaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class AssinaturaService {
    private final ClienteRepository clienteRepository;
    private final AssinaturaRepository assinaturaRepository;
    @Autowired
    public AssinaturaService (AssinaturaRepository assinaturaRepository ,  ClienteRepository clienteRepository) {
        this.assinaturaRepository = assinaturaRepository;
        this.clienteRepository = clienteRepository;
    }
    @Transactional
    public AssinaturaDTO buscarAssinatura(String id) {
        Assinatura assinatura = assinaturaRepository.findByCliente_CnpjCpf(id).orElseThrow(() -> new RuntimeException("Assinatura não encontrada"));

        AssinaturaDTO dto = new AssinaturaDTO();


        dto.setAssinaturaProduto(assinatura.getAssinaturaProdutos().stream().map(this::toDTO).collect(Collectors.toList()));
        dto.setTipoAssinatura(assinatura.getTipo_assinatura());
        dto.setStatus(assinatura.getStatus());
        dto.setData_fim(assinatura.getData_fim());
        dto.setData_inicio(assinatura.getData_inicio());
        dto.setQuantidade_caixa(assinatura.getQuantidade_caixa());
        dto.setCnpjCpfCliente(assinatura.getCliente().getCnpjCpf());


        return dto;
    }
    public  Assinatura inserirAssinatura(AssinaturaDTO assinaturaDTO ){


        Optional<Assinatura>  optionalAssinatura = assinaturaRepository.findByCliente_CnpjCpf(assinaturaDTO.getCnpjCpfCliente());
        if(optionalAssinatura.isPresent()) {
           throw new AssinaturaException.JaExisteException(assinaturaDTO.getCnpjCpfCliente());
        }
         Cliente cliente = clienteRepository
                .findById(assinaturaDTO.getCnpjCpfCliente())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        Assinatura assinatura = new Assinatura();
        assinatura.setCliente(cliente);
        assinatura.setTipo_assinatura(assinaturaDTO.getTipoAssinatura());
        assinatura.setStatus(assinaturaDTO.getStatus());
        assinatura.setValor_total(assinaturaDTO.getValor_total());
        assinatura.setData_fim(assinaturaDTO.getData_fim());
        assinatura.setData_inicio(assinaturaDTO.getData_inicio());

    return    assinaturaRepository.save(assinatura);


    }

    public Assinatura atualizarAssinatura(AssinaturaDTO assinaturaDTO) {
        Assinatura assinatura = assinaturaRepository.findByCliente_CnpjCpf(assinaturaDTO.getCnpjCpfCliente())
                .orElseThrow(() -> new RuntimeException("Assinatura inexistente"));

        assinatura.setData_fim(assinaturaDTO.getData_fim());
        assinatura.setValor_total(assinaturaDTO.getValor_total());
        assinatura.setData_inicio(assinaturaDTO.getData_inicio());
        assinatura.setStatus(assinaturaDTO.getStatus());
        assinatura.setTipo_assinatura(assinaturaDTO.getTipoAssinatura());
        assinatura.setQuantidade_caixa(assinaturaDTO.getQuantidade_caixa());

        return assinaturaRepository.save(assinatura);
        }

    private AssinaturaProdutoDTO toDTO(AssinaturaProduto produto) {
        AssinaturaProdutoDTO dto = new AssinaturaProdutoDTO();

        dto.setIdAssinaturaProduto(produto.getIdAssinaturaProduto());
        dto.setQuantidade(produto.getQuantidade());
        dto.setValorProduto(produto.getValorProduto());
        dto.setDesconto(produto.getDesconto());
        dto.setAcrescimo(produto.getAcrescimo());
        dto.setValorUnitarioFinal(produto.getValorUnitarioFinal());
        dto.setValorTotal(produto.getValor());
        dto.setIdProduto(produto.getProduto().getIdProduto());
        dto.setNomeProduto(produto.getProduto().getNome());

        dto.setDataInclusao(produto.getDataInclusao());

        return dto;
    }
    }



