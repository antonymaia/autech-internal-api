package br.autech.springrestapi.service;

import br.autech.springrestapi.dtos.AssinaturaDTO;
import br.autech.springrestapi.dtos.AssinaturaProdutoDTO;
import br.autech.springrestapi.model.Assinatura;
import br.autech.springrestapi.model.AssinaturaProduto;
import br.autech.springrestapi.model.Cliente;
import br.autech.springrestapi.model.Produto;
import br.autech.springrestapi.repository.AssinaturaProdutoRepository;
import br.autech.springrestapi.repository.AssinaturaRepository;
import br.autech.springrestapi.repository.ClienteRepository;
import br.autech.springrestapi.repository.ProdutoRepository;
import br.autech.springrestapi.service.exception.AssinaturaException;
import br.autech.springrestapi.service.exception.BadRequestException;
import br.autech.springrestapi.service.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AssinaturaService {
    private final ClienteRepository clienteRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final AssinaturaProdutoRepository assinaturaProdutoRepository;
    private final ProdutoRepository produtoRepository;

    @Autowired
    public AssinaturaService(AssinaturaRepository assinaturaRepository,
                             ClienteRepository clienteRepository,
                             AssinaturaProdutoRepository assinaturaProdutoRepository,
                             ProdutoRepository produtoRepository) {
        this.assinaturaRepository = assinaturaRepository;
        this.clienteRepository = clienteRepository;
        this.assinaturaProdutoRepository = assinaturaProdutoRepository;
        this.produtoRepository = produtoRepository;
    }

    public Assinatura buscarAssinatura(String id) {
        Optional<Assinatura> assinatura = assinaturaRepository.findByCliente_CnpjCpf(id);

        return assinatura.orElse(null);
    }

    @Transactional
    public Assinatura inserirAssinatura(AssinaturaDTO assinaturaDTO) {
        Optional<Assinatura> optionalAssinatura = assinaturaRepository.findByCliente_CnpjCpf(assinaturaDTO.getCnpjCpfCliente());
        if (optionalAssinatura.isPresent()) {
            throw new AssinaturaException.JaExisteException(assinaturaDTO.getCnpjCpfCliente());
        }
        Cliente cliente = clienteRepository
                .findById(assinaturaDTO.getCnpjCpfCliente())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado"));

        Assinatura assinatura = new Assinatura();
        assinatura.setCliente(cliente);
        assinatura.setTipo_assinatura(assinaturaDTO.getTipoAssinatura());
        assinatura.setStatus(assinaturaDTO.getStatus());
        assinatura.setValor_total(BigDecimal.ZERO);
        assinatura.setDataInicial(LocalDate.now(ZoneId.of("America/Sao_Paulo")));

        assinatura = assinaturaRepository.save(assinatura);

        for(AssinaturaProdutoDTO produto : assinaturaDTO.getProdutos()){
            AssinaturaProduto assinaturaProduto = adicionarProduto(assinatura.getId(), produto);
            assinatura.getProdutos().add(assinaturaProduto);
        }

        assinatura.setValor_total(calcularValorTotal(assinatura));
        return assinaturaRepository.save(assinatura);
    }

    @Transactional
    public Assinatura atualizarAssinatura(AssinaturaDTO assinaturaDTO) {
        Assinatura assinatura = assinaturaRepository.findByCliente_CnpjCpf(assinaturaDTO.getCnpjCpfCliente())
                .orElseThrow(() -> new RuntimeException("Assinatura inexistente"));
        assinatura.setStatus(assinaturaDTO.getStatus());
        assinatura.setTipo_assinatura(assinaturaDTO.getTipoAssinatura());
        assinatura.setValor_total(calcularValorTotal(assinatura));

        return assinaturaRepository.save(assinatura);
    }

    @Transactional
    public AssinaturaProduto adicionarProduto(Long assinaturaId, AssinaturaProdutoDTO dto) {
        if (dto.getProdutoId() == null) {
            throw new BadRequestException("produtoId obrigatorio");
        }
        int qtd = dto.getQuantidade() == null ? 1 : dto.getQuantidade();
        if (qtd <= 0) {
            throw new BadRequestException("quantidade deve ser maior que zero");
        }

        Assinatura assinatura = assinaturaRepository.findById(assinaturaId)
                .orElseThrow(() -> new ObjectNotFoundException("Assinatura " + assinaturaId + " nao encontrada"));

        Produto produto = produtoRepository.findById(dto.getProdutoId())
                .orElseThrow(() -> new ObjectNotFoundException("Produto " + dto.getProdutoId() + " nao encontrado"));

        Optional<AssinaturaProduto> existente = assinaturaProdutoRepository
                .findByAssinaturaAndProduto_IdProduto(assinatura, produto.getIdProduto());

        AssinaturaProduto salvo;
        if (existente.isPresent()) {
            AssinaturaProduto ap = existente.get();
            ap.setQuantidade(qtd);
            salvo = assinaturaProdutoRepository.save(ap);
        } else {
            BigDecimal snapshot = dto.getValor() != null ? dto.getValor() : produto.getValor();
            salvo = assinaturaProdutoRepository.save(new AssinaturaProduto(assinatura, produto, qtd, snapshot));
        }

        assinatura.setValor_total(calcularValorTotal(assinatura));
        assinaturaRepository.save(assinatura);

        return salvo;
    }

    @Transactional
    public void removerProduto(Long assinaturaProdutoId) {
        AssinaturaProduto ap = assinaturaProdutoRepository.findById(assinaturaProdutoId)
                .orElseThrow(() -> new ObjectNotFoundException("AssinaturaProduto " + assinaturaProdutoId + " nao encontrado"));
        Assinatura assinatura = ap.getAssinatura();
        assinaturaProdutoRepository.delete(ap);

        assinatura.setValor_total(calcularValorTotal(assinatura));
        assinaturaRepository.save(assinatura);
    }

    @Transactional(readOnly = true)
    public List<AssinaturaProduto> listarProdutos(Long assinaturaId) {
        Assinatura assinatura = assinaturaRepository.findById(assinaturaId)
                .orElseThrow(() -> new ObjectNotFoundException("Assinatura " + assinaturaId + " nao encontrada"));
        return assinaturaProdutoRepository.findAllByAssinatura(assinatura);
    }

    private BigDecimal calcularValorTotal(Assinatura assinatura) {
        List<AssinaturaProduto> produtos = assinaturaProdutoRepository.findAllByAssinatura(assinatura);
        BigDecimal total = BigDecimal.ZERO;
        for (AssinaturaProduto ap : produtos) {
            BigDecimal valorUnitario = ap.getValor() != null ? ap.getValor() : BigDecimal.ZERO;
            int qtd = ap.getQuantidade() != null ? ap.getQuantidade() : 0;
            total = total.add(valorUnitario.multiply(BigDecimal.valueOf(qtd)));
        }
        return total;
    }
}
